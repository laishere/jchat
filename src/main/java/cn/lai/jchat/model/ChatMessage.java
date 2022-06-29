package cn.lai.jchat.model;

import cn.lai.jchat.ImageUtils;
import javafx.scene.image.Image;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class ChatMessage implements Serializable {
    private final UUID id;
    private final String text;

    private final String thumbnail;
    private final double imageWidth;
    private final double imageHeight;
    private final FileResource file;
    private final Type type;
    private final Date createTime;

    private transient UUID contextUserId;
    private transient User sender;
    private transient State state;
    private transient FileTask fileTask;
    private transient boolean isMine;

    private ChatMessage(String text, String thumbnail, FileResource file, Type type, double imageWidth, double imageHeight) {
        this.id = UUID.randomUUID();
        this.text = text;
        this.thumbnail = thumbnail;
        this.file = file;
        this.type = type;
        this.createTime = new Date();
        this.isMine = true;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public ChatMessage copy() {
        ChatMessage chatMessage = copyWithoutFileTask();
        FileTask task = getFileTask();
        if (task != null)
            task = task.copy();
        chatMessage.setFileTask(task);
        return chatMessage;
    }

    private ChatMessage copyWithoutFileTask() {
        ChatMessage chatMessage = new ChatMessage(getText(), getThumbnail(), getFile(), getType(), getImageWidth(), getImageHeight());
        chatMessage.setSender(getSender());
        chatMessage.setState(getState());
        chatMessage.setMine(isMine());
        chatMessage.setContextUserId(getContextUserId());
        return chatMessage;
    }

    public ChatMessage updateFileTask(FileTask task) {
        ChatMessage chatMessage = copyWithoutFileTask();
        chatMessage.setFileTask(task.copy());
        return chatMessage;
    }

    public static ChatMessage text(String text) {
        return new ChatMessage(text, null, null, Type.TEXT, -1, -1);
    }

    public static ChatMessage image(File file, UUID resId) {
        Image image = null;
        String thumbnail = null;
        try {
            image = ImageUtils.imageFromFile(file);
            thumbnail = ImageUtils.makeThumbnail(image, 64, 64);
        } catch (Exception ignored) {
        }
        if (image == null) {
            return file(file, resId);
        }
        return new ChatMessage(null,
                thumbnail,
                FileResource.createFileResource(file, resId),
                Type.IMAGE,
                image.getWidth(),
                image.getHeight()
        );
    }

    public static ChatMessage file(File file, UUID resId) {
        return new ChatMessage(null,
                null,
                FileResource.createFileResource(file, resId),
                Type.FILE,
                -1,
                -1
        );
    }

    public UUID getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public FileResource getFile() {
        return file;
    }

    public Type getType() {
        return type;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public FileTask getFileTask() {
        return fileTask;
    }

    public void setFileTask(FileTask fileTask) {
        this.fileTask = fileTask;
    }

    public boolean isMine() {
        return isMine;
    }

    public void setMine(boolean mine) {
        isMine = mine;
    }

    public double getImageWidth() {
        return imageWidth;
    }

    public double getImageHeight() {
        return imageHeight;
    }

    public UUID getContextUserId() {
        return contextUserId;
    }

    public void setContextUserId(UUID contextUserId) {
        this.contextUserId = contextUserId;
    }

    public String getSummary() {
        String summary = "";
        switch (getType()) {
            case TEXT:
                summary = text;
                break;
            case IMAGE:
                summary = "[图片]";
                break;
            case FILE:
                summary = "[文件]";
                FileTask task = getFileTask();
                if (task != null && !task.isDone()) {
                    String action = task.getKey().isDownloadTask() ? "接收" : "发送";
                    float p = task.getProgress();
                    summary += " ";
                    if (p == FileTask.FAILED) summary += action + "失败";
                    else if (p == FileTask.CANCELED) summary += "取消" + action;
                    else if (p == FileTask.IDLE) summary += "等待" + action;
                    else summary += String.format("%.1f%%", p * 100);
                }
                break;
        }
        return summary;
    }

    public enum Type implements Serializable {
        TEXT, IMAGE, FILE
    }

    public enum State implements Serializable {
        SENDING, OK, FAILED
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return Double.compare(that.getImageWidth(), getImageWidth()) == 0 && Double.compare(that.getImageHeight(), getImageHeight()) == 0 && isMine() == that.isMine() && Objects.equals(getId(), that.getId()) && Objects.equals(getText(), that.getText()) && Objects.equals(getThumbnail(), that.getThumbnail()) && Objects.equals(getFile(), that.getFile()) && getType() == that.getType() && Objects.equals(getCreateTime(), that.getCreateTime()) && Objects.equals(getContextUserId(), that.getContextUserId()) && Objects.equals(getSender(), that.getSender()) && getState() == that.getState() && Objects.equals(getFileTask(), that.getFileTask());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getText(), getThumbnail(), getImageWidth(), getImageHeight(), getFile(), getType(), getCreateTime(), getContextUserId(), getSender(), getState(), getFileTask(), isMine());
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id=" + id +
                ", text='" + text + '\'' +
                ", thumbnail='" + thumbnail + '\'' +
                ", imageWidth=" + imageWidth +
                ", imageHeight=" + imageHeight +
                ", file=" + file +
                ", type=" + type +
                ", createTime=" + createTime +
                ", contextUserId=" + contextUserId +
                ", sender=" + sender +
                ", state=" + state +
                ", fileTask=" + fileTask +
                ", isMine=" + isMine +
                '}';
    }

    public static class FileResource implements Serializable {
        private final String fileName;
        private final FileType type;
        private final long fileSize;
        private final UUID resId;
        private transient String localFilePath;

        private FileResource(String fileName, FileType type, long fileSize, UUID resId) {
            this.fileName = fileName;
            this.type = type;
            this.fileSize = fileSize;
            this.resId = resId;
        }

        public String getFileName() {
            return fileName;
        }

        public FileType getType() {
            return type;
        }

        public long getFileSize() {
            return fileSize;
        }

        public UUID getResId() {
            return resId;
        }

        public String getLocalFilePath() {
            return localFilePath;
        }

        public void setLocalFilePath(String localFilePath) {
            this.localFilePath = localFilePath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FileResource that = (FileResource) o;
            return getFileSize() == that.getFileSize() && Objects.equals(getFileName(), that.getFileName()) && getType() == that.getType() && Objects.equals(getResId(), that.getResId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getFileName(), getType(), getFileSize(), getResId());
        }

        @Override
        public String toString() {
            return "FileResource{" +
                    "fileName='" + fileName + '\'' +
                    ", type=" + type +
                    ", fileSize=" + fileSize +
                    ", resId=" + resId +
                    '}';
        }

        public static FileResource createFileResource(File file, UUID resId) {
            FileResource resource = new FileResource(file.getName(), getFileType(file), file.length(), resId);
            resource.localFilePath = file.getAbsolutePath();
            return resource;
        }

        public static FileType getFileType(File file) {
            String name = file.getName();
            if (name.matches(".*?\\.(jpg|png|gif|bmp|jpeg)$")) {
                return FileType.IMAGE;
            }
            return FileType.OTHER;
        }
    }

    public enum FileType implements Serializable {
        IMAGE, OTHER
    }
}
