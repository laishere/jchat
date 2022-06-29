package cn.lai.jchat.controller;

import cn.lai.jchat.Utils;
import cn.lai.jchat.chat.*;
import cn.lai.jchat.model.ChatMessage;
import cn.lai.jchat.model.ChatSession;
import cn.lai.jchat.model.FileTask;
import cn.lai.jchat.model.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.util.Callback;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class ChatController implements MessageSubscriber, FileTaskSubscriber {

    @FXML
    private ListView<ChatMessage> chatListView;
    @FXML
    private TextArea input;
    @FXML
    private Node chatWrapper;
    @FXML
    private Node emptyWrapper;
    @FXML
    private Node sendBtn;
    @FXML
    private Label title;
    private User user;

    public ChatController() {
        Platform.runLater(this::init);
    }

    private final ObservableList<ChatMessage> chatMessages = FXCollections.observableArrayList();

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        UUID oldId = null;
        if (this.user != null)
            oldId = this.user.getId();
        System.out.println("setUser " + oldId + " " + user);
        if (oldId != null && user != null && oldId.equals(user.getId()))
            return;
        this.user = user;
        chatWrapper.setVisible(user != null);
        emptyWrapper.setVisible(user == null);
        ChatClient chatClient = ChatManagerImpl.getInstance().getChatClient();
        FileManager fileManager = ChatManagerImpl.getInstance().getFileManager();
        if (oldId != null) {
            chatClient.unsubscribe(oldId, this);
            fileManager.unsubscribe(oldId, this);
        }
        if (user != null) {
            fileManager.subscribe(user.getId(), this);
            chatClient.subscribe(user.getId(), this);
            ChatMessageStore store = ChatManagerImpl.getInstance().getChatMessageStore();
            List<ChatMessage> historyMessages = store.getChatMessages(user.getId());
            ChatManagerImpl.getInstance().getFileManager().updateChatMessageFileTask(historyMessages);
            chatMessages.clear();
            chatMessages.addAll(historyMessages);
            input.requestFocus();
            title.setText(user.getName());
            chatListView.scrollTo(chatMessages.size() - 1);
        }
    }

    private void init() {
        chatListView.setCellFactory(cellFactory);
        chatListView.setItems(chatMessages);
        setUser(user);
        input.textProperty().addListener((observable, oldValue, newValue) -> {
            sendBtn.setDisable(newValue.isBlank());
        });
        sendBtn.setDisable(true);
        input.addEventFilter(KeyEvent.ANY, keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                if (keyEvent.isControlDown()) {
                    if (keyEvent.getEventType() == KeyEvent.KEY_PRESSED)
                        input.appendText("\n");
                } else {
                    keyEvent.consume();
                    Platform.runLater(this::send);
                }
            }
        });
    }

    public void send() {
        if (user == null) return;
        String text = input.getText();
        if (text.isBlank()) return;
        ChatClient chatClient = ChatManagerImpl.getInstance().getChatClient();
        chatClient.sendMessage(user.getId(), ChatMessage.text(text));
        input.setText("");
    }

    public void pickFile() {
        if (user == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择文件");
        List<File> files = chooser.showOpenMultipleDialog(chatWrapper.getScene().getWindow());
        if (files == null || files.isEmpty()) return;
        UUID userId = user.getId();
        new Thread(() -> {
            for (File file : files) {
                ChatSession session = ChatManagerImpl.getInstance().getSessionManager().findChatSessionByUserId(userId);
                if (session == null)
                    break;
                UUID resId = ChatManagerImpl.getInstance().getFileManager().share(file);
                ChatMessage.FileType type = ChatMessage.FileResource.getFileType(file);
                ChatMessage message;
                if (type == ChatMessage.FileType.IMAGE) {
                    message = ChatMessage.image(file, resId);
                } else {
                    message = ChatMessage.file(file, resId);
                }
                ChatManagerImpl.getInstance().getChatClient().sendMessage(userId, message);
            }
        }).start();
    }

    private final Callback<ListView<ChatMessage>, ListCell<ChatMessage>> cellFactory = param -> new ListCell<>() {
        private final ItemChatMessageController left = new ItemChatMessageController(true);
        private final ItemChatMessageController right = new ItemChatMessageController(false);

        @Override
        public void updateSelected(boolean selected) {
//            super.updateSelected(selected);
        }

        @Override
        protected void updateItem(ChatMessage item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
                return;
            }
            setGraphic(item.isMine() ? right.setItem(item) : left.setItem(item));
        }
    };

    private boolean isUserInvalid(UUID userId) {
        return user == null || !user.getId().equals(userId);
    }

    @Override
    public void onNewMessage(UUID userId, ChatMessage message) {
        Platform.runLater(() -> {
            if (isUserInvalid(userId)) return;
            int index = Utils.findIndexByKey(chatMessages, message.getId(), ChatMessage::getId);
            if (index != -1) return; // 重复
            ChatMessage chatMessage = message.copy();
            ChatManagerImpl.getInstance().getFileManager().updateChatMessageFileTask(chatMessages);
            chatMessages.add(chatMessage);
            chatListView.scrollTo(chatMessages.size() - 1);
        });
    }

    @Override
    public void onMessageUpdate(UUID userId, ChatMessage message) {
        Platform.runLater(() -> {
            if (isUserInvalid(userId)) return;
            int index = Utils.findIndexByKey(chatMessages, message.getId(), ChatMessage::getId);
            if (index != -1) {
                chatMessages.set(index, message);
            }
        });
    }

    @Override
    public void onFileTaskUpdate(FileTask task) {
        Platform.runLater(() -> {
            if (isUserInvalid(task.getKey().getUserId())) return;
            int index = Utils.findIndexByKey(chatMessages, task.getKey().getResId(),
                    it -> it.getFile() == null ? null : it.getFile().getResId());
            if (index != -1) {
                ChatMessage message = chatMessages.get(index).updateFileTask(task);
                chatMessages.set(index, message);
                if (message.getType() == ChatMessage.Type.IMAGE) {
                    System.out.println("updateFileTask " + message);
                }
            }
        });
    }
}
