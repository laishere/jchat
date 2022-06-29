package cn.lai.jchat;

import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

//created by Alexander Berg
public class ResizeHelper {

    public static void addResizeListener(Stage stage) {
        ResizeListener resizeListener = new ResizeListener(stage);
        stage.getScene().addEventHandler(MouseEvent.ANY, resizeListener);
    }

    static class ResizeListener implements EventHandler<MouseEvent> {
        private final Stage stage;
        private Cursor cursorEvent = Cursor.DEFAULT;
        private double startX = 0;
        private double startY = 0;
        private double downX = 0;
        private double downY = 0;
        private double downW = 0;
        private double downH = 0;

        private double getBorderSize() {
            Region parent = (Region) stage.getScene().getRoot();
            return parent.getPadding().getTop() + 4;
        }

        public ResizeListener(Stage stage) {
            this.stage = stage;
        }

        @Override
        public void handle(MouseEvent mouseEvent) {
            EventType<? extends MouseEvent> mouseEventType = mouseEvent.getEventType();
            Scene scene = stage.getScene();

            double mouseEventX = mouseEvent.getSceneX(),
                    mouseEventY = mouseEvent.getSceneY(),
                    sceneWidth = scene.getWidth(),
                    sceneHeight = scene.getHeight();

            if (MouseEvent.MOUSE_MOVED.equals(mouseEventType)) {
                if (mouseEventX < getBorderSize() && mouseEventY < getBorderSize()) {
                    cursorEvent = Cursor.NW_RESIZE;
                } else if (mouseEventX < getBorderSize() && mouseEventY > sceneHeight - getBorderSize()) {
                    cursorEvent = Cursor.SW_RESIZE;
                } else if (mouseEventX > sceneWidth - getBorderSize() && mouseEventY < getBorderSize()) {
                    cursorEvent = Cursor.NE_RESIZE;
                } else if (mouseEventX > sceneWidth - getBorderSize() && mouseEventY > sceneHeight - getBorderSize()) {
                    cursorEvent = Cursor.SE_RESIZE;
                } else if (mouseEventX < getBorderSize()) {
                    cursorEvent = Cursor.W_RESIZE;
                } else if (mouseEventX > sceneWidth - getBorderSize()) {
                    cursorEvent = Cursor.E_RESIZE;
                } else if (mouseEventY < getBorderSize()) {
                    cursorEvent = Cursor.N_RESIZE;
                } else if (mouseEventY > sceneHeight - getBorderSize()) {
                    cursorEvent = Cursor.S_RESIZE;
                } else {
                    cursorEvent = Cursor.DEFAULT;
                }
                scene.setCursor(cursorEvent);
            } else if (MouseEvent.MOUSE_EXITED.equals(mouseEventType) || MouseEvent.MOUSE_EXITED_TARGET.equals(mouseEventType)) {
                scene.setCursor(Cursor.DEFAULT);
            } else if (MouseEvent.MOUSE_PRESSED.equals(mouseEventType)) {
                startX = stage.getWidth() - mouseEventX;
                startY = stage.getHeight() - mouseEventY;
                downX = mouseEvent.getScreenX();
                downY = mouseEvent.getScreenY();
                downW = stage.getWidth();
                downH = stage.getHeight();
            } else if (MouseEvent.MOUSE_DRAGGED.equals(mouseEventType)) {
                if (!Cursor.DEFAULT.equals(cursorEvent)) {
                    if (!Cursor.W_RESIZE.equals(cursorEvent) && !Cursor.E_RESIZE.equals(cursorEvent)) {
                        double minHeight = Math.max(stage.getMinHeight(), (getBorderSize() * 2));
                        if (Cursor.NW_RESIZE.equals(cursorEvent) || Cursor.N_RESIZE.equals(cursorEvent) || Cursor.NE_RESIZE.equals(cursorEvent)) {
                            if (stage.getHeight() > minHeight || mouseEventY < 0) {
                                double deltaY = downY -  mouseEvent.getScreenY();
                                double originBottom = stage.getY() + stage.getHeight();
                                stage.setHeight(deltaY + downH);
                                stage.setY(originBottom - stage.getHeight());
                            }
                        } else {
                            if (stage.getHeight() > minHeight || mouseEventY + startY - stage.getHeight() > 0) {
                                stage.setHeight(mouseEventY + startY);
                            }
                        }
                    }

                    if (!Cursor.N_RESIZE.equals(cursorEvent) && !Cursor.S_RESIZE.equals(cursorEvent)) {
                        double minWidth = Math.max(stage.getMinWidth(), (getBorderSize() * 2));
                        if (Cursor.NW_RESIZE.equals(cursorEvent) || Cursor.W_RESIZE.equals(cursorEvent) || Cursor.SW_RESIZE.equals(cursorEvent)) {
                            if (stage.getWidth() > minWidth || mouseEventX < 0) {
                                double deltaX = downX -  mouseEvent.getScreenX();
                                double originRight = stage.getX() + stage.getWidth();
                                stage.setWidth(deltaX + downW);
                                stage.setX(originRight - stage.getWidth());
                            }
                        } else {
                            if (stage.getWidth() > minWidth || mouseEventX + startX - stage.getWidth() > 0) {
                                stage.setWidth(mouseEventX + startX);
                            }
                        }
                    }
                }
            } else {
                scene.setCursor(Cursor.DEFAULT);
            }
        }
    }
}