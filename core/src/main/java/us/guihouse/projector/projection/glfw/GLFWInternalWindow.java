package us.guihouse.projector.projection.glfw;

import us.guihouse.projector.models.WindowConfig;

import java.awt.image.BufferedImage;

public interface GLFWInternalWindow {
    void init();
    void shutdown();
    void updateOutput(BufferedImage src);
    void updateWindowConfig(WindowConfig windowConfig);
}
