package cn.lai.jchat;

import cn.lai.jchat.chat.ChatManagerImpl;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginPage extends Application {


    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("登录");
        stage.setScene(scene);
        stage.show();
        Platform.setImplicitExit(true);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        ChatManagerImpl.getInstance().stop();
    }

    public static void main(String[] args) {
        launch();
    }
}
