package cn.lai.jchat.chat;

import cn.lai.jchat.model.ChatSession;
import cn.lai.jchat.model.OnlineUser;

public interface ChatListener {
    void onAddOnlineUser(OnlineUser onlineUser);
    void onRemoveOnlineUser(OnlineUser onlineUser);
    void onAddChatSession(ChatSession chatSession);
    void onRemoveChatSession(ChatSession chatSession);
}