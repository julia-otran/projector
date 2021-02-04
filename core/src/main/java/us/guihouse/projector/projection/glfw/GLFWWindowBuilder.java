package us.guihouse.projector.projection.glfw;

import us.guihouse.projector.other.GraphicsFinder;

public class GLFWWindowBuilder {
    public static GLFWWindow createWindow(GraphicsFinder.Device device) {
        return new GLFWWindow(device);
    }
}
