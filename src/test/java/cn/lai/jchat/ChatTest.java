package cn.lai.jchat;

import cn.lai.jchat.chat.ChatListener;
import cn.lai.jchat.chat.ChatManager;
import cn.lai.jchat.chat.ChatManagerImpl;
import cn.lai.jchat.chat.MessageSubscriber;
import cn.lai.jchat.model.ChatMessage;
import cn.lai.jchat.model.ChatSession;
import cn.lai.jchat.model.OnlineUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatTest {

    @Test
    public void testChat() {
        List<String> names = new ArrayList<>();
        final int size = 10;
        final int sessionSize = 2 * (size - 1);
        final Map<Integer, List<ChatMessage>> messages = new HashMap<>();
        List<ChatManager> managers = new ArrayList<>();
        final AtomicInteger doneClients = new AtomicInteger();
        for (int i = 0; i < size; i++) {
            names.add("" + i);
            final int id = i;
            messages.put(id, new ArrayList<>());
            final ChatManagerImpl manager = new ChatManagerImpl();
            manager.setChatListener(new ChatListener() {

                @Override
                public void onAddOnlineUser(OnlineUser onlineUser) {
                    onOnlineUserListChanged(manager.getOnlineUserManager().getOnlineUsers());
                }

                @Override
                public void onRemoveOnlineUser(OnlineUser onlineUser) {
                    onOnlineUserListChanged(manager.getOnlineUserManager().getOnlineUsers());
                }

                @Override
                public void onAddChatSession(ChatSession chatSession) {
                    onChatSessionListChanged(manager.getSessionManager().getChatSessionList());
                }

                @Override
                public void onRemoveChatSession(ChatSession chatSession) {
                    onChatSessionListChanged(manager.getSessionManager().getChatSessionList());
                }

                private boolean isConnected = false;
                public synchronized void onOnlineUserListChanged(List<OnlineUser> users) {
                    if (users.size() > size - 1) {
                        throw new RuntimeException("" + users.size());
                    }
                    if (users.size() == size - 1 && !isConnected) {
                        isConnected = true;
                        for (OnlineUser user : users) {
                            manager.getChatClient().connect(user);
                            manager.getChatClient().subscribe(user.getUser().getId(), subscriber);
                        }
                        System.out.println("all connect");
                    }
                }

                private boolean isSent = false;
                public synchronized void onChatSessionListChanged(List<ChatSession> chatSessions) {
                    if (chatSessions.size() > sessionSize) {
                        throw new RuntimeException("" + chatSessions.size());
                    }
                    if (chatSessions.size() == sessionSize && !isSent) {
                        isSent = true;
                        for (ChatSession session : chatSessions) {
                            ChatMessage chatMessage = ChatMessage.text("" + id);
                            manager.getChatClient().sendMessage(session.getContextUserId(), chatMessage);
                        }
                        System.out.println("all send");
                    }
                }

                private final MessageSubscriber subscriber = new MessageSubscriber() {
                    @Override
                    public void onNewMessage(UUID userId, ChatMessage message) {
                        if (message.getState() != ChatMessage.State.OK) return;
                        synchronized (messages) {
                            messages.get(id).add(message);
                            if (messages.get(id).size() == sessionSize) {
                                int done = doneClients.incrementAndGet();
                                System.out.println("done " + done);
                                messages.notifyAll();
                            }
                        }
                    }

                    @Override
                    public void onMessageUpdate(UUID userId, ChatMessage message) {

                    }
                };
            });
            manager.start();
            manager.updateMyself(names.get(i), null);
            managers.add(manager);
        }
        synchronized (messages) {
            while (doneClients.get() < size) {
                try {
                    messages.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
        for (ChatManager chatManager : managers) {
            chatManager.stop();
        }
        Map<String, Integer> cnt = new HashMap<>();
        for (List<ChatMessage> list : messages.values()) {
            Assertions.assertEquals(sessionSize, list.size()); // 每个客户端接收到 sessionSize 条消息
            for (ChatMessage message : list) {
                if (!cnt.containsKey(message.getText())) {
                    cnt.put(message.getText(), 1);
                } else {
                    cnt.put(message.getText(), cnt.get(message.getText()) + 1);
                }
            }
        }
        Assertions.assertEquals(size, cnt.size()); // 总共size个客户端
        for (Integer c : cnt.values()) {
            Assertions.assertEquals(sessionSize, c); // 每个客户端收发送 sessionSize 条消息
        }
    }
}
