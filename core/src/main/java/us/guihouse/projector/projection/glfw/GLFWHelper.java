package us.guihouse.projector.projection.glfw;

import org.lwjgl.glfw.GLFWErrorCallback;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.glfw.GLFW.*;

public class GLFWHelper implements Runnable {
    private static final GLFWHelper instance = new GLFWHelper();

    public static void initGLFW() {
        instance.init();
    }

    public static void finish() {
        instance.stop();
    }

    public static void invokeLater(Runnable r) {
        instance.push(r);
    }

    private Thread thread;
    private boolean running;
    private final Queue<Runnable> eventQueue = new ConcurrentLinkedQueue<>();
    private final Object waiter = new Object();

    private void init() {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    private void stop() {
        if (!running) {
            return;
        }

        running = false;
        synchronized (waiter) {
            waiter.notify();
        }
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void push(Runnable r) {
        eventQueue.add(r);
        synchronized (waiter) {
            waiter.notify();
        }
    }

    @Override
    public void run() {
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE);

        Runnable r;

        while (running) {
            try {
                synchronized (waiter) {
                    waiter.wait(1000);
                }
                do {
                   r = eventQueue.poll();
                   if (r != null) {
                       r.run();
                   }
                } while (r != null);
            } catch (InterruptedException e) {
                e.printStackTrace();
                running = false;
            }
        }

        glfwTerminate();

        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) {
            cb.free();
        }
    }
}
