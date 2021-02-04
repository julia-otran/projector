package us.guihouse.projector.projection.glfw

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import us.guihouse.projector.models.WindowConfig
import us.guihouse.projector.models.WindowConfigBlend
import us.guihouse.projector.utils.BlendGenerator
import java.awt.Rectangle

class GLFWBlend(private val bounds: Rectangle) {
    private val blendsTex = HashMap<Int, WindowConfigBlend>()

    fun render() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPushMatrix()
        GL11.glOrtho(0.0, bounds.width.toDouble(), bounds.height.toDouble(), 0.0, 1.0, 0.0)

        GL11.glEnable(GL11.GL_BLEND)
        GL20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        GL20.glColor4f(0f, 0f, 0f, 1f)

        blendsTex.forEach { (tex, blend) ->
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex)

            GL11.glBegin(GL11.GL_QUADS)
            GL11.glTexCoord2i(0,0); GL11.glVertex2d(blend.x.toDouble(), blend.y.toDouble())
            GL11.glTexCoord2i(0, 1); GL11.glVertex2d(blend.x.toDouble(), (blend.y + blend.height).toDouble())
            GL11.glTexCoord2i(1, 1); GL11.glVertex2d((blend.x + blend.width).toDouble(), (blend.y + blend.height).toDouble())
            GL11.glTexCoord2i(1, 0); GL11.glVertex2d((blend.x + blend.width).toDouble(), blend.y.toDouble())
            GL11.glEnd()
        }

        GL11.glDisable(GL11.GL_BLEND)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
        GL11.glPopMatrix()
    }

    fun updateWindowConfigs(windowConfig: WindowConfig) {
        shutdown()

        windowConfig.blends.forEach { blend ->
            val blendImage = BlendGenerator.makeBlender(blend)
            val blendBuffer = BufferUtils.createByteBuffer(blend.width * blend.height * 4)
            RGBImageCopy.copyImageToBuffer(blendImage, blendBuffer, true)

            val tex = GL11.glGenTextures()
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex)
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST.toFloat())
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST.toFloat())
            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA,
                blend.width,
                blend.height,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                blendBuffer
            )

            blendsTex[tex] = blend
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
    }

    fun shutdown() {
        blendsTex.keys.forEach { GL11.glDeleteTextures(it) }
        blendsTex.clear()
    }
}