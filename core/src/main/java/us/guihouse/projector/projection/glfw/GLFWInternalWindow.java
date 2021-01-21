package us.guihouse.projector.projection.glfw;

import java.awt.image.BufferedImage;

public interface GLFWInternalWindow {
    void init();
    void shutdown();
    void updateOutput(BufferedImage src);
}
