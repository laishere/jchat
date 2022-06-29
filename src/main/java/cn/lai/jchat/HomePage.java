package cn.lai.jchat;

import cn.lai.jchat.chat.ChatManagerImpl;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class HomePage extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/home.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        if (Utils.isWindows()) {
            scene.setFill(Color.TRANSPARENT);
            stage.initStyle(StageStyle.TRANSPARENT);
            ResizeHelper.addResizeListener(stage);
        }
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        ChatManagerImpl.getInstance().stop();
    }
}