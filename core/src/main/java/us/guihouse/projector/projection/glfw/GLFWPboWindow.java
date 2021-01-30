package us.guihouse.projector.projection.glfw;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.guihouse.projector.models.WindowConfig;
import us.guihouse.projector.other.RuntimeProperties;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class GLFWPboWindow implements GLFWInternalWindow {
    private static final Logger LOGGER = LoggerFactory.getLogger(GLFWWindow.class);

    private final long window;
    private final List<GLFWDrawer> drawers;
    private GLFWAsyncTexStream texStream;

    private final Rectangle bounds;

    private int textureId;

    private GLFWColorCorrection colorCorrection;

    class TexUpdate implements Runnable {
        private long time = 0;
        private long frames = 0;

        @Override
        public void run() {
            GLFWAsyncTexStream.Buffer buffer = texStream.pollBuffer();

            if (buffer == null) {
                return;
            }

            if (RuntimeProperties.isLogFPS()) {
                long current = System.nanoTime();
                frames++;

                if (current - time > 1000000000) {
                    System.out.println("GL Frames " + frames);
                    time = current;
                    frames = 0;
                }
            }

            glfwMakeContextCurrent(window);

            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_COLOR_MATERIAL);
            GL20.glDisable(GL20.GL_MULTISAMPLE);

            GL30.glClearColor(0f, 0f, 0.0f, 1.0f);
            GL30.glClear(GL30.GL_COLOR_BUFFER_BIT);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, buffer.getGlBuffer());
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, bounds.width, bounds.height, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, 0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            GL20.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, 0);

            texStream.freeBuffer(buffer);

            colorCorrection.loopCycle(textureId);

            drawers.forEach(GLFWDrawer::draw);

            glfwSwapBuffers(window);

            glfwPollEvents();
        }
    }

    GLFWPboWindow(Rectangle bounds, long window, List<GLFWDrawer> drawers) {
        this.bounds = bounds;
        this.window = window;
        this.drawers = drawers;
    }

    public void init() {
        LOGGER.debug("Initializing PBO Based Window");
        texStream = new GLFWAsyncTexStream(bounds, window);
        texStream.init();

        glfwMakeContextCurrent(window);

        textureId = GL11.glGenTextures();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        final ByteBuffer buffer = BufferUtils.createByteBuffer(bounds.width * bounds.height * 3);
        buffer.flip();

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, bounds.width, bounds.height, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        colorCorrection = new GLFWColorCorrection();
        colorCorrection.init();

        GLFWHelper.invokeContinuous(new TexUpdate());
    }

    public void updateOutput(BufferedImage src) {
        if (texStream == null) {
            return;
        }

        texStream.upload(src);
    }

    @Override
    public void updateWindowConfig(WindowConfig windowConfig) {
        glfwMakeContextCurrent(window);
        colorCorrection.setWindowConfig(windowConfig);
    }

    public void shutdown() {
        if (texStream != null) {
            texStream.stop();
        }
        if (colorCorrection != null) {
            colorCorrection.shutdown();
        }
    }
}
