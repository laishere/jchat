package cn.lai.jchat.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class ChatSessionInfo {
    private User user;
    private ChatMessage lastMessage;
    private final BooleanProperty selectedProperty = new SimpleBooleanProperty();

    public ChatSessionInfo(User user, ChatMessage lastMessage) {
        this.user = user;
        this.lastMessage = lastMessage;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ChatMessage getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(ChatMessage lastMessage) {
        this.lastMessage = lastMessage;
    }

    public boolean isSelectedProperty() {
        return selectedProperty.get();
    }

    public BooleanProperty selectedPropertyProperty() {
        return selectedProperty;
    }
}
