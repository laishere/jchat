package cn.lai.jchat;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.coobird.thumbnailator.Thumbnails;
import okio.Buffer;
import okio.ByteString;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class ImageUtils {
    public static String makeThumbnail(Image image, int maxWidth, int maxHeight) {
        BufferedImage img = SwingFXUtils.fromFXImage(image, null);
        try (Buffer buffer = new Buffer()) {
            Thumbnails.of(img)
                    .size(maxWidth, maxHeight)
                    .outputFormat("jpg")
                    .toOutputStream(buffer.outputStream());
            return buffer.readByteString().base64();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Image imageFromFile(File file) {
        String url = "file:" + file.getAbsolutePath();
        return new Image(url);
    }

    public static Image loadImage(String base64) {
        if (base64 != null) {
            ByteString byteString = ByteString.decodeBase64(base64);
            if (byteString != null) {
                return new Image(new ByteArrayInputStream(byteString.toByteArray()));
            }
        }
        return new Image(HomePage.class.getResource("/images/placeholder.png").toExternalForm());
    }

    public static Image loadImage(String base64, double width, double height) {
        if (width < 0 || height < 0)
            return loadImage(base64);
        if (base64 != null) {
            ByteString byteString = ByteString.decodeBase64(base64);
            if (byteString != null) {
                return new Image(new ByteArrayInputStream(byteString.toByteArray()), width, height, true, false);
            }
        }
        return new Image(HomePage.class.getResource("/images/placeholder.png").toExternalForm());
    }
}
