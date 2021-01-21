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
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import us.guihouse.projector.other.GraphicsFinder;
import us.guihouse.projector.other.RuntimeProperties;
import us.guihouse.projector.projection.glfw.GLFWAsyncTexStream;
import us.guihouse.projector.projection.glfw.GLFWHelper;
import us.guihouse.projector.projection.glfw.GLFWWindow;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 *
 * @author guilherme
 */
public interface ProjectionWindow {
    void init();
    void shutdown();
    void updateOutput(BufferedImage src);
    void makeVisible();
    GraphicsFinder.Device getCurrentDevice();
}
