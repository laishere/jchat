package cn.lai.jchat.chat;

import cn.lai.jchat.model.ChatMessage;

import java.util.UUID;

public interface MessageSubscriber {
    void onNewMessage(UUID userId, ChatMessage message);
    void onMessageUpdate(UUID userId, ChatMessage message);
}
