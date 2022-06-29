package cn.lai.jchat.controller;

import cn.lai.jchat.HomePage;
import cn.lai.jchat.ImageUtils;
import cn.lai.jchat.UIUtils;
import cn.lai.jchat.Utils;
import cn.lai.jchat.chat.ChatManagerImpl;
import cn.lai.jchat.component.CircleImageView;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField nameInput;
    @FXML
    private Label loadingLabel;
    @FXML
    private CircleImageView avatar;

    public LoginController() {
        Platform.runLater(this::init);
    }

    private void init() {
        nameInput.setText(Utils.randomName());
        avatar.setImage(new Image(Utils.randomAvatar()));
    }

    public void login(ActionEvent event) {
        String name = nameInput.getText();
        if (name.isBlank()) {
            UIUtils.showMessage("请输入名称");
            return;
        }
        ((Button)event.getSource()).setDisable(true);
        loadingLabel.setVisible(true);
        setup(name, avatar.getImage());
    }

    private void setup(String name, Image image) {
        new Thread(() -> {
            ChatManagerImpl.getInstance().start();
            ChatManagerImpl.getInstance().updateMyself(name, ImageUtils.makeThumbnail(image, 128, 128));
            Platform.runLater(this::gotoHome);
        }).start();
    }

    private void gotoHome() {
        Window window = nameInput.getScene().getWindow();
        ((Stage) window).close();
        try {
            new HomePage().start(new Stage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
