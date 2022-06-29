package cn.lai.jchat;

import javafx.scene.control.Alert;

public class UIUtils {
    public static void showMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(message);
        alert.showAndWait();
    }
}
