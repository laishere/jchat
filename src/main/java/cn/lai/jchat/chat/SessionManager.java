package cn.lai.jchat.chat;

import cn.lai.jchat.Utils;
import cn.lai.jchat.model.ChatSession;
import cn.lai.jchat.model.FileSession;
import cn.lai.jchat.model.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 负责管理会话超时后的关闭和清除
 */
public class SessionManager {
    private final List<Session> sessions = new LinkedList<>();
    private long nextTimeToCheck = 0L;
    private static final long IDLE_TIME_OUT = 10 * 60 * 1000L;
    private final AtomicBoolean isAlive = new AtomicBoolean();
    private final ChatManager chatManager;
    private final Map<UUID, ChatSession> chatSessionMap = new HashMap<>();
    private final Logger logger = LogManager.getLogger(getClass().getSimpleName());

    public SessionManager(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    public void start() {
        if (isAlive.get())
            return;
        isAlive.set(true);
        new Thread(this::checkSessionLoop).start();
    }

    public synchronized void stop() {
        isAlive.set(false);
        for (Session session : sessions) {
            closeSession(session);
        }
        sessions.clear();
        notifyAll();
    }

    public void addSession(Session session) {
        synchronized (this) {
            sessions.add(session);
        }
        if (session instanceof ChatSession) {
            chatSessionMap.put(session.getContextUserId(), (ChatSession) session);
        }
    }

    public void notifySessionClosed(Session session) {
        if (session.getState() == Session.State.CLOSED)
            return;
        closeSession(session);
        List<Session> associatedSessions = null;
        synchronized (this) {
            if (session instanceof ChatSession) {
                // 关闭所有与之有关的会话
                associatedSessions = sessions.stream()
                        .filter(it -> it.getContextUserId().equals(session.getContextUserId()))
                        .collect(Collectors.toList());
            }
        }
        if (associatedSessions != null) {
            for (Session s : associatedSessions) {
                closeSession(s);
            }
        }
        synchronized (this) {
            nextTimeToCheck = 0;
            notifyAll();
        }
    }

    public synchronized List<ChatSession> getChatSessionList() {
        return sessions
                .stream()
                .filter(it -> it instanceof ChatSession)
                .map(it -> (ChatSession) it)
                .collect(Collectors.toList());
    }

    public synchronized FileSession getAvailableDownloadSessionAndMarkBusy(UUID userId) {
        FileSession session = sessions
                .stream()
                .filter(it ->
                        it instanceof FileSession &&
                        ((FileSession) it).isClient() &&
                        !it.isBusy() &&
                        it.getContextUserId().equals(userId)
                )
                .map(it -> (FileSession) it)
                .findFirst()
                .orElse(null);
        if (session == null) return null;
        session.setBusy(true);
        return session;
    }

    public synchronized ChatSession findChatSessionByUserId(UUID userId) {
        return chatSessionMap.get(userId);
    }

    private void checkSessionLoop() {
        while (isAlive.get()) {
            synchronized (this) {
                while (isAlive.get()) {
                    long delta = nextTimeToCheck - System.currentTimeMillis();
                    if (delta <= 0) break;
                    try {
                        wait(Math.min(delta, IDLE_TIME_OUT));
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            if (isAlive.get())
                checkSessions();
        }
    }

    private void checkSessions() {
        long now = System.currentTimeMillis();
        long minUpdateTime = now;
        List<Session> toRemove = new ArrayList<>();
        synchronized (this) {
            Iterator<Session> iterator = sessions.iterator();
            while (iterator.hasNext()) {
                Session session = iterator.next();
                if (session.isBusy() && session.isAlive()) continue;
                if (now - session.getUpdateTime().getTime() > IDLE_TIME_OUT ||
                        !session.isAlive() ||
                        session.getSocket().isClosed()
                ) {
                    logger.debug("关闭 " + session + " " + session.getSocket().isClosed());
                    iterator.remove();
                    toRemove.add(session);
                } else {
                    minUpdateTime = Math.min(minUpdateTime, session.getUpdateTime().getTime());
                }
            }
        }
        for (Session session : toRemove) {
            closeSession(session);
            try {
                chatManager.onSessionRemoved(session);
                if (session instanceof ChatSession) {
                    chatSessionMap.remove(session.getContextUserId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        nextTimeToCheck = minUpdateTime + IDLE_TIME_OUT;
    }

    private void closeSession(Session session) {
        session.setState(Session.State.CLOSED);
        Utils.closeQuietly(session.getSocket());
    }
}
