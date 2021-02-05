package us.guihouse.projector.projection.glfw;

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
import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class GLFWWindow {
    private final GraphicsFinder.Device device;

    private final Rectangle bounds;
    private double translateX = 0;
    private double translateY = 0;

    private final GLFWBlackLevelAdjust blackLevelAdjust;
    private final GLFWBlend blends;
    private final GLFWColorCorrection colorCorrection;
    private final GLFWHelperLines helperLines;

    private long window = 0;
    private long monitor;
    private int refreshRate;

    public GLFWWindow(GraphicsFinder.Device device) {
        this.device = device;
        bounds = getCurrentDevice().getDevice().getDefaultConfiguration().getBounds();
        blackLevelAdjust = new GLFWBlackLevelAdjust();
        blends = new GLFWBlend(bounds);
        colorCorrection = new GLFWColorCorrection();
        helperLines = new GLFWHelperLines(bounds);
    }

    public void createWindow(long glShare) {
        PointerBuffer monitors = glfwGetMonitors();

        if (monitors == null) {
            throw new IllegalStateException("Unable to list monitors");
        }

        monitor = -1;

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
        refreshRate = vidMode == null ? 30 : vidMode.refreshRate();

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE);
        glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_FALSE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        window = glfwCreateWindow(bounds.width, bounds.height, "Projetor", monitor, glShare);

        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");
    }

    public void init(WindowConfig windowConfig) {
        // Make the OpenGL context current
        glfwMakeContextCurrent(window);

        // Enable v-sync
        glfwSwapInterval(1);

        GL.createCapabilities();

        glfwSetWindowMonitor(window, monitor, bounds.x, bounds.y, bounds.width, bounds.height, refreshRate);

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        GL11.glViewport(0, 0, bounds.width, bounds.height);

        blackLevelAdjust.init(bounds);
        colorCorrection.init();
        blackLevelAdjust.updateConfigs(windowConfig.getBlackLevelAdjust());
        colorCorrection.setWindowConfig(windowConfig);
        blends.updateWindowConfigs(windowConfig);
        helperLines.updateWindowConfig(windowConfig);

        translateX = -1d * windowConfig.getX() / bounds.width;
        translateY = -1d * windowConfig.getY() / bounds.height;
    }

    public void loopCycle(int texId) {
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_COLOR_MATERIAL);
        GL20.glDisable(GL20.GL_MULTISAMPLE);

        glfwMakeContextCurrent(window);

        GL30.glClearColor(0f, 0f, 0.3f, 1.0f);
        GL30.glClear(GL30.GL_COLOR_BUFFER_BIT);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glTranslated(translateX, translateY, 0d);

        colorCorrection.loopCycle(texId);

        GL11.glPopMatrix();

        blends.render();
        blackLevelAdjust.draw();
        helperLines.render();

        glfwSwapBuffers(window);
        glfwPollEvents();
    }

    public void shutdown() {
        if (blends != null) {
            blends.shutdown();
        }

        if (colorCorrection != null) {
            colorCorrection.shutdown();
        }

        if (blackLevelAdjust != null) {
            blackLevelAdjust.finish();
        }

        if (window != 0) {
            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
        }
    }

    public void updateWindowConfig(WindowConfig wc) {
        if (window > 0) {
            glfwMakeContextCurrent(window);
        }
        if (colorCorrection != null) {
            colorCorrection.setWindowConfig(wc);
        }
        if (blackLevelAdjust != null) {
            blackLevelAdjust.updateConfigs(wc.getBlackLevelAdjust());
        }
        if (blends != null) {
            blends.updateWindowConfigs(wc);
        }
        if (helperLines != null) {
            helperLines.updateWindowConfig(wc);
        }
    }

    public void makeVisible() {
        if (window != 0) {
            glfwShowWindow(window);
        }
    }

    public GraphicsFinder.Device getCurrentDevice() {
        return device;
    }
}
