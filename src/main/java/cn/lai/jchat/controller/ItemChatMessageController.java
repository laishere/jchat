package cn.lai.jchat.controller;

import cn.lai.jchat.ImageUtils;
import cn.lai.jchat.UIUtils;
import cn.lai.jchat.Utils;
import cn.lai.jchat.component.ChatImageView;
import cn.lai.jchat.component.CircleImageView;
import cn.lai.jchat.model.ChatMessage;
import cn.lai.jchat.model.FileTask;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

public class ItemChatMessageController implements ItemController<ChatMessage> {

    private Node textRoot;
    private Node imageRoot;
    private Node fileRoot;
    @FXML
    private CircleImageView textAvatar, imageAvatar, fileAvatar;
    @FXML
    private Node imageWrapper, fileWrapper;
    @FXML
    private Label text;
    @FXML
    private ChatImageView image;
    @FXML
    private Label fileTitle, fileStatus;
    @FXML
    private ImageView fileIcon;
    private final boolean isLeft;

    public ItemChatMessageController(boolean isLeft) {
        this.isLeft = isLeft;
    }

    @Override
    public Node setItem(ChatMessage item) {
        switch (item.getType()) {
            case TEXT: return setText(item);
            case IMAGE: return setImage(item);
            case FILE: return setFile(item);
        }
        return null;
    }

    private URL getResource(String name) {
        String path = String.format("/item_chat_%s_%s.fxml", name, isLeft ? "left" : "right");
        return getClass().getResource(path);
    }

    private void initTextIfNeeded() {
        if (textRoot != null) return;
        FXMLLoader loader = new FXMLLoader(getResource("text"));
        loader.setController(this);
        try {
            textRoot = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initImageIfNeeded() {
        if (imageRoot != null) return;
        FXMLLoader loader = new FXMLLoader(getResource("image"));
        loader.setController(this);
        try {
            imageRoot = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initFileIfNeeded() {
        if (fileRoot != null) return;
        FXMLLoader loader = new FXMLLoader(getResource("file"));
        loader.setController(this);
        try {
            fileRoot = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Node setText(ChatMessage item) {
        initTextIfNeeded();
        textAvatar.setImage(ImageUtils.loadImage(item.getSender().getAvatar()));
        text.setText(item.getText());
        return textRoot;
    }

    private Node setImage(ChatMessage item) {
        initImageIfNeeded();
        imageAvatar.setImage(ImageUtils.loadImage(item.getSender().getAvatar()));
        image.setChatMessageImage(item);
        imageWrapper.setOnMouseClicked(e -> onClickFile(item));
        return imageRoot;
    }

    private Node setFile(ChatMessage item) {
        initFileIfNeeded();
        fileAvatar.setImage(ImageUtils.loadImage(item.getSender().getAvatar()));
        ChatMessage.FileResource res = item.getFile();
        fileTitle.setText(res.getFileName());
        setFileIcon(res.getFileName());
        FileTask task = item.getFileTask();
        if (task == null) {
            fileStatus.setText("无下载任务");
        } else if (task.isDone()) {
            fileStatus.setText(Utils.formatSize(res.getFileSize()));
        } else {
            String prefix = task.getKey().isDownloadTask() ? "接收" : "发送";
            String status;
            if (task.getProgress() == FileTask.CANCELED)
                status = prefix + "已取消";
            else if (task.getProgress() == FileTask.FAILED)
                status = prefix + "失败";
            else if (task.getProgress() == FileTask.IDLE)
                status = "等待" + prefix + "中";
            else
                status = prefix + "中 " + Math.round(task.getProgress() * 100) + "%";
            fileStatus.setText(status);
        }
        fileWrapper.setOnMouseClicked(e -> onClickFile(item));
        return fileRoot;
    }

    private final static String[][] fileTypeMap = new String[][] {
            {"zip", "zip", "rar", "tar", "gz"},
            {"txt", "txt"},
            {"doc", "doc", "docx"},
            {"pdf", "pdf"},
            {"xls", "xls", "xlsx"},
            {"ppt", "ppt", "pptx"},
            {"audio", "mp3", "wav"},
            {"video", "mp4", "flv", "avi"},
            {"other"}
    };

    private final static Image[] fileIconMap = new Image[fileTypeMap.length];

    private static Image getFileIcon(int index) {
        if (fileIconMap[index] == null) {
            String path = String.format("/images/ic_%s.png", fileTypeMap[index][0]);
            String url = ItemChatMessageController.class.getResource(path).toExternalForm();
            fileIconMap[index] = new Image(url);
        }
        return fileIconMap[index];
    }

    private void setFileIcon(String name) {
        int i;
        for (i = 0; i < fileTypeMap.length - 1; i++) {
            String[] extArr = fileTypeMap[i];
            int j;
            for (j = 1; j < extArr.length; j++) {
                if (name.endsWith('.' + extArr[j])) {
                    break;
                }
            }
            if (j < extArr.length) {
                break;
            }
        }
        fileIcon.setImage(getFileIcon(i));
    }

    private void onClickFile(ChatMessage item) {
        ChatMessage.FileResource resource = item.getFile();
        String path = resource.getLocalFilePath();
        if (path == null && item.getFileTask() != null) {
            path = item.getFileTask().getFilePath();
        }
        if (path != null) {
            File file = new File(path);
            if (!file.exists()) {
                UIUtils.showMessage("文件不存在");
                return;
            }
            try {
                if (Utils.isCommonFile(path)) {
                    Desktop.getDesktop().open(file);
                }
                else {
                    String[] cmdArr;
                    String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
                    if (os.startsWith("mac")) {
                        cmdArr = new String[] {"open", "-R", path};
                    } else if (os.startsWith("windows")) {
                        cmdArr = new String[] {"explorer", "/select,", path};
                    } else {
                        throw new RuntimeException("不支持的操作系统" + os);
                    }
                    Runtime.getRuntime().exec(cmdArr);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
