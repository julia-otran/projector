package us.guihouse.projector.projection.glfw

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import us.guihouse.projector.models.WindowConfig
import us.guihouse.projector.models.WindowConfigHelpLine
import java.awt.Rectangle

class GLFWHelperLines(private val bounds: Rectangle) {
    private var helpLines = ArrayList<WindowConfigHelpLine>()

    fun render() {
        GL11.glEnable(GL11.GL_BLEND)
        GL20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        GL20.glEnable(GL20.GL_MULTISAMPLE)

        GL20.glColor4f(1f, 1f, 1f, 1f)

        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPushMatrix()
        GL11.glOrtho(0.0, bounds.width.toDouble(), bounds.height.toDouble(), 0.0, 1.0, 0.0)

        helpLines.forEach {
            GL11.glBegin(GL11.GL_LINES)
            GL11.glVertex2d(it.x1.toDouble(), it.y1.toDouble())
            GL11.glVertex2d(it.x2.toDouble(), it.y2.toDouble())
            GL11.glEnd()
        }

        GL11.glPopMatrix()
        GL20.glDisable(GL20.GL_MULTISAMPLE)
        GL11.glDisable(GL11.GL_BLEND)
    }

    fun updateWindowConfig(windowConfig: WindowConfig) {
        helpLines.clear()
        helpLines.addAll(windowConfig.helpLines)
    }
}