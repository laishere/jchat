package cn.lai.jchat.model;

import okio.BufferedSink;
import okio.BufferedSource;

import java.io.IOException;
import java.net.Socket;

public class ChatSession extends Session {
    public static final String NAME = "CHAT";
    private User user;
    private int remoteServerPort;
    private final boolean isFromServer;

    public ChatSession(Socket socket, BufferedSource source, BufferedSink sink, boolean isFromServer) throws IOException {
        super(socket, source, sink);
        this.isFromServer = isFromServer;
    }

    public synchronized User getUser() {
        return user;
    }

    public synchronized void setUser(User user) {
        this.user = user;
    }

    public int getRemoteServerPort() {
        return remoteServerPort;
    }

    public void setRemoteServerPort(int remoteServerPort) {
        this.remoteServerPort = remoteServerPort;
    }

    public boolean isFromServer() {
        return isFromServer;
    }

    @Override
    public String toString() {
        return "ChatSession{" +
                "user=" + user +
                ", remoteServerPort=" + remoteServerPort +
                ", isFromServer=" + isFromServer +
                '}';
    }
}
