package cn.lai.jchat.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class OnlineUser implements Serializable {
    private User user;
    private String host;
    private int port;
    private Date lastActiveTime;

    public OnlineUser() {
    }

    public OnlineUser(User user, String host, int port, Date lastActiveTime) {
        this.user = user;
        this.host = host;
        this.port = port;
        this.lastActiveTime = lastActiveTime;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Date getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(Date lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OnlineUser that = (OnlineUser) o;
        return port == that.port && Objects.equals(user, that.user) && Objects.equals(host, that.host) && Objects.equals(lastActiveTime, that.lastActiveTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, host, port, lastActiveTime);
    }

    @Override
    public String toString() {
        return "OnlineUser{" +
                "user=" + user +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", lastActiveTime=" + lastActiveTime +
                '}';
    }
}
