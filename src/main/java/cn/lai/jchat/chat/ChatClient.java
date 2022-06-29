package cn.lai.jchat.chat;

import cn.lai.jchat.Utils;
import cn.lai.jchat.model.ChatMessage;
import cn.lai.jchat.model.ChatSession;
import cn.lai.jchat.model.OnlineUser;
import cn.lai.jchat.model.Session;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatClient {
    private final ChatManager chatManager;
    private final AtomicBoolean isAlive = new AtomicBoolean();
    private final Map<UUID, BlockingQueue<ChatMessage>> messageQueues = new HashMap<>();
    private final Logger logger = LogManager.getLogger(getClass().getSimpleName());
    private final Map<UUID, Set<MessageSubscriber>> messageSubscribers = new HashMap<>();
    private final Set<ChatSession> connectedChatSessions = new HashSet<>();
    private final Set<UUID> connectingUserIds = new HashSet<>();
    private final Set<UUID> sendingMessages = new HashSet<>();

    public ChatClient(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    public void start() {
        if (isAlive.get()) return;
        isAlive.set(true);
    }

    public void stop() {
        isAlive.set(false);
    }

    public void connect(OnlineUser onlineUser) {
        if (chatManager.getMyself() == null)
            return;
        synchronized (this) {
            if (connectingUserIds.contains(onlineUser.getUser().getId())) {
                logger.debug(onlineUser + " 已在连接队列");
                return;
            }
            connectingUserIds.add(onlineUser.getUser().getId());
        }
        new Thread(() -> doConnect(onlineUser)).start();
    }

    private void doConnect(final OnlineUser onlineUser) {
        ChatSession chatSession = null;
        Socket socket = null;
        try {
            socket = new Socket(onlineUser.getHost(), onlineUser.getPort());
            chatSession = new ChatSession(socket, null, null, false);
            chatSession.setContextUserId(onlineUser.getUser().getId());
            chatSession.setUser(onlineUser.getUser());
            chatSession.setRemoteServerPort(onlineUser.getPort());
            chatManager.onSessionCreated(chatSession);
            Buffer buffer = ObjectHelper.serialize(chatManager.getMyself());
            String userBase64 = buffer.readByteString().base64();
            buffer.close();
            BufferedSink sink = chatSession.getSink();
            sink.writeUtf8(ChatSession.NAME + "\n");
            sink.writeUtf8(chatManager.getMyself().getId().toString() + "\n");
            sink.writeUtf8(userBase64 + "\n");
            sink.writeUtf8(chatManager.getChatServer().getServerSocket().getLocalPort() + "\n");
            sink.flush();
            BufferedSource source = chatSession.getSource();
            String status = source.readUtf8Line();
            if (!"OK".equals(status)) {
                throw new IllegalStateException("状态码为 " + status);
            }
            chatSession.setState(Session.State.CONNECTED);
            chatManager.onSessionConnected(chatSession);
            logger.debug("已连接 " + chatSession);
        } catch (Exception e) {
            logger.debug("连接失败", e);
            if (chatSession != null) {
                chatManager.notifySessionClosed(chatSession);
            }
            Utils.closeQuietly(socket);
        } finally {
            synchronized (this) {
                connectingUserIds.remove(onlineUser.getUser().getId());
            }
        }
    }

    public synchronized void subscribe(UUID userId, MessageSubscriber subscriber) {
        if (userId == null || subscriber == null)
            return;
        if (!messageSubscribers.containsKey(userId))
            messageSubscribers.put(userId, new HashSet<>());
        messageSubscribers.get(userId).add(subscriber);
    }

    public synchronized void unsubscribe(UUID userId, MessageSubscriber subscriber) {
        if (!messageSubscribers.containsKey(userId)) return;
        messageSubscribers.get(userId).remove(subscriber);
    }

    public synchronized void onChatSessionConnected(ChatSession chatSession) {
        if (connectedChatSessions.contains(chatSession))
            return;
        connectedChatSessions.add(chatSession);
        messageQueues.putIfAbsent(chatSession.getContextUserId(), new LinkedBlockingQueue<>());
        new Thread(() -> sendLoop(chatSession)).start();
        new Thread(() -> receiveLoop(chatSession)).start();
    }

    private synchronized BlockingQueue<ChatMessage> getChatMessageQueue(UUID userId) {
        return messageQueues.get(userId);
    }

    public synchronized void onChatSessionRemoved(ChatSession chatSession) {
        BlockingQueue<ChatMessage> queue = getChatMessageQueue(chatSession.getContextUserId());
        if (queue != null) {
            queue.clear();
        }
        connectedChatSessions.remove(chatSession);
    }

    public void sendMessage(UUID userId, ChatMessage message) {
        BlockingQueue<ChatMessage> queue;
        message.setSender(chatManager.getMyself());
        message.setContextUserId(userId);
        boolean exist;
        synchronized (this) {
            messageQueues.putIfAbsent(userId, new LinkedBlockingQueue<>());
            queue = messageQueues.get(userId);
            exist = sendingMessages.contains(message.getId());
            if (!exist) {
                sendingMessages.add(message.getId());
            }
        }
        message.setState(ChatMessage.State.SENDING);
        if (!exist) {
            addChatMessage(userId, message);
        } else {
            updateMessage(userId, message);
        }
        try {
            queue.put(message);
        } catch (InterruptedException ignored) {
        }
    }

    private void addChatMessage(UUID userId, ChatMessage message) {
        List<MessageSubscriber> subscribers;
        ChatMessageStore store = chatManager.getChatMessageStore();
        chatManager.getFileManager().updateChatMessageFileTask(message);
        store.insert(userId, message);
        synchronized (this) {
            if (!messageSubscribers.containsKey(userId))
                return;
            subscribers = new ArrayList<>(messageSubscribers.get(userId));
        }
        for (MessageSubscriber subscriber : subscribers) {
            try {
                subscriber.onNewMessage(userId, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateMessage(UUID userId, ChatMessage message) {
        List<MessageSubscriber> subscribers;
        chatManager.getFileManager().updateChatMessageFileTask(message);
        ChatMessageStore store = chatManager.getChatMessageStore();
        store.update(userId, message);
        synchronized (this) {
            if (message.getState() == ChatMessage.State.OK) {
                sendingMessages.remove(message.getId());
            }
            if (!messageSubscribers.containsKey(userId))
                return;
            subscribers = new ArrayList<>(messageSubscribers.get(userId));
        }
        for (MessageSubscriber subscriber : subscribers) {
            try {
                subscriber.onMessageUpdate(userId, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendLoop(final ChatSession session) {
        final BlockingQueue<ChatMessage> queue;
        synchronized (this) {
            queue = messageQueues.get(session.getContextUserId());
        }
        BufferedSink sink = session.getSink();
        logger.debug("发信循环 " + session.getContextUserId());
        while (isAlive.get() && session.isAlive()) {
            try {
                ChatMessage message = queue.poll(500, TimeUnit.MILLISECONDS);
                if (message == null) continue;
                Buffer buffer = ObjectHelper.serialize(message);
                String base64 = buffer.readByteString().base64();
                buffer.close();
                session.setBusy(true);
                logger.debug("发送 " + message + " -> " + session.getContextUserId());
                try {
                    sink.writeUtf8(base64 + "\n");
                    sink.flush();
                    logger.debug("发送成功!");
                    session.setUpdateTime(new Date());
                    session.setBusy(false);
                    message.setState(ChatMessage.State.OK);
                    updateMessage(session.getContextUserId(), message);
                } catch (Exception e) {
                    session.setBusy(false);
                    message.setState(ChatMessage.State.FAILED);
                    updateMessage(session.getContextUserId(), message);
                    if (e instanceof IOException) {
                        if (session.getState() != Session.State.CLOSED) {
                            logger.debug("发信失败，已断开");
                        }
                        chatManager.notifySessionClosed(session);
                    }
                    break;
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void onChatMessageReceived(ChatSession session, ChatMessage message) {
        logger.debug("收信 " + session.getContextUserId() + " -> " + message);
        addChatMessage(session.getContextUserId(), message);
        if (message.getFile() != null) {
            ChatMessage.FileResource resource = message.getFile();
            chatManager.getFileManager().download(session.getContextUserId(), resource.getResId(), resource.getFileName());
        }
    }

    private void receiveLoop(final ChatSession session) {
        logger.debug("收信循环 " + session.getContextUserId());
        BufferedSource source = session.getSource();
        while (isAlive.get() && session.isAlive()) {
            try {
                String line = source.readUtf8Line();
                if (line == null) {
                    // EOF，正常断开
                    chatManager.notifySessionClosed(session);
                    break;
                }
                ChatMessage chatMessage = (ChatMessage) ObjectHelper.deserialize(ByteString.decodeBase64(line));
                chatMessage.setContextUserId(session.getContextUserId());
                chatMessage.setSender(session.getUser().copy());
                chatMessage.setState(ChatMessage.State.OK);
                chatMessage.setMine(false);
                onChatMessageReceived(session, chatMessage);
                session.setUpdateTime(new Date());
            } catch (IOException e) {
                if (session.getState() != Session.State.CLOSED) {
                    e.printStackTrace();
                    logger.debug("收信失败，已断开");
                }
                chatManager.notifySessionClosed(session);
                break;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
