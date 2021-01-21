package us.guihouse.projector.projection.glfw;

import org.lwjgl.glfw.GLFW;

public class GLFWExtensions {
    public static boolean isPboSupported() {
        return GLFW.glfwExtensionSupported("GL_ARB_pixel_buffer_object");
    }
}
