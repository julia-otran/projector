package us.guihouse.projector.projection.video

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import us.guihouse.projector.projection.Paintable
import us.guihouse.projector.projection.glfw.GLFWGraphicsAdapter
import us.guihouse.projector.projection.glfw.GLFWGraphicsAdapterProvider
import us.guihouse.projector.projection.glfw.RGBImageCopy
import us.guihouse.projector.projection.models.VirtualScreen
import java.awt.Rectangle

class ProjectionVideoPaintable : Paintable {
    private var data = HashMap<String, IntArray>()
    private val positions = HashMap<String, Rectangle>()
    private val videoSizes = HashMap<String, Rectangle>()
    private val texes = HashMap<String, Int>()

    fun generateTex(provider: GLFWGraphicsAdapterProvider, vs: VirtualScreen, position: Rectangle, videoSize: Rectangle) {
        positions[vs.virtualScreenId] = position
        videoSizes[vs.virtualScreenId] = videoSize

        val oldTex = texes[vs.virtualScreenId]

        if (oldTex != null) {
            provider.freeTex(oldTex)
        }

        val tex = provider.dequeueTex()
        texes[vs.virtualScreenId] = tex

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex)

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, videoSize.getWidth().toInt(), videoSize.getHeight().toInt(), 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, 0L)

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
    }

    fun setProjectionData(data: IntArray?, vs: VirtualScreen) {
        this.data[vs.virtualScreenId] = data ?: IntArray(0)
    }

    override fun paintComponent(g: GLFWGraphicsAdapter, vs: VirtualScreen) {
        val data = this.data[vs.virtualScreenId] ?: IntArray(0)

        val position = positions[vs.virtualScreenId]

        val videoW = videoSizes[vs.virtualScreenId]?.getWidth()?.toInt() ?: 0
        val videoH = videoSizes[vs.virtualScreenId]?.getHeight()?.toInt() ?: 0

        val videoTex = texes[vs.virtualScreenId]

        if (position != null && videoTex != null) {
            if (videoW * videoH == 0) {
                return
            }

            if (videoW * videoH != data.size) {
                return
            }

            val buffer = g.provider.dequeueGlBuffer()

            GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, buffer)
            GL30.glBufferData(
                    GL30.GL_PIXEL_UNPACK_BUFFER,
                    data.size.toLong() * 3,
                    GL30.GL_STREAM_DRAW
            )

            val destination = GL30.glMapBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, GL30.GL_WRITE_ONLY)

            if (destination != null) {
                RGBImageCopy.copyImageToBuffer(data, destination, false)
            }

            GL30.glUnmapBuffer(GL30.GL_PIXEL_UNPACK_BUFFER)
            GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, 0)

            val alpha = g.alpha;

            g.provider.enqueueForDraw {
                GL11.glEnable(GL11.GL_BLEND)
                GL20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
                GL11.glEnable(GL11.GL_TEXTURE_2D)

                GL11.glPushMatrix()

                g.adjustOrtho()
                g.updateAlpha(alpha)

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, videoTex)

                GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, buffer)
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, videoW, videoH, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, 0L)

                GL11.glBegin(GL11.GL_QUADS)

                GL11.glTexCoord2d(0.0, 0.0)
                GL11.glVertex2d(position.getX(), position.getY())

                GL11.glTexCoord2d(0.0, 1.0)
                GL11.glVertex2d(position.getX(), position.maxY)

                GL11.glTexCoord2d(1.0, 1.0)
                GL11.glVertex2d(position.maxX, position.maxY)

                GL11.glTexCoord2d(1.0, 0.0)
                GL11.glVertex2d(position.maxX, position.getY())

                GL11.glEnd()

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
                GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, 0)

                GL11.glPopMatrix()

                GL11.glDisable(GL11.GL_BLEND)
                GL11.glDisable(GL11.GL_TEXTURE_2D)
            }
        }
    }

    fun freeTex(vs: VirtualScreen, provider: GLFWGraphicsAdapterProvider) {
        texes[vs.virtualScreenId]?.let {
            provider.freeTex(it)
        }
    }
}