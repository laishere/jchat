package cn.lai.jchat.chat;

import cn.lai.jchat.Constants;
import cn.lai.jchat.Utils;
import cn.lai.jchat.model.*;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class FileManager {
    private final AtomicBoolean isAlive = new AtomicBoolean();
    private final BlockingQueue<FileTask> downloadQueue = new LinkedBlockingQueue<>();
    private final Map<FileTask.Key, FileTask> fileTaskMap = new HashMap<>();
    private final Map<UUID, File> idFileMap = new HashMap<>();
    private final Logger logger = LogManager.getLogger(getClass().getSimpleName());
    private final ChatManager chatManager;
    private final AtomicInteger downloadWorkerSize = new AtomicInteger();
    private final AtomicInteger busyDownloadWorkerSize = new AtomicInteger();
    private final Map<UUID, Set<FileTaskSubscriber>> fileTaskSubscribers = new HashMap<>();
    private final Map<FileTask.Key, FileTask> pendingUpdateTask = new HashMap<>();
    private final Timer timer = new Timer();
    private final Map<FileTask.Key, FileTask> failedDownloadTask = new HashMap<>();

    public FileManager(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    public void start() {
        if (isAlive.get()) return;
        isAlive.set(true);
    }

    public void stop() {
        if (!isAlive.get()) return;
        isAlive.set(false);
        timer.cancel();
    }

    public synchronized UUID share(File file) {
        UUID id = UUID.randomUUID();
        idFileMap.put(id, file);
        return id;
    }

    private synchronized File getSharedFile(UUID resId) {
        return idFileMap.get(resId);
    }

    public void download(UUID contextUserId, UUID resId, String fileName) {
        FileTask.Key key = new FileTask.Key(true, contextUserId, resId);
        FileTask task = new FileTask(key, fileName);
        download(task);
    }

    private synchronized void download(FileTask task) {
        FileTask.Key key = task.getKey();
        if (fileTaskMap.containsKey(key)) {
            task = fileTaskMap.get(key);
            if (task.getProgress() >= 0f) return;
        }
        updateTask(task, FileTask.IDLE, false);
        fileTaskMap.put(key, task);
        try {
            downloadQueue.put(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (downloadWorkerSize.get() < Constants.MAX_DOWNLOAD_WORKERS
                && busyDownloadWorkerSize.get() == downloadWorkerSize.get()) {
            downloadWorkerSize.incrementAndGet();
            busyDownloadWorkerSize.incrementAndGet();
            new Thread(this::downloadWorker).start();
        }
    }

    public void notifyChatSessionConnected(ChatSession chatSession) {
        List<FileTask> tasks;
        synchronized (this) {
             tasks = failedDownloadTask.values().stream()
                    .filter(it -> it.getKey().getUserId().equals(chatSession.getContextUserId()))
                    .collect(Collectors.toList());
        }
        for (FileTask task : tasks) {
            download(task);
        }
    }

    public void updateChatMessageFileTask(Collection<ChatMessage> chatMessages) {
        for (ChatMessage chatMessage : chatMessages) {
            if (chatMessage.getFile() != null) {
                FileTask.Key key = new FileTask.Key(!chatMessage.isMine(),
                        chatMessage.getContextUserId(), chatMessage.getFile().getResId());
                FileTask task;
                synchronized (this) {
                    task = fileTaskMap.get(key);
                }
                if (task == null) {
                    task = new FileTask(key, chatMessage.getFile().getFileName());
                }
                chatMessage.setFileTask(task);
            }
        }
    }

    public void updateChatMessageFileTask(ChatMessage chatMessages) {
        updateChatMessageFileTask(Collections.singleton(chatMessages));
    }

    public void addFileSession(FileSession serverSession) {
        if (serverSession.isClient()) return;
        new Thread(() -> serverWorker(serverSession)).start();
    }

    public synchronized void subscribe(UUID userId, FileTaskSubscriber subscriber) {
        if (!fileTaskSubscribers.containsKey(userId)) {
            fileTaskSubscribers.put(userId, new HashSet<>());
        }
        fileTaskSubscribers.get(userId).add(subscriber);
    }

    public synchronized void unsubscribe(UUID userId, FileTaskSubscriber subscriber) {
        if (!fileTaskSubscribers.containsKey(userId))
            return;
        fileTaskSubscribers.get(userId).remove(subscriber);
    }

    private File getDownloadFile(FileTask task) {
        File parent = new File(Constants.DOWNLOAD_PATH);
        parent.mkdirs();
        String fileName = task.getFileName();
        if (fileName == null) fileName = UUID.randomUUID().toString();
        int dotIndex = fileName.lastIndexOf('.');
        final String name = dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);
        final String ext = dotIndex == -1 ? "" : fileName.substring(dotIndex);
        String extra = "";
        int index = 1;
        while (true) {
            String fullName = name + extra + ext;
            File dst = new File(parent, fullName);
            if (!dst.exists()) return dst;
            extra = "(" + index + ")";
            index++;
        }
    }

    private void updateTask(FileTask task, float progress, boolean done) {
        task.setDone(done);
        task.setProgress(progress);
        updateTask(task);
        if (task.getKey().isDownloadTask()) {
            synchronized (this) {
                if (progress == FileTask.FAILED || progress == FileTask.CANCELED) {
                    failedDownloadTask.put(task.getKey(), task);
                } else {
                    failedDownloadTask.remove(task.getKey());
                }
            }
        }
    }

    private void updateTask(FileTask task) {
        synchronized (this) {
            if (pendingUpdateTask.isEmpty() && isAlive.get()) {
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        notifyTaskUpdate();
                    }
                }, 200);
            }
            pendingUpdateTask.put(task.getKey(), task);
        }
    }

    private void notifyTaskUpdate() {
        List<FileTask> tasks;
        synchronized (this) {
            tasks = new ArrayList<>(pendingUpdateTask.values());
        }
        for (FileTask task : tasks) {
            UUID userId = task.getKey().getUserId();
            List<FileTaskSubscriber> subscribers;
            synchronized (this) {
                if (!fileTaskSubscribers.containsKey(userId))
                    continue;
                subscribers = new ArrayList<>(fileTaskSubscribers.get(userId));
            }
            for (FileTaskSubscriber subscriber : subscribers) {
                try {
                    subscriber.onFileTaskUpdate(task);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        synchronized (this) {
            pendingUpdateTask.clear();
        }
    }

    private FileSession getOrCreateFileSession(UUID userId) {
        FileSession session = chatManager.getSessionManager().getAvailableDownloadSessionAndMarkBusy(userId);
        if (session == null) {
            session = createFileClientSession(userId);
        }
        return session;
    }

    private FileSession createFileClientSession(UUID userId) {
        ChatSession chatSession = chatManager.getSessionManager().findChatSessionByUserId(userId);
        Socket socket = null;
        try {
            socket = new Socket(chatSession.getSocket().getInetAddress(), chatSession.getRemoteServerPort());
            FileSession session = new FileSession(socket, null, null, true);
            BufferedSink sink = session.getSink();
            sink.writeUtf8(FileSession.NAME + "\n");
            sink.writeUtf8(chatManager.getMyself().getId() + "\n");
            sink.flush();
            String status = session.getSource().readUtf8Line();
            if (!"OK".equals(status)) {
                throw new IllegalStateException("状态码为 " + status);
            }
            session.setContextUserId(userId);
            session.setState(Session.State.CONNECTED);
            session.setBusy(true);
            chatManager.onSessionCreated(session);
            chatManager.onSessionConnected(session);
            return session;
        } catch (IOException e) {
            Utils.closeQuietly(socket);
            throw new RuntimeException(e);
        }
    }

    private void downloadWorker() {
        logger.debug("文件下载线程开始");
        while (isAlive.get()) {
            try {
                busyDownloadWorkerSize.decrementAndGet();
                FileTask task;
                try {
                    task = downloadQueue.poll(500, TimeUnit.MILLISECONDS);
                } finally {
                    // 确保每次循环都调用inc
                    busyDownloadWorkerSize.incrementAndGet();
                }
                if (task == null) continue;
                if (!task.startDownload()) continue;
                FileSession session;
                try {
                    session = getOrCreateFileSession(task.getKey().getUserId());
                } catch (Exception e) {
                    logger.debug("获取文件会话失败", e);
                    downloadQueue.put(task);
                    continue;
                }
                BufferedSink fileSink = null;
                try {
                    File dst = task.getFilePath() != null ? new File(task.getFilePath()) : getDownloadFile(task);
                    BufferedSink sink = session.getSink();
                    BufferedSource source = session.getSource();
                    sink.writeUtf8(task.getKey().getResId() + "\n");
                    sink.flush();
                    int chunkSize = 1024;
                    String sizeLine = source.readUtf8Line();
                    long fileSize;
                    if (sizeLine == null) {
                        // EOF
                        chatManager.notifySessionClosed(session);
                        fileSize = -1;
                    } else {
                        fileSize = Long.parseLong(sizeLine);
                    }
                    long remainSize = fileSize;
                    if (remainSize > 0) {
                        logger.debug("开始下载 " + fileSize);
                        fileSink = Okio.buffer(Okio.sink(dst));
                        task.setFilePath(dst.getAbsolutePath());
                        updateTask(task, 0f, false);
                        while (remainSize > 0 && isAlive.get() && session.isAlive()) {
                            long readSize = Math.min(remainSize, chunkSize);
                            long actual = source.read(fileSink.getBuffer(), readSize);
                            if (actual == -1) {
                                // EOF
                                chatManager.notifySessionClosed(session);
                                break;
                            }
                            fileSink.flush();
                            remainSize -= actual;
                            float progress = 1f - (float) remainSize / fileSize;
                            updateTask(task, progress, false);
//                            try {
//                                Thread.sleep(1); // 模拟慢速下载
//                            } catch (InterruptedException ignored) {
//                            }
                        }
                    }
                    if (remainSize == 0) {
                        updateTask(task, 1f, true);
                        logger.debug("下载完毕 -> " + dst.getName());
                    }
                    else {
                        updateTask(task, FileTask.CANCELED, false);
                        logger.debug("下载取消");
                    }
                } catch (Exception e) {
                    logger.debug("下载失败", e);
                    updateTask(task, FileTask.FAILED, false);
                    chatManager.notifySessionClosed(session);
                } finally {
                    session.setBusy(false);
                    Utils.closeQuietly(fileSink);
                }
            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        downloadWorkerSize.decrementAndGet();
        logger.debug("文件下载线程结束");
    }

    private synchronized FileTask getOrCreateServerTask(UUID userId, UUID resId, File file) {
        FileTask.Key key = new FileTask.Key(false, userId, resId);
        if (fileTaskMap.containsKey(key))
            return fileTaskMap.get(key);
        FileTask task = new FileTask(key, file.getName());
        fileTaskMap.put(key, task);
        return task;
    }

    private void serverWorker(FileSession session) {
        BufferedSink sink = session.getSink();
        BufferedSource source = session.getSource();
        logger.debug("文件服务线程开始 " + session);
        while (isAlive.get() && session.isAlive()) {
            FileTask task = null;
            BufferedSource fileSource = null;
            try {
                session.setBusy(false);
                String resIdLine;
                try {
                    resIdLine = source.readUtf8Line();
                } finally {
                    session.setBusy(true);
                }
                logger.debug("restId " + resIdLine + source.isOpen());
                if (resIdLine == null) {
                    // EOF
                    chatManager.notifySessionClosed(session);
                    break;
                }
                UUID resId = UUID.fromString(resIdLine);
                File file = getSharedFile(resId);
                if (file == null || !file.exists()) {
                    logger.warn("不存在文件" + resId);
                    sink.writeUtf8(-1 + "\n");
                    sink.flush();
                    continue;
                }
                task = getOrCreateServerTask(session.getContextUserId(), resId, file);
                updateTask(task, 0f, false);
                long fileSize = file.length();
                sink.writeUtf8(fileSize + "\n");
                sink.flush();
                long remainSize = fileSize;
                int chunkSize = 1024;
                fileSource = Okio.buffer(Okio.source(file));
                logger.debug("开始发送文件 " + file + " -> " + session.getContextUserId());
                while (remainSize > 0 && isAlive.get()) {
                    long writeSize = Math.min(remainSize, chunkSize);
                    sink.write(fileSource, writeSize);
                    sink.flush();
                    remainSize -= writeSize;
                    float progress = 1f - (float) remainSize / fileSize;
                    updateTask(task, progress, false);
                }
                if (remainSize == 0)
                    updateTask(task, 1f, true);
                else
                    updateTask(task, FileTask.CANCELED, false);
                logger.debug("发送文件成功");
            } catch (IOException e) {
                if (session.isAlive())
                    logger.debug("文件服务出错", e);
                if (task != null) {
                    updateTask(task, FileTask.FAILED, false);
                }
                chatManager.notifySessionClosed(session);
            } finally {
                Utils.closeQuietly(fileSource);
            }
        }
        session.setBusy(false);
        logger.debug("文件服务线程结束" + session);
    }
}
