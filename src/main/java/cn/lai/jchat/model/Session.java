package cn.lai.jchat.model;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class Session {
    private UUID contextUserId;
    private final Socket socket;
    private final BufferedSource source;
    private final BufferedSink sink;
    private final Date createTime;
    private State state;
    private Date updateTime;
    private boolean isBusy;

    public Session(Socket socket, BufferedSource source, BufferedSink sink) throws IOException {
        this.source = source != null ? source : Okio.buffer(Okio.source(socket));
        this.sink = sink != null ? sink : Okio.buffer(Okio.sink(socket));
        this.socket = socket;
        createTime = new Date();
        updateTime = new Date();
        state = State.CONNECTING;
        isBusy = false;
    }

    public synchronized UUID getContextUserId() {
        return contextUserId;
    }

    public synchronized void setContextUserId(UUID contextUserId) {
        this.contextUserId = contextUserId;
    }

    public synchronized Socket getSocket() {
        return socket;
    }

    public BufferedSource getSource() {
        return source;
    }

    public BufferedSink getSink() {
        return sink;
    }

    public synchronized Date getCreateTime() {
        return createTime;
    }

    public synchronized State getState() {
        return state;
    }

    public synchronized void setState(State state) {
        this.state = state;
    }

    public synchronized Date getUpdateTime() {
        return updateTime;
    }

    public synchronized void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public synchronized boolean isBusy() {
        return isBusy;
    }

    public synchronized void setBusy(boolean busy) {
        isBusy = busy;
    }

    public synchronized boolean isAlive() {
        return state == State.CONNECTED;
    }

    public enum State {
        CONNECTING, CONNECTED, CLOSED
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return isBusy() == session.isBusy() && Objects.equals(getContextUserId(), session.getContextUserId()) && Objects.equals(getSocket(), session.getSocket()) && Objects.equals(getCreateTime(), session.getCreateTime()) && getState() == session.getState() && Objects.equals(getUpdateTime(), session.getUpdateTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getContextUserId(), getSocket(), getCreateTime(), getState(), getUpdateTime(), isBusy());
    }
}
