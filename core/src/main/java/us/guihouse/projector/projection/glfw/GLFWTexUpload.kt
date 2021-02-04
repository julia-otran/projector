package us.guihouse.projector.projection.glfw

import java.awt.image.BufferedImage

interface GLFWTexUpload {
    fun enqueue(img: BufferedImage)
    fun updateTex(texId: Int)
    fun start()
    fun stop()
}
