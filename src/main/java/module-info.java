module cn.lai.jchat {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires okio;
    requires org.apache.logging.log4j;
    requires thumbnailator;

    opens cn.lai.jchat.controller to javafx.fxml;
    opens cn.lai.jchat.component to javafx.fxml;
    exports cn.lai.jchat;
}