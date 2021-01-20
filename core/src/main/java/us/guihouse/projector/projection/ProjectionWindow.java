/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.Getter;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.system.MemoryStack;
import us.guihouse.projector.other.GraphicsFinder;
import us.guihouse.projector.other.RuntimeProperties;
import us.guihouse.projector.projection.glfw.GLFWHelper;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 *
 * @author guilherme
 */
public class ProjectionWindow  {
    @Getter
    private final GraphicsFinder.Device currentDevice;

    private long window;

    private ByteBuffer buffer;

    private Rectangle bounds;

    private boolean drawing = false;

    private BufferedImage temp;

    ProjectionWindow(GraphicsFinder.Device device) {
        this.currentDevice = device;

        bounds = getCurrentDevice().getDevice().getDefaultConfiguration().getBounds();
        buffer = BufferUtils.createByteBuffer(bounds.width * bounds.height * 4);
        temp = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);
    }

    void init() {
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
        window = glfwCreateWindow(bounds.width, bounds.height, "Projetor", monitor, NULL);

        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);

        // Enable v-sync
        glfwSwapInterval(1);

        GL.createCapabilities();

        glfwSetWindowMonitor(window, monitor, bounds.x, bounds.y, bounds.width, bounds.height, refreshRate);
    }

    private static byte toByte(int num) {
        if (num >= 255) {
            num = num & 0xFF;
        }

        if (num <= Byte.MAX_VALUE && num >= 0) {
            return (byte) num;
        }

        num = num - 256;

        if (num >= Byte.MIN_VALUE && num <= Byte.MAX_VALUE) {
            return (byte) num;
        }

        return 0;
    }

    private static void copyPixel(int argb, ByteBuffer buffer) {
        byte num = toByte((argb >> 16) & 0xFF);
        buffer.put(num);
        num = toByte((argb >> 8) & 0xFF);
        buffer.put(num);
        num = toByte(argb & 0xFF);
        buffer.put(num);
        num = toByte((argb >> 24) & 0xFF);
        buffer.put(num);
    }

    private long time = 0;
    private long frames = 0;
    void updateOutput(BufferedImage src) {
        if (drawing) {
            return;
        }

        drawing = true;

        src.copyData(temp.getRaster());

        GLFWHelper.invokeLater(() -> {
            if (RuntimeProperties.isLogFPS()) {
                long current = System.nanoTime();
                frames++;

                if (current - time > 1000000000) {
                    System.out.println("GL Frames " + frames);
                    time = current;
                    frames = 0;
                }
            }

            buffer.clear();

            int[] pixelsSrc = ((DataBufferInt)temp.getRaster().getDataBuffer()).getData();

            for (int y = bounds.height - 1; y >= 0; y--) {
                for (int x = 0; x < bounds.width; x++) {
                    int i = y * bounds.width + x;
                    int argb = pixelsSrc[i];
                    copyPixel(argb, buffer);
                }
            }

            buffer.flip();

            glfwMakeContextCurrent(window);
            GL11.glDrawPixels(bounds.width, bounds.height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
            drawing = false;

            glfwSwapBuffers(window);
            glfwPollEvents();
        });
    }

    void makeVisible() {
        if (window != 0) {
            glfwShowWindow(window);
        }
    }

    void shutdown() {
        // Free the window callbacks and destroy the window
        if (window != 0) {
            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
        }
    }

    public void setFullScreen(boolean fullScreen) {
    }
}
