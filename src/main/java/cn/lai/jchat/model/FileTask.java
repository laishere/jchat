package cn.lai.jchat.model;

import java.util.Objects;
import java.util.UUID;

public class FileTask {
    public final static float FAILED = -1F;
    public final static float CANCELED = -2F;
    public final static float IDLE = -3F;

    private final Key key;
    private final String fileName;
    private float progress;
    private boolean isDone;
    private String filePath;

    public FileTask(Key key, String fileName) {
        this.key = key;
        this.fileName = fileName;
        this.progress = IDLE;
    }

    public Key getKey() {
        return key;
    }

    public String getFileName() {
        return fileName;
    }

    public synchronized float getProgress() {
        return progress;
    }

    public synchronized void setProgress(float progress) {
        this.progress = progress;
    }

    public synchronized boolean startDownload() {
        if (progress >= 0f) return false;
        progress = 0f;
        return true;
    }

    public synchronized boolean isDone() {
        return isDone;
    }

    public synchronized void setDone(boolean done) {
        isDone = done;
    }

    public synchronized String getFilePath() {
        return filePath;
    }

    public synchronized void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public FileTask copy() {
        FileTask task = new FileTask(key, fileName);
        task.setDone(isDone());
        task.setFilePath(getFilePath());
        task.setProgress(getProgress());
        return task;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileTask task = (FileTask) o;
        return Float.compare(task.getProgress(), getProgress()) == 0 && isDone() == task.isDone() && Objects.equals(getKey(), task.getKey()) && Objects.equals(getFileName(), task.getFileName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getFileName(), getProgress(), isDone());
    }

    @Override
    public String toString() {
        return "FileTask{" +
                "key=" + key +
                ", fileName='" + fileName + '\'' +
                ", progress=" + progress +
                ", isDone=" + isDone +
                '}';
    }

    public static class Key {
        private final boolean isDownloadTask;
        private final UUID userId;
        private final UUID resId;

        public Key(boolean isDownloadTask, UUID userId, UUID resId) {
            this.isDownloadTask = isDownloadTask;
            this.userId = userId;
            this.resId = resId;
        }

        public boolean isDownloadTask() {
            return isDownloadTask;
        }

        public UUID getUserId() {
            return userId;
        }

        public UUID getResId() {
            return resId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return isDownloadTask() == key.isDownloadTask() && Objects.equals(getUserId(), key.getUserId()) && Objects.equals(getResId(), key.getResId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(isDownloadTask(), getUserId(), getResId());
        }

        @Override
        public String toString() {
            return "Key{" +
                    "isDownloadTask=" + isDownloadTask +
                    ", userId=" + userId +
                    ", resId='" + resId + '\'' +
                    '}';
        }
    }
}
