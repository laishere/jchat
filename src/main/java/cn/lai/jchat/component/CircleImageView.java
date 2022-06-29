package cn.lai.jchat.component;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

public class CircleImageView extends ImageView {
    public CircleImageView() {
        imageProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.getProgress() == 1.0) {
                double size = Math.min(newValue.getWidth(), newValue.getHeight());
                double left = (newValue.getWidth() - size) / 2;
                double top = (newValue.getHeight() - size) / 2;
                Rectangle2D rect = new Rectangle2D(left, top, size, size);
                setViewport(rect);
            }
        });
        Circle clipShape = new Circle();
        layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            clipShape.setCenterX(newValue.getCenterX());
            clipShape.setCenterY(newValue.getCenterY());
            clipShape.setRadius(Math.min(newValue.getWidth(), newValue.getHeight()) / 2);
        });
        setClip(clipShape);
        setSize(50);
    }

    private DoubleProperty sizeProperty;

    public final DoubleProperty sizeProperty() {
        if (sizeProperty == null) {
            sizeProperty = new SimpleDoubleProperty() {
                @Override
                protected void invalidated() {
                    double size = sizeProperty.getValue();
                    setFitWidth(size);
                    setFitHeight(size);
                }
            };
        }
        return sizeProperty;
    }

    public final void setSize(double size) {
        sizeProperty().setValue(size);
    }

    public final double getSize() {
        return sizeProperty().getValue();
    }
}
