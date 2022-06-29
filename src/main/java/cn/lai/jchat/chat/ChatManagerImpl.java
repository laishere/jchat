package cn.lai.jchat.chat;

import cn.lai.jchat.model.*;

import java.net.ServerSocket;
import java.util.UUID;

public class ChatManagerImpl implements ChatManager {
    private final User user = new User(UUID.randomUUID(), null, null);
    private OnlineUserManager onlineUserManager;
    private ChatServer chatServer;
    private SessionManager sessionManager;
    private ChatClient chatClient;
    private final ChatMessageStore chatMessageStore = new ChatMessageStore();
    private FileManager fileManager;
    private boolean isAlive = false;
    private ChatListener chatListener;

    private static ChatManagerImpl INSTANCE;

    public static ChatManagerImpl getInstance() {
        synchronized (ChatManagerImpl.class) {
            if (INSTANCE == null) {
                INSTANCE = new ChatManagerImpl();
            }
            return INSTANCE;
        }
    }

    public synchronized void setChatListener(ChatListener chatListener) {
        this.chatListener = chatListener;
    }

    @Override
    public synchronized void start() {
        if (isAlive) return;
        isAlive = true;
        onlineUserManager = new OnlineUserManager(this);
        chatServer = new ChatServer(this);
        sessionManager = new SessionManager(this);
        chatClient = new ChatClient(this);
        fileManager = new FileManager(this);
        onlineUserManager.start();
        chatServer.start();
        sessionManager.start();
        chatClient.start();
        fileManager.start();
    }

    @Override
    public synchronized void stop() {
        if (!isAlive) return;
        System.out.println("stop...");
        isAlive = false;
        onlineUserManager.stop();
        chatServer.stop();
        sessionManager.stop();
        chatClient.stop();
        fileManager.stop();
    }

    @Override
    public synchronized SessionManager getSessionManager() {
        return sessionManager;
    }

    @Override
    public synchronized ChatClient getChatClient() {
        return chatClient;
    }

    @Override
    public synchronized ChatServer getChatServer() {
        return chatServer;
    }

    @Override
    public synchronized OnlineUserManager getOnlineUserManager() {
        return onlineUserManager;
    }

    @Override
    public ChatMessageStore getChatMessageStore() {
        return chatMessageStore;
    }

    @Override
    public FileManager getFileManager() {
        return fileManager;
    }

    @Override
    public User getMyself() {
        return user.copy();
    }

    public void updateMyself(String name, String avatar) {
        user.setName(name);
        user.setAvatar(avatar);
        updateOnlineInfo();
    }

    private void updateOnlineInfo() {
        if (getMyself().getName() == null)
            return;
        ServerSocket serverSocket = chatServer.getServerSocket();
        if (serverSocket == null) return;
        OnlineUser onlineUser = new OnlineUser();
        onlineUser.setUser(getMyself());
        onlineUser.setPort(serverSocket.getLocalPort());
        onlineUserManager.updateMyself(onlineUser);
    }

    private synchronized void notifyChatListener(Runnable callback) {
        if (chatListener == null) return;
        callback.run();
    }

    @Override
    public void onSessionCreated(Session session) {
        session.setState(Session.State.CONNECTING);
        sessionManager.addSession(session);
        if (session instanceof ChatSession) {
            notifyChatListener(() -> chatListener.onAddChatSession((ChatSession) session));
        }
    }

    @Override
    public void onSessionConnected(Session session) {
        session.setState(Session.State.CONNECTED);
        if (session instanceof ChatSession) {
            chatClient.onChatSessionConnected((ChatSession) session);
            fileManager.notifyChatSessionConnected((ChatSession) session);
        } else if (session instanceof FileSession) {
            if (!((FileSession) session).isClient()) {
                fileManager.addFileSession((FileSession) session);
            }
        }
    }

    @Override
    public void notifySessionClosed(Session session) {
        sessionManager.notifySessionClosed(session);
    }

    @Override
    public void onSessionRemoved(Session session) {
        if (session instanceof ChatSession) {
            chatClient.onChatSessionRemoved((ChatSession) session);
            notifyChatListener(() -> chatListener.onRemoveChatSession((ChatSession) session));
        }
    }

    @Override
    public void onAddOnlineUser(OnlineUser user) {
        notifyChatListener(() -> chatListener.onAddOnlineUser(user));
    }

    @Override
    public void onRemoveOnlineUser(OnlineUser user) {
        notifyChatListener(() -> chatListener.onRemoveOnlineUser(user));
    }

    @Override
    public void onServerCreated() {
        updateOnlineInfo();
    }
}