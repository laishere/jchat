package cn.lai.jchat.controller;

import cn.lai.jchat.ImageUtils;
import cn.lai.jchat.Utils;
import cn.lai.jchat.chat.*;
import cn.lai.jchat.component.CircleImageView;
import cn.lai.jchat.model.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class HomeController implements ChatListener, MessageSubscriber, FileTaskSubscriber {

    @FXML
    private Label name;
    @FXML
    private CircleImageView avatar;
    @FXML
    private ListView<ChatSessionInfo> sessionListView;
    @FXML
    private ListView<OnlineUser> discoverListView;
    @FXML
    private Label tabItemSession;
    @FXML
    private Label tabItemDiscover;
    @FXML
    private StackPane chatWrapper;
    @FXML
    private ImageView maximizeImage;
    @FXML
    private HBox mainWrapper;
    @FXML
    private Region rootWrapper;
    @FXML
    private Node topBtnWrapper;

    private ChatController chatController;
    private final ObservableList<ChatSessionInfo> chatSessionInfos = FXCollections.observableArrayList();
    private final ObservableList<OnlineUser> onlineUsers = FXCollections.observableArrayList();

    public HomeController() {
        Platform.runLater(this::init);
    }

    private void init() {
        sessionListView.setCellFactory(sessionCellFactory);
        sessionListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        discoverListView.setCellFactory(onlineUserCellFactory);
        sessionListView.setItems(chatSessionInfos);
        discoverListView.setItems(onlineUsers);
        chatSessionInfos.addListener((ListChangeListener<ChatSessionInfo>) change -> updateSessionTab());
        onlineUsers.addListener((ListChangeListener<? super OnlineUser>) change -> updateDiscoverTab());
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/chat.fxml"));
        try {
            Node node = loader.load();
            chatWrapper.getChildren().add(0, node);
            chatController = loader.getController();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        updateSessionTab();
        updateDiscoverTab();
        User user = ChatManagerImpl.getInstance().getMyself();
        name.setText(user.getName());
        avatar.setImage(ImageUtils.loadImage(user.getAvatar()));
        ChatManagerImpl.getInstance().setChatListener(this);
        initSessionAndOnlineUserList();
        topBtnWrapper.managedProperty().bind(topBtnWrapper.visibleProperty());
        if (Utils.isWindows()) {
            setupOnWindows();
            topBtnWrapper.setVisible(true);
        } else {
            rootWrapper.setPadding(Insets.EMPTY);
            rootWrapper.setEffect(null);
            topBtnWrapper.setVisible(false);
        }
    }

    private void setupOnWindows() {
        Rectangle clipShape = new Rectangle();
        clipShape.setArcWidth(20);
        clipShape.setArcHeight(20);
        clipShape.setWidth(mainWrapper.getWidth());
        clipShape.setHeight(mainWrapper.getHeight());
        mainWrapper.widthProperty().addListener((observable, oldValue, newValue) -> {
            clipShape.setWidth(newValue.doubleValue());
        });
        mainWrapper.heightProperty().addListener((observable, oldValue, newValue) -> {
            clipShape.setHeight(newValue.doubleValue());
        });
        mainWrapper.setClip(clipShape);
        mainWrapper.setOnMousePressed(this::onWindowMousePressed);
        mainWrapper.setOnMouseDragged(this::onWindowMouseDragged);
        getStage().maximizedProperty().addListener((observable, oldValue, newValue) -> {
            String name = newValue ? "maximize-2.png" : "maximize.png";
            String url = Objects.requireNonNull(getClass().getResource("/images/" + name)).toExternalForm();
            maximizeImage.setImage(new Image(url));
            if (newValue) {
                rootWrapper.setPadding(Insets.EMPTY);
            } else {
                rootWrapper.setPadding(new Insets(20));
            }
        });
        rootWrapper.setPadding(new Insets(20));
    }

    private void initSessionAndOnlineUserList() {
        ChatManagerImpl chatManager = ChatManagerImpl.getInstance();
        for (ChatSession session : chatManager.getSessionManager().getChatSessionList()) {
            onAddChatSession(session);
        }
        for (OnlineUser onlineUser : chatManager.getOnlineUserManager().getOnlineUsers()) {
            onAddOnlineUser(onlineUser);
        }
    }

    private boolean isUpdatingSessionTab = false;
    private void updateSessionTab() {
        if (isUpdatingSessionTab) return;
        isUpdatingSessionTab = true;
        Utils.postDelayed(100, () -> {
            int size = chatSessionInfos.size();
            String title = String.format("会话(%d)", size);
            tabItemSession.setText(title);
            if (size == 0) {
                chatController.setUser(null);
            }
            else if (chatController.getUser() == null) {
                chatController.setUser(chatSessionInfos.get(0).getUser());
                selectSession(0);
            } else {
                int index = Utils.findIndexByKey(chatSessionInfos, chatController.getUser().getId(), it -> it.getUser().getId());
                if (index != -1) {
                    selectSession(index);
                }
            }
            isUpdatingSessionTab = false;
        });
    }

    private void selectSession(int index) {
        if (index < 0 || index > chatSessionInfos.size())
            return;
        int old = Utils.findIndexByKey(chatSessionInfos, true, ChatSessionInfo::isSelectedProperty);
        if (old != -1 && old != index)
            chatSessionInfos.get(old).selectedPropertyProperty().setValue(false);
        chatSessionInfos.get(index).selectedPropertyProperty().setValue(true);
    }

    private void updateDiscoverTab() {
        int size = onlineUsers.size();
        String title = String.format("发现(%d)", size);
        tabItemDiscover.setText(title);
    }

    public void onClickSessionTab() {
        sessionListView.setVisible(true);
        discoverListView.setVisible(false);
        tabItemSession.getStyleClass().add("active");
        tabItemDiscover.getStyleClass().remove("active");
    }

    public void onClickDiscoverTab() {
        sessionListView.setVisible(false);
        discoverListView.setVisible(true);
        tabItemSession.getStyleClass().remove("active");
        tabItemDiscover.getStyleClass().add("active");
    }

    private Stage getStage() {
        return (Stage) chatWrapper.getScene().getWindow();
    }

    public void onClickClose() {
        getStage().close();
    }

    public void closeSession(ChatSessionInfo info) {
        ChatSession session = ChatManagerImpl.getInstance().getSessionManager()
                .findChatSessionByUserId(info.getUser().getId());
        if (session != null)
            ChatManagerImpl.getInstance().notifySessionClosed(session);
    }

    public void onClickMinimize() {
        getStage().setIconified(true);
    }

    public void onClickMaximize() {
        getStage().setMaximized(!getStage().isMaximized());
    }

    private double offsetX, offsetY;
    private boolean isValidDown = false;
    private void onWindowMousePressed(MouseEvent event) {
        isValidDown = event.getSceneY() < 50 && event.getSceneY() > 10;
        if (!isValidDown) return;
        Stage stage = getStage();
        offsetX = stage.getX() - event.getScreenX();
        offsetY = stage.getY() - event.getScreenY();
    }

    private void onWindowMouseDragged(MouseEvent event) {
        if (!isValidDown) return;
        Stage stage = getStage();
        stage.setX(offsetX + event.getScreenX());
        stage.setY(offsetY + event.getScreenY());
    }

    public void chatWith(User user) {
        int index = Utils.findIndexByKey(chatSessionInfos, user.getId(), it -> it.getUser().getId());
        if (index != -1) {
            chatController.setUser(user);
            selectSession(index);
        }
    }

    private final Callback<ListView<ChatSessionInfo>, ListCell<ChatSessionInfo>> sessionCellFactory = param -> new ListCell<>() {
        private final Region node;
        private final ItemChatSessionController controller;

        {
            try {
                FXMLLoader loader = new FXMLLoader(HomeController.class.getResource("/item_chat_session.fxml"));
                node = loader.load();
                controller = loader.getController();
                controller.setHomeController(HomeController.this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void updateSelected(boolean selected) {
//            super.updateSelected(selected);
        }

        @Override
        protected void updateItem(ChatSessionInfo item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                node.backgroundProperty().bind(
                        Bindings.when(item.selectedPropertyProperty())
                                .then(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)))
                                .otherwise(Background.EMPTY)
                );
                setGraphic(node);
                controller.setItem(item);
            }
        }
    };

    private final Callback<ListView<OnlineUser>, ListCell<OnlineUser>> onlineUserCellFactory = param -> new ListCell<>() {
        private Node node;
        private ItemOnlineUserController controller;

        @Override
        public void updateSelected(boolean selected) {
//            super.updateSelected(selected);
        }

        @Override
        protected void updateItem(OnlineUser item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                if (node == null) {
                    try {
                        FXMLLoader loader = new FXMLLoader(HomeController.class.getResource("/item_online_user.fxml"));
                        node = loader.load();
                        controller = loader.getController();
                        controller.setHomeController(HomeController.this);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                setGraphic(node);
                controller.setItem(item);
            }
        }
    };


    @Override
    public void onAddOnlineUser(OnlineUser onlineUser) {
        Platform.runLater(() -> {
            int index = Utils.findIndexByKey(chatSessionInfos, onlineUser.getUser().getId(), it -> it.getUser().getId());
            if (index == -1) {
                // 仅添加未连接的用户
                onlineUsers.add(0, onlineUser);
            }
        });
    }

    @Override
    public void onRemoveOnlineUser(OnlineUser onlineUser) {
        Platform.runLater(() -> {
            int index = Utils.findIndexByKey(onlineUsers, onlineUser.getUser().getId(), it -> it.getUser().getId());
            if (index != -1) {
                onlineUsers.remove(index);
            }
        });
    }

    @Override
    public void onAddChatSession(ChatSession chatSession) {
        Platform.runLater(() -> {
            int index = Utils.findIndexByKey(onlineUsers, chatSession.getContextUserId(), it -> it.getUser().getId());
            if (index != -1) {
                // 从在线用户中删除
                onlineUsers.remove(index);
            }
            ChatMessageStore store = ChatManagerImpl.getInstance().getChatMessageStore();
            ChatMessage message = store.getLastMessage(chatSession.getContextUserId());
            if (message != null) {
                ChatManagerImpl.getInstance().getFileManager().updateChatMessageFileTask(message);
            }
            ChatSessionInfo info = new ChatSessionInfo(chatSession.getUser(), message == null ? null : message.copy());
            int oldIndex = Utils.findIndexByKey(chatSessionInfos, chatSession.getContextUserId(), it -> it.getUser().getId());
            if (oldIndex != -1) {
                chatSessionInfos.set(oldIndex, info);
            } else {
                chatSessionInfos.add(info);
            }
            ChatManagerImpl.getInstance().getChatClient().subscribe(chatSession.getContextUserId(), this);
            ChatManagerImpl.getInstance().getFileManager().subscribe(chatSession.getContextUserId(), this);
        });
    }

    @Override
    public void onRemoveChatSession(ChatSession chatSession) {
        Platform.runLater(() -> {
            OnlineUserManager mgr = ChatManagerImpl.getInstance().getOnlineUserManager();
            OnlineUser user = mgr.findOnlineUserById(chatSession.getContextUserId());
            if (user != null) {
                // 移除会话时把在线用户放回在线用户列表中
                onAddOnlineUser(user);
            }
            int index = Utils.findIndexByKey(chatSessionInfos, chatSession.getContextUserId(), it -> it.getUser().getId());
            if (index != -1) {
                if (chatController.getUser() != null && chatController.getUser().getId().equals(chatSession.getContextUserId())) {
                    chatController.setUser(null);
                }
                chatSessionInfos.remove(index);
                ChatManagerImpl.getInstance().getChatClient().unsubscribe(chatSession.getContextUserId(), this);
                ChatManagerImpl.getInstance().getFileManager().unsubscribe(chatSession.getContextUserId(), this);
            }
        });
    }

    @Override
    public void onNewMessage(UUID userId, ChatMessage message) {
        Platform.runLater(() -> {
            int index = Utils.findIndexByKey(chatSessionInfos, userId, it -> it.getUser().getId());
            if (index != -1) {
                ChatSessionInfo info = chatSessionInfos.get(index);
                ChatMessage chatMessage = message.copy();
                ChatManagerImpl.getInstance().getFileManager().updateChatMessageFileTask(chatMessage);
                info.setLastMessage(chatMessage);
                if (index == 0)
                    chatSessionInfos.set(0, info);
                else {
                    chatSessionInfos.remove(index);
                    chatSessionInfos.add(0, info);
                }
            }
        });
    }

    @Override
    public void onMessageUpdate(UUID userId, ChatMessage message) {

    }

    @Override
    public void onFileTaskUpdate(FileTask task) {
        Platform.runLater(() -> {
            int index = Utils.findIndexByKey(chatSessionInfos, task.getKey().getUserId(), it -> it.getUser().getId());
            if (index != -1) {
                ChatSessionInfo info = chatSessionInfos.get(index);
                ChatMessage message = info.getLastMessage();
                if (message == null) return;
                if (message.getFile() == null) return;
                if (!message.getFile().getResId().equals(task.getKey().getResId()))
                    return;
                info.setLastMessage(message.updateFileTask(task));
                chatSessionInfos.set(index, info);
            }
        });
    }
}
