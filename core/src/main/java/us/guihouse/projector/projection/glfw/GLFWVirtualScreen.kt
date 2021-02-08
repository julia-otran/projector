package us.guihouse.projector.projection.glfw

import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.system.MemoryUtil
import us.guihouse.projector.models.WindowConfig
import us.guihouse.projector.other.EventQueue
import us.guihouse.projector.other.RuntimeProperties
import us.guihouse.projector.projection.models.VirtualScreen
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.lang.RuntimeException

class GLFWVirtualScreen(private val virtualScreen: VirtualScreen, private val windows: Map<String, GLFWWindow>, private val windowConfigs: Map<String, WindowConfig>) {
    private val eventQueue = EventQueue(10)

    private var glWindow = 0L
    private var glTexUploadWindow = 0L

    private var texGLFWTexUpload: GLFWTexUpload? = null

    private var bounds: Rectangle? = null

    private var looper = Loop()

    private var texture = 0

    fun init() {
        bounds = Rectangle(virtualScreen.width, virtualScreen.height)

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        glWindow = GLFW.glfwCreateWindow(640, 480, "Projector VS", MemoryUtil.NULL, 0)

        if (glWindow == 0L) {
            throw RuntimeException("Cannot create GLFW window")
        }

        glTexUploadWindow = GLFW.glfwCreateWindow(640, 480, "Projector Async Tex Upload", MemoryUtil.NULL, glWindow)

        if (glTexUploadWindow == 0L) {
            throw RuntimeException("Cannot create GLFW window")
        }

        windows.values.forEach {
            it.createWindow(glWindow)
            it.makeVisible()
        }

        eventQueue.setStartRunnable(Starter())
        eventQueue.setStopRunnable(Stopper())

        eventQueue.init()
    }

    fun updateImage(src: BufferedImage) {
        texGLFWTexUpload?.enqueue(src)
    }

    internal inner class Starter : Runnable {
        override fun run() {
            windows.forEach { (id, window) ->
                window.init(windowConfigs[id], virtualScreen)
            }

            GLFW.glfwMakeContextCurrent(glWindow)
            GL.createCapabilities()

            texGLFWTexUpload = if (GLFWExtensions.isPboSupported()) {
                GLFWAsyncTexUpload(bounds!!, glTexUploadWindow)
            } else {
                GLFWSyncTexUpload(bounds!!)
            }

            texGLFWTexUpload!!.start()

            texture = GL11.glGenTextures()

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture)
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST.toFloat())
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST.toFloat())

            val buffer = BufferUtils.createByteBuffer(bounds!!.width * bounds!!.height * 3)
            buffer.flip()

            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGB,
                bounds!!.width,
                bounds!!.height,
                0,
                GL11.GL_RGB,
                GL11.GL_UNSIGNED_BYTE,
                buffer
            )

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)

            eventQueue.enqueueContinuous(looper)
        }

    }

    internal inner class Loop : Runnable {
        private var frames = 0
        private var time = 0L

        override fun run() {
            if (RuntimeProperties.isLogFPS()) {
                val current = System.nanoTime()
                frames++
                if (current - time > 1000000000) {
                    println("GL Frames $frames")
                    time = current
                    frames = 0
                }
            }

            GLFW.glfwMakeContextCurrent(glWindow)
            texGLFWTexUpload!!.updateTex(texture)

            windows.values.forEach { it.loopCycle(texture) }
            GLFW.glfwPollEvents()
        }
    }

    internal inner class Stopper : Runnable {
        override fun run() {
            eventQueue.removeContinuous(looper)
            windows.values.forEach { it.shutdown() }
            GLFW.glfwDestroyWindow(glWindow)
        }
    }

    fun shutdown() {
        eventQueue.stop()
    }

    fun updateWindowConfigs(newWindowConfigs: MutableList<WindowConfig>) {
        eventQueue.enqueueForRun {
            newWindowConfigs.forEach {
                windows[it.displayId]?.updateWindowConfig(it)
            }
        }
    }


}