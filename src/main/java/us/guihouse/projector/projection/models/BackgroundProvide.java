package us.guihouse.projector.projection.models;

import javafx.scene.image.Image;

import java.awt.image.BufferedImage;

public interface BackgroundProvide {
    enum Type {
        NONE, STATIC, OVERLAY_ANIMATED
    }

    BufferedImage getBackground();
    BufferedImage getLogo();
    BufferedImage getOverlay();
    BufferedImage getStaticBackground();
    Type getType();
}
