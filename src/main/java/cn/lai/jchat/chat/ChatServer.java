package cn.lai.jchat.chat;

import cn.lai.jchat.Utils;
import cn.lai.jchat.model.ChatSession;
import cn.lai.jchat.model.FileSession;
import cn.lai.jchat.model.Session;
import cn.lai.jchat.model.User;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatServer {
    private final AtomicBoolean isAlive = new AtomicBoolean();
    private ServerSocket serverSocket;
    private final ChatManager chatManager;
    private final Logger logger = LogManager.getLogger(getClass().getSimpleName());

    public ChatServer(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    public synchronized ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void start() {
        if (isAlive.get())
            return;
        isAlive.set(true);
        new Thread(this::listenLoop).start();
    }

    public void stop() {
        isAlive.set(false);
        Utils.closeQuietly(getServerSocket());
    }

    private ServerSocket createServerSocket() {
        try {
            ServerSocket socket = new ServerSocket(0);
            synchronized (this) {
                return serverSocket = socket;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        for (int port = 3000; port < 65535; port++) {
//            try {
//                tmpSocket = new ServerSocket(port);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    private void listenLoop() {
        final ServerSocket serverSocket = createServerSocket();
        logger.debug("正在监听 " + serverSocket.getLocalPort());
        chatManager.onServerCreated();
        while (isAlive.get()) {
            try {
                Socket socket = serverSocket.accept();
                logger.debug("接受 " + socket);
                initSession(socket);
            } catch (IOException e) {
                if (!serverSocket.isClosed())
                    e.printStackTrace();
            }
        }
        synchronized (this) {
            this.serverSocket = null;
        }
        Utils.closeQuietly(serverSocket);
    }

    private void initSession(final Socket socket) {
        new Thread(() -> {
            BufferedSource source = null;
            BufferedSink sink = null;
            try {
                source = Okio.buffer(Okio.source(socket));
                sink = Okio.buffer(Okio.sink(socket));
                String type = source.readUtf8Line();
                final Session session;
                logger.debug("type " + type);
                if (ChatSession.NAME.equals(type)) {
                    ChatSession chatSession = new ChatSession(socket, source, sink, true);
                    initChatSession(chatSession);
                    session = chatSession;
                } else if (FileSession.NAME.equals(type)) {
                    FileSession fileSession = new FileSession(socket, source, sink, false);
                    initFileSession(fileSession);
                    session = fileSession;
                } else {
                    throw new IllegalStateException("不支持的会话类型");
                }
                sink.writeUtf8("OK\n");
                sink.flush();
                chatManager.onSessionCreated(session);
                session.setState(Session.State.CONNECTED);
                chatManager.onSessionConnected(session);
                logger.debug("新会话 " + session);
            } catch (Exception e) {
                logger.warn("建立会话失败", e);
                if (sink != null) {
                    try {
                        sink.writeUtf8("ERR\n");
                        sink.flush();
                    } catch (IOException ignored) {
                    }
                }
                Utils.closeQuietly(socket);
            }
        }).start();
    }
    private void initChatSession(ChatSession session) throws IOException, ClassNotFoundException {
        initCommonSession(session);
        String userInfo = Objects.requireNonNull(session.getSource().readUtf8Line());
        User user = (User) ObjectHelper.deserialize(ByteString.decodeBase64(userInfo));
        int remoteServerPort = Integer.parseInt(Objects.requireNonNull(session.getSource().readUtf8Line()));
        session.setUser(user);
        session.setRemoteServerPort(remoteServerPort);
    }

    private void initFileSession(FileSession session) throws IOException {
        initCommonSession(session);
    }

    private void initCommonSession(Session session) throws IOException {
        String uuid = Objects.requireNonNull(session.getSource().readUtf8Line());
        session.setContextUserId(UUID.fromString(uuid));
    }
}
