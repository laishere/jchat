package cn.lai.jchat;

import javafx.application.Platform;

import javax.swing.*;
import java.io.Closeable;
import java.io.IOException;
import java.util.Locale;

public class Utils {
    public static void closeQuietly(Closeable ...closeableList) {
        for (Closeable closeable : closeableList) {
            if (closeable == null) continue;
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static <T, K> int findIndexByKey(Iterable<T> list, K key, Indicator<T, K> indicator) {
        if (list == null) return -1;
        if (key == null) return -1;
        int i = 0;
        for (T t : list) {
            if (key.equals(indicator.getKey(t))) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public interface Indicator<T, K> {
        K getKey(T item);
    }

    public static String randomName() {
        int size = 5;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            int c = (int)(Math.random() * 26);
            sb.append((char) (c + 'a'));
        }
        return "用户" + sb;
    }

    public static String randomAvatar() {
        int index = (int)(Math.random() * 6) + 1;
        return Utils.class.getResource("/images/avatars/" + index + ".png").toExternalForm();
    }

    public static String formatSize(double size) {
        String[] suffix = new String[] {"B", "K", "M"};
        for (String s : suffix) {
            if (size < 1024) {
                return Math.round(size) + s;
            }
            size /= 1024;
        }
        return Math.round(size) + "G";
    }

    public static boolean isCommonFile(String path) {
        return path.matches(".*?\\.(jpg|jpeg|png|gif|webp|bmp|mp3|wav|mp4|" +
                "zip|txt|rar|doc|docx|xls|xlsx|ppt|pptx|pdf)$");
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
    }

    public static void postDelayed(int millis, Runnable runnable) {
        Timer timer = new Timer(millis, e -> Platform.runLater(runnable));
        timer.setRepeats(false);
        timer.start();
    }
}
