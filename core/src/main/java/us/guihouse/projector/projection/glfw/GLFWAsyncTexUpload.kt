package us.guihouse.projector.projection.glfw

import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentLinkedQueue
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import us.guihouse.projector.projection.glfw.GLFWAsyncTexUpload
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import java.lang.Runnable
import us.guihouse.projector.projection.glfw.RGBImageCopy
import org.lwjgl.system.MemoryUtil
import us.guihouse.projector.other.EventQueue
import java.awt.Rectangle
import java.lang.RuntimeException
import java.util.ArrayList
import java.util.Queue
import java.util.function.Consumer

class GLFWAsyncTexUpload(private val bounds: Rectangle, private val window: Long) : EventQueue(), GLFWTexUpload {
    data class Buffer internal constructor(val glBuffer: Int, val image: BufferedImage)

    private val allocatedBuffers: MutableList<Buffer> = ArrayList()
    private val freeBuffers: Queue<Buffer> = ConcurrentLinkedQueue()
    private val filledBuffers: Queue<Buffer> = ConcurrentLinkedQueue()

    override fun onStart() {
        super.onStart()
        GLFW.glfwMakeContextCurrent(window)
        GL.createCapabilities()

        for (i in 0 until NUM_BUFFERS) {
            val glBuffer = GL20.glGenBuffers()
            val image = BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB)
            val buffer = Buffer(glBuffer, image)
            freeBuffers.add(buffer)
            allocatedBuffers.add(buffer)
            GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, glBuffer)
            GL30.glBufferData(
                GL30.GL_PIXEL_UNPACK_BUFFER,
                bounds.width.toLong() * bounds.height * 3,
                GL30.GL_STREAM_DRAW
            )
        }
        GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, 0)
    }

    override fun onStop() {
        super.onStop()
        allocatedBuffers.forEach(Consumer { b: Buffer -> GL30.glDeleteBuffers(b.glBuffer) })
        allocatedBuffers.clear()
        freeBuffers.clear()
        filledBuffers.clear()
        GLFW.glfwDestroyWindow(window)
    }

    override fun enqueue(img: BufferedImage) {
        val buffer = freeBuffers.poll()
        if (buffer != null) {
            img.copyData(buffer.image.raster)
            enqueueForRun {
                GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, buffer.glBuffer)
                val destination = GL30.glMapBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, GL30.GL_WRITE_ONLY)
                if (destination != null) {
                    RGBImageCopy.copyImageToBuffer(buffer.image, destination)
                }
                GL30.glUnmapBuffer(GL30.GL_PIXEL_UNPACK_BUFFER)
                GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, 0)
                filledBuffers.add(buffer)
            }
        }
    }

    companion object {
        private const val NUM_BUFFERS = 3
    }

    override fun updateTex(texId: Int) {
        filledBuffers.poll()?.let { buffer ->
            GL20.glBindTexture(GL20.GL_TEXTURE_2D, texId)
            GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, buffer.glBuffer)
            GL11.glTexSubImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                0,
                0,
                bounds.width,
                bounds.height,
                GL11.GL_RGB,
                GL11.GL_UNSIGNED_BYTE,
                0
            )
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
            GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, 0)
            freeBuffers.add(buffer)
        }
    }

    override fun start() {
        super.init()
    }
}