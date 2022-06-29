package cn.lai.jchat;

import cn.lai.jchat.chat.OnlineUserManager;
import cn.lai.jchat.model.OnlineUser;
import cn.lai.jchat.model.User;
import okio.ByteString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class OnlineUserManagerTest {
    @Test
    public void test() {
        final List<OnlineUser> userList = new ArrayList<>();
        OnlineUserManager mgr1 = new OnlineUserManager(
                new TestChatManager() {
                    @Override
                    public void onAddOnlineUser(OnlineUser user) {
                        synchronized (userList) {
                            userList.add(user);
                            userList.notify();
                        }
                    }
                }
        );
        mgr1.start();
        List<OnlineUser> onlineUsers = new ArrayList<>();
        List<UUID> userIdList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setId(UUID.randomUUID());
            user.setName("张三");
            user.setAvatar(ByteString.of(new byte[32 * 1024]).base64());
            OnlineUser onlineUser = new OnlineUser();
            onlineUser.setUser(user);
            onlineUsers.add(onlineUser);
            userIdList.add(user.getId());
            OnlineUserManager mgr2 = new OnlineUserManager(
                    new TestChatManager()
            );
            mgr2.start();
            mgr2.updateMyself(onlineUser);
        }
        synchronized (userList) {
            while (userList.size() < onlineUsers.size()) {
                try {
                    userList.wait();
                } catch (Exception ignored) {
                }
            }
        }
        List<UUID> actualIdList = new ArrayList<>();
        for (OnlineUser user : userList) {
            actualIdList.add(user.getUser().getId());
        }
        Collections.sort(userIdList);
        Collections.sort(actualIdList);
        Assertions.assertEquals(userIdList, actualIdList);
    }
}
