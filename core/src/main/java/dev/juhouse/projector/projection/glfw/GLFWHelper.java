package dev.juhouse.projector.projection.glfw;

import org.lwjgl.glfw.GLFWErrorCallback;
import dev.juhouse.projector.other.EventQueue;

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

    private final Runnable pollEvents = org.lwjgl.glfw.GLFW::glfwPollEvents;

    @Override
    public void onStart() {
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        enqueueContinuous(pollEvents);
    }

    @Override
    public void onStop() {
        removeContinuous(pollEvents);

        glfwTerminate();

        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) {
            cb.free();
        }
    }
}
