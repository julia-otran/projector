package us.guihouse.projector.projection.glfw;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.guihouse.projector.models.WindowConfig;
import us.guihouse.projector.other.RuntimeProperties;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static us.guihouse.projector.projection.glfw.RGBImageCopy.copyImageToBuffer;

public class GLFWTexWindow implements GLFWInternalWindow {
    private static final Logger LOGGER = LoggerFactory.getLogger(GLFWTexWindow.class);

    private final long window;

    private final ByteBuffer buffer;

    private final Rectangle bounds;
    private final List<GLFWDrawer> drawers;

    private boolean drawing = false;

    private final BufferedImage temp;

    private int textureId;

    GLFWTexWindow(Rectangle bounds, long window, List<GLFWDrawer> drawers) {
        this.window = window;
        this.bounds = bounds;
        this.drawers = drawers;

        buffer = BufferUtils.createByteBuffer(bounds.width * bounds.height * 3);
        temp = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);
    }

    public void init() {
        LOGGER.warn("Initializing Tex Window for compatibility");
        glfwMakeContextCurrent(window);

        textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        copyImageToBuffer(temp, buffer);

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, bounds.width, bounds.height, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

    }

    private long time = 0;
    private long frames = 0;
    public void updateOutput(BufferedImage src) {
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

            copyImageToBuffer(temp, buffer);

            glfwMakeContextCurrent(window);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, bounds.width, bounds.height, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);
            GL11.glBegin(GL11.GL_QUADS);

            GL11.glTexCoord2f(0, 0); GL11.glVertex2f(-1f, -1f);
            GL11.glTexCoord2f(0, 1); GL11.glVertex2f(-1f, 1f);
            GL11.glTexCoord2f(1, 1); GL11.glVertex2f(1f,1f);
            GL11.glTexCoord2f(1, 0); GL11.glVertex2f(1f, -1f);

            GL11.glEnd();

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            drawers.forEach(GLFWDrawer::draw);

            drawing = false;

            glfwSwapBuffers(window);
            glfwPollEvents();
        });
    }

    @Override
    public void updateWindowConfig(WindowConfig windowConfig) {

    }

    public void shutdown() {
    }
}
