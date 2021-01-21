package us.guihouse.projector.projection.glfw;

import org.lwjgl.glfw.GLFWErrorCallback;
import us.guihouse.projector.other.EventQueue;

import static org.lwjgl.glfw.GLFW.*;

public class GLFWHelper extends EventQueue {
    private static final GLFWHelper instance = new GLFWHelper();

    public static void initGLFW() {
        instance.init();
    }

    public static void finish() {
        instance.stop();
    }

    public static void invokeLater(Runnable r) {
        instance.enqueueForRun(r);
    }

    @Override
    public void onStart() {
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");
    }

    @Override
    public void onStop() {
        glfwTerminate();

        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) {
            cb.free();
        }
    }
}
