package dev.juhouse.projector.projection.glfw;

import dev.juhouse.projector.other.GraphicsFinder;

public class GLFWWindowBuilder {
    public static GLFWWindow createWindow(GraphicsFinder.Device device) {
        return new GLFWWindow(device);
    }
}
