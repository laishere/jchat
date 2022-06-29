package cn.lai.jchat.chat;

import cn.lai.jchat.model.FileTask;

public interface FileTaskSubscriber {
    void onFileTaskUpdate(FileTask task);
}
