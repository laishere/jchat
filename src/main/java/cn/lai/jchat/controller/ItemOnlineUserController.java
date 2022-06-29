package cn.lai.jchat.controller;

import cn.lai.jchat.ImageUtils;
import cn.lai.jchat.chat.ChatManagerImpl;
import cn.lai.jchat.component.CircleImageView;
import cn.lai.jchat.model.OnlineUser;
import javafx.fxml.FXML;
import javafx.scene.control.Label;


public class ItemOnlineUserController {
    @FXML
    private CircleImageView avatar;
    @FXML
    private Label name;
    private OnlineUser onlineUser;
    private HomeController homeController;

    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    public void setItem(OnlineUser item) {
        onlineUser = item;
        avatar.setImage(ImageUtils.loadImage(item.getUser().getAvatar()));
        name.setText(item.getUser().getName() + "按实际大世纪大劫案世纪大事加大静安寺的");
    }

    public void connect() {
        if (onlineUser == null)
            return;
        ChatManagerImpl.getInstance().getChatClient().connect(onlineUser);
        homeController.onClickSessionTab();
    }
}
