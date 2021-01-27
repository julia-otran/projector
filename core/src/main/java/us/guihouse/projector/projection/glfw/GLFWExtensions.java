package us.guihouse.projector.projection.glfw;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GLCapabilities;

public class GLFWExtensions {
    public static boolean isPboSupported() {
        return GLFW.glfwExtensionSupported("GL_ARB_pixel_buffer_object");
    }
}
