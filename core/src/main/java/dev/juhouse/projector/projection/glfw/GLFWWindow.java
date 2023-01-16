package dev.juhouse.projector.projection.glfw;

import dev.juhouse.projector.projection.models.VirtualScreen;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import dev.juhouse.projector.models.WindowConfig;
import dev.juhouse.projector.other.GraphicsFinder;

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
    private GLFWVidMode vidMode;

    private final GLFWBlackLevelAdjust blackLevelAdjust;
    private final GLFWBlend blends;
    private final GLFWColorCorrection colorCorrection;
    private final GLFWHelperLines helperLines;

    private long window = 0;

    public GLFWWindow(GraphicsFinder.Device device) {
        this.device = device;
        bounds = getCurrentDevice().getDevice().getDefaultConfiguration().getBounds();
        blackLevelAdjust = new GLFWBlackLevelAdjust();
        blends = new GLFWBlend(bounds);
        colorCorrection = new GLFWColorCorrection(bounds);
        helperLines = new GLFWHelperLines(bounds);
    }

    public void createWindow(long glShare) {
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

        vidMode = glfwGetVideoMode(monitor);
        if (vidMode == null) {
            throw new IllegalStateException("Unable to find vidMode");
        }

        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE);
        glfwWindowHint(GLFW_SAMPLES, 4);
        glfwWindowHint(GLFW_CENTER_CURSOR, GLFW_FALSE);

        glfwWindowHint(GLFW_RED_BITS, vidMode.redBits());
        glfwWindowHint(GLFW_GREEN_BITS, vidMode.greenBits());
        glfwWindowHint(GLFW_BLUE_BITS, vidMode.blueBits());
        glfwWindowHint(GLFW_REFRESH_RATE, vidMode.refreshRate());

        window = glfwCreateWindow(vidMode.width(), vidMode.height(), "Projetor", monitor, glShare);

        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
        glfwSetWindowPos(window, bounds.x, bounds.y);
    }

    public void init(WindowConfig windowConfig, VirtualScreen virtualScreen) {
        // Make the OpenGL context current
        glfwMakeContextCurrent(window);

        // Enable v-sync
        glfwSwapInterval(1);

        GL.createCapabilities();

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        GL11.glViewport(0, 0, vidMode.width(), vidMode.height());

        blackLevelAdjust.init(vidMode);
        colorCorrection.init(windowConfig, virtualScreen);

        blackLevelAdjust.updateConfigs(windowConfig.getBlackLevelAdjust());
        colorCorrection.setWindowConfig(windowConfig);
        blends.updateWindowConfigs(windowConfig);
        helperLines.updateWindowConfig(windowConfig);
    }

    public void loopCycle(int texId) {
        glfwMakeContextCurrent(window);

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_COLOR_MATERIAL);
        GL20.glDisable(GL20.GL_MULTISAMPLE);

        GL30.glClearColor(0f, 0f, 0.3f, 1.0f);
        GL30.glClear(GL30.GL_COLOR_BUFFER_BIT);

        colorCorrection.loopCycle(texId);

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

    public GraphicsFinder.Device getCurrentDevice() {
        return device;
    }
}
