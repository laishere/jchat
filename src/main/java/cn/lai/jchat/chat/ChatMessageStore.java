package cn.lai.jchat.chat;

import cn.lai.jchat.Utils;
import cn.lai.jchat.model.ChatMessage;

import java.util.*;

public class ChatMessageStore {

    private final Map<UUID, List<ChatMessage>> records = new HashMap<>();

    private List<ChatMessage> getMessageList(UUID userId) {
        if (!records.containsKey(userId)) {
            records.put(userId, new ArrayList<>());
        }
        return records.get(userId);
    }

    public synchronized void insert(UUID userId, ChatMessage chatMessage) {
        getMessageList(userId).add(chatMessage);
    }

    public synchronized void update(UUID userId, ChatMessage chatMessage) {
        List<ChatMessage> list = getMessageList(userId);
        int index = Utils.findIndexByKey(list, chatMessage.getId(), ChatMessage::getId);
        if (index != -1) {
            list.set(index, chatMessage);
        }
    }

    public synchronized List<ChatMessage> getChatMessages(UUID userId) {
        return new ArrayList<>(getMessageList(userId));
    }

    public synchronized ChatMessage getLastMessage(UUID userId) {
        List<ChatMessage> list = getMessageList(userId);
        if (list.isEmpty()) return null;
        return list.get(list.size() - 1);
    }
}
