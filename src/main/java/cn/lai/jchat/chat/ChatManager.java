package cn.lai.jchat.chat;

import cn.lai.jchat.model.OnlineUser;
import cn.lai.jchat.model.Session;
import cn.lai.jchat.model.User;

public interface ChatManager {
    User getMyself();

    SessionManager getSessionManager();

    ChatClient getChatClient();

    ChatServer getChatServer();

    OnlineUserManager getOnlineUserManager();

    ChatMessageStore getChatMessageStore();

    FileManager getFileManager();

    void onSessionCreated(Session session);

    void onSessionConnected(Session session);

    void onSessionRemoved(Session session);

    void notifySessionClosed(Session session);

    void onServerCreated();

    void onAddOnlineUser(OnlineUser user);

    void onRemoveOnlineUser(OnlineUser user);

    void start();

    void stop();
}
