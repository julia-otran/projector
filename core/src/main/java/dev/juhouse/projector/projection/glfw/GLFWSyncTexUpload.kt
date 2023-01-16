package dev.juhouse.projector.projection.glfw

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue

class GLFWSyncTexUpload(private val bounds: Rectangle) : GLFWTexUpload {
    companion object {
        const val NUM_BUFFERS = 3
    }

    private val filledImages = ConcurrentLinkedQueue<ByteBuffer>()
    private val freeImages = ConcurrentLinkedQueue<ByteBuffer>()

    override fun enqueue(img: BufferedImage) {
        freeImages.poll()?.let {
            RGBImageCopy.copyImageToBuffer(img, it)
            filledImages.add(it)
        }
    }

    override fun updateTex(texId: Int) {
        filledImages.poll()?.let {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId)
            GL11.glTexSubImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                0,
                0,
                bounds.width,
                bounds.height,
                GL11.GL_RGB,
                GL11.GL_UNSIGNED_BYTE,
                it
            )
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
            freeImages.add(it)
        }
    }

    override fun start() {
        for (i in 0 until NUM_BUFFERS) {
            freeImages.add(BufferUtils.createByteBuffer(bounds.width * bounds.height * 3))
        }
    }

    override fun stop() {
        freeImages.clear()
        filledImages.clear()
    }
}
