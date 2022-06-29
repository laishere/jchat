package cn.lai.jchat.controller;

import cn.lai.jchat.ImageUtils;
import cn.lai.jchat.component.CircleImageView;
import cn.lai.jchat.model.ChatMessage;
import cn.lai.jchat.model.ChatSessionInfo;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;


public class ItemChatSessionController {
    @FXML
    private CircleImageView avatar;
    @FXML
    private Label name;
    @FXML
    private Label lastMessage;
    @FXML
    private Node closeBtn;
    @FXML
    private Region itemWrapper;
    private HomeController homeController;
    private ChatSessionInfo chatSessionInfo;

    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    {
        Platform.runLater(this::init);
    }

    private void init() {
        itemWrapper.addEventHandler(MouseEvent.MOUSE_ENTERED, this::handleItemHover);
        itemWrapper.addEventHandler(MouseEvent.MOUSE_EXITED, this::handleItemHover);
    }

    private void handleItemHover(MouseEvent event) {
        closeBtn.setVisible(event.getEventType().equals(MouseEvent.MOUSE_ENTERED));
    }

    public void setItem(ChatSessionInfo item) {
        avatar.setImage(ImageUtils.loadImage(item.getUser().getAvatar()));
        name.setText(item.getUser().getName());
        ChatMessage message = item.getLastMessage();
        String summary = message == null ? "" : message.getSummary();
        lastMessage.setText(summary);
        chatSessionInfo = item;
    }

    public void onClickItem() {
        homeController.chatWith(chatSessionInfo.getUser());
    }

    public void onClickClose() {
        homeController.closeSession(chatSessionInfo);
    }
}
