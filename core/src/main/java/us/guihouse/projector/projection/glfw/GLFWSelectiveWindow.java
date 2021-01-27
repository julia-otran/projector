package us.guihouse.projector.projection.glfw;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import us.guihouse.projector.models.WindowConfig;
import us.guihouse.projector.other.GraphicsFinder;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class GLFWSelectiveWindow implements GLFWWindow {
    private final GraphicsFinder.Device device;

    private final Rectangle bounds;

    private GLFWInternalWindow delegate;
    private final GLFWBlackLevelAdjust blackLevelAdjust;

    private long window = 0;

    public GLFWSelectiveWindow(GraphicsFinder.Device device) {
        this.device = device;
        bounds = getCurrentDevice().getDevice().getDefaultConfiguration().getBounds();
        blackLevelAdjust = new GLFWBlackLevelAdjust();
    }

    @Override
    public void init(WindowConfig windowConfig) {
        PointerBuffer monitors = glfwGetMonitors();

        if (monitors == null) {
            throw new IllegalStateException("Unable to list monitors");
        }

        long monitor = -1;

        try ( MemoryStack stack = stackPush() ) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);

            for (int i = 0; i < monitors.limit(); i++) {
                glfwGetMonitorPos(monitors.get(i), x, y);

                if (x.get(0) == bounds.x && y.get(0) == bounds.y) {
                    monitor = monitors.get(i);
                }
            }
        }

        if (monitor == -1) {
            throw new IllegalStateException("Unable to find monitor");
        }

        GLFWVidMode vidMode = glfwGetVideoMode(monitor);
        int refreshRate = vidMode == null ? 30 : vidMode.refreshRate();

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE);
        glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_FALSE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        window = glfwCreateWindow(bounds.width, bounds.height, "Projetor", monitor, NULL);

        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);

        // Enable v-sync
        glfwSwapInterval(1);

        GL.createCapabilities();

        glfwSetWindowMonitor(window, monitor, bounds.x, bounds.y, bounds.width, bounds.height, refreshRate);

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        GL11.glViewport(0, 0, bounds.width, bounds.height);

        blackLevelAdjust.init(bounds, windowConfig.getBlackLevelAdjust());

        List<GLFWDrawer> drawers = new ArrayList<>();

        drawers.add(blackLevelAdjust);

        if (GLFWExtensions.isPboSupported()) {
            delegate = new GLFWPboWindow(bounds, window, drawers);
        } else {
            delegate = new GLFWTexWindow(bounds, window, drawers);
        }

        delegate.init();
    }

    @Override
    public void shutdown() {
        if (delegate != null) {
            delegate.shutdown();
        }

        if (blackLevelAdjust != null) {
            blackLevelAdjust.finish();
        }

        if (window != 0) {
            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
        }
    }

    @Override
    public void updateOutput(BufferedImage src) {
        if (delegate != null) {
            delegate.updateOutput(src);
        }
    }

    @Override
    public void makeVisible() {
        if (window != 0) {
            glfwShowWindow(window);
        }
    }

    @Override
    public GraphicsFinder.Device getCurrentDevice() {
        return device;
    }
}
