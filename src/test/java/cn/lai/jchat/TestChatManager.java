package cn.lai.jchat;

import cn.lai.jchat.chat.*;
import cn.lai.jchat.model.OnlineUser;
import cn.lai.jchat.model.Session;
import cn.lai.jchat.model.User;

public class TestChatManager implements ChatManager {

    @Override
    public User getMyself() {
        return null;
    }

    @Override
    public SessionManager getSessionManager() {
        return null;
    }

    @Override
    public ChatClient getChatClient() {
        return null;
    }

    @Override
    public ChatServer getChatServer() {
        return null;
    }

    @Override
    public OnlineUserManager getOnlineUserManager() {
        return null;
    }

    @Override
    public ChatMessageStore getChatMessageStore() {
        return null;
    }

    @Override
    public FileManager getFileManager() {
        return null;
    }

    @Override
    public void onSessionCreated(Session session) {

    }

    @Override
    public void onSessionConnected(Session session) {

    }

    @Override
    public void onSessionRemoved(Session session) {

    }

    @Override
    public void notifySessionClosed(Session session) {

    }

    @Override
    public void onServerCreated() {

    }

    @Override
    public void onAddOnlineUser(OnlineUser user) {

    }

    @Override
    public void onRemoveOnlineUser(OnlineUser user) {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
