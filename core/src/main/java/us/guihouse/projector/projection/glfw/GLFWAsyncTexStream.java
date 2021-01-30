package us.guihouse.projector.projection.glfw;

import lombok.Getter;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import us.guihouse.projector.other.EventQueue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static us.guihouse.projector.projection.glfw.RGBImageCopy.copyImageToBuffer;

public class GLFWAsyncTexStream extends EventQueue {
    private static final int NUM_BUFFERS = 3;

    static public class Buffer {
        @Getter
        final int glBuffer;

        final BufferedImage image;

        Buffer(Integer glBuffer, BufferedImage image) {
            this.glBuffer = glBuffer;
            this.image = image;
        }
    }

    private final Rectangle bounds;

    private final List<Buffer> allocatedBuffers = new ArrayList<>();
    private final Queue<Buffer> freeBuffers = new ConcurrentLinkedQueue<>();
    private final Queue<Buffer> filledBuffers = new ConcurrentLinkedQueue<>();

    private final long window;

    public GLFWAsyncTexStream(Rectangle bounds, long parentWindow) {
        this.bounds = bounds;

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        window = glfwCreateWindow(640, 480, "Projector Background Window", NULL, parentWindow);

        if ( window == NULL )
            throw new RuntimeException("Failed to create the background GLFW window");
    }

    @Override
    public void onStart() {
        super.onStart();
        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        for(int i = 0; i < NUM_BUFFERS; i++) {
            int glBuffer = GL20.glGenBuffers();

            BufferedImage image = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);
            Buffer buffer = new Buffer(glBuffer, image);

            freeBuffers.add(buffer);
            allocatedBuffers.add(buffer);

            GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, glBuffer);
            GL30.glBufferData(GL30.GL_PIXEL_UNPACK_BUFFER, (long) bounds.width * bounds.height * 3, GL30.GL_STREAM_DRAW);
        }

        GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, 0);
    }

    @Override
    public void onStop() {
        super.onStop();

        allocatedBuffers.forEach(b -> {
            GL30.glDeleteBuffers(b.glBuffer);
        });

        allocatedBuffers.clear();
        freeBuffers.clear();
        filledBuffers.clear();

        glfwDestroyWindow(window);
    }

    public void upload(BufferedImage src) {
        Buffer buffer = freeBuffers.poll();
        if (buffer != null) {
            src.copyData(buffer.image.getRaster());

            enqueueForRun(() -> {
                GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, buffer.glBuffer);
                ByteBuffer destination = GL30.glMapBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, GL30.GL_WRITE_ONLY);
                if (destination != null) {
                    copyImageToBuffer(buffer.image, destination);
                }
                GL30.glUnmapBuffer(GL30.GL_PIXEL_UNPACK_BUFFER);
                filledBuffers.add(buffer);
            });
        }
    }

    public Buffer pollBuffer() {
        return filledBuffers.poll();
    }

    public void freeBuffer(Buffer buffer) {
        freeBuffers.add(buffer);
    }
}
