package cn.lai.jchat.component;

import cn.lai.jchat.ImageUtils;
import cn.lai.jchat.model.ChatMessage;
import cn.lai.jchat.model.FileTask;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.Map;

public class ChatImageView extends ImageView {
    private double originalFitWidth = -1.0;
    private double originalFitHeight = -1.0;
    private final static double MAX_LOAD_WIDTH = 200;
    private final static double MAX_LOAD_HEIGHT = 200;
    private final static Map<String, Image> imageCache = new HashMap<>();

    private synchronized static Image getImage(String path) {
        String url = "file:" + path;
        if (!imageCache.containsKey(url)) {
            Image image = new Image(url, MAX_LOAD_WIDTH, MAX_LOAD_HEIGHT, true, true, true);
            imageCache.put(url, image);
        }
        return imageCache.get(url);
    }

    public ChatImageView() {
        Rectangle clipShape = new Rectangle();
        clipShape.setArcWidth(20);
        clipShape.setArcHeight(20);
        imageProperty().addListener((observable, oldValue, newValue) -> {
            updateFitSize();
        });
        fitWidthProperty().addListener((observable, oldValue, newValue) -> {
            if (originalFitWidth == -1.0 && newValue.doubleValue() > 0) {
                originalFitWidth = newValue.doubleValue();
                updateFitSize();
            }
        });
        fitHeightProperty().addListener((observable, oldValue, newValue) -> {
            if (originalFitHeight == -1.0 && newValue.doubleValue() > 0) {
                originalFitHeight = newValue.doubleValue();
                updateFitSize();
            }
        });
        layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            clipShape.setWidth(newValue.getWidth());
            clipShape.setHeight(newValue.getHeight());
        });
        setClip(clipShape);
        setPreserveRatio(true);
    }

    public void setChatMessageImage(ChatMessage message) {
        if (message.getType() != ChatMessage.Type.IMAGE)
            return;
        double w = Math.min(MAX_LOAD_WIDTH, message.getImageWidth());
        double h = Math.min(MAX_LOAD_HEIGHT, message.getImageHeight());
        if (originalFitWidth > 0) w = originalFitWidth;
        if (originalFitHeight > 0) h = originalFitHeight;
        String localPath = message.getFile().getLocalFilePath();
        if (localPath == null) {
            FileTask task = message.getFileTask();
            if (task != null && task.isDone())
                localPath = task.getFilePath();
        }
        boolean isImageLoaded = false;
        if (localPath != null) {
            Image image = getImage(localPath);
            Runnable updateImage = () -> {
                setImage(image);
                setEffect(null);
            };
            if (image.getProgress() == 1.0) {
                updateImage.run();
                isImageLoaded = true;
            } else image.progressProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.doubleValue() == 1.0)
                    updateImage.run();
            });
        }
        if (!isImageLoaded && message.getThumbnail() != null) {
            Image image = ImageUtils.loadImage(message.getThumbnail(), w, h);
            setImage(image);
            GaussianBlur blur = new GaussianBlur();
            blur.setRadius(10);
            setEffect(blur);
        }
    }

    private void updateFitSize() {
        Image image = getImage();
        if (image == null)
            return;
        if (originalFitWidth > 0)
            setFitWidth(Math.min(image.getWidth(), originalFitWidth));
        if (originalFitHeight > 0)
            setFitHeight(Math.min(image.getHeight(), originalFitHeight));
//        System.out.println(getFitWidth() + " " + getFitHeight());
    }
}
