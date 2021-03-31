package us.guihouse.projector.projection.glfw

import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import org.lwjgl.system.MemoryUtil
import us.guihouse.projector.models.WindowConfig
import us.guihouse.projector.other.EventQueue
import us.guihouse.projector.other.RuntimeProperties
import us.guihouse.projector.projection.ProjectionCanvas
import us.guihouse.projector.projection.models.VirtualScreen
import java.awt.Rectangle
import java.lang.RuntimeException

class GLFWVirtualScreen(private val projectionCanvas: ProjectionCanvas,
                        private val virtualScreen: VirtualScreen,
                        private val windows: Map<String, GLFWWindow>,
                        private val windowConfigs: Map<String, WindowConfig>,
                        private val previewWindow: GLFWPreviewWindow?) {
    private val eventQueue = EventQueue(5)

    private var glWindow = 0L
    private var glDrawerWindow = 0L

    private var bounds: Rectangle? = null

    private var looper = Loop()

    private var glTexture = 0
    private var glFrameBuffer = 0
    private var glDepthRenderBuffer = 0

    private val drawer = GLFWGraphicsAdapterDrawer()

    fun init(): Long {
        bounds = Rectangle(virtualScreen.width, virtualScreen.height)

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)

        glWindow = GLFW.glfwCreateWindow(640, 480, "Projector VS", MemoryUtil.NULL, 0)

        if (glWindow == 0L) {
            throw RuntimeException("Cannot create GLFW window")
        }

        glDrawerWindow = GLFW.glfwCreateWindow(640, 480, "Projector VS Draw", MemoryUtil.NULL, glWindow)

        if (glDrawerWindow == 0L) {
            throw RuntimeException("Cannot create GLFW drawer window")
        }

        windows.values.forEach {
            it.createWindow(glWindow)
        }

        previewWindow?.createWindow(glWindow)
        previewWindow?.init(virtualScreen)

        eventQueue.setStartRunnable(Starter())
        eventQueue.setStopRunnable(Stopper())

        drawer.init(glDrawerWindow, projectionCanvas, bounds!!, virtualScreen)
        eventQueue.init()

        return glWindow
    }

    fun runOnProvider(callback: GLFWGraphicsAdapterProvider.Callback) {
        drawer.enqueueForRun {
            callback.run(drawer)
        }
    }

    internal inner class Starter : Runnable {
        override fun run() {
            windows.forEach { (id, window) ->
                window.init(windowConfigs[id], virtualScreen)
            }

            GLFW.glfwMakeContextCurrent(glWindow)
            GL.createCapabilities()

            glTexture = GL11.glGenTextures()

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTexture)
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST.toFloat())
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST.toFloat())

            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA,
                bounds!!.width,
                bounds!!.height,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                0
            )

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)

            glDepthRenderBuffer = GL30.glGenRenderbuffers()
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, glDepthRenderBuffer)
            GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, bounds!!.width, bounds!!.height)
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0)

            glFrameBuffer = GL30.glGenFramebuffers()
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, glFrameBuffer)
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_TEXTURE_2D, glTexture, 0)
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, glDepthRenderBuffer)

            if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw RuntimeException("Framebuffer incomplete")
            }
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)

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

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, glFrameBuffer)
            GL30.glPushMatrix()
            GL30.glViewport(0, 0, bounds!!.width, bounds!!.height)
            GL30.glClearColor(0.1f, 0f, 0.1f, 1.0f)
            GL30.glClear(GL30.GL_COLOR_BUFFER_BIT)

            drawer.drawNextFrame()

            GL30.glPopMatrix()

            previewWindow?.loopCycle()

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)

            windows.values.forEach { it.loopCycle(glTexture) }
        }
    }

    internal inner class Stopper : Runnable {
        override fun run() {
            eventQueue.removeContinuous(looper)
            drawer.stop()
            previewWindow?.shutdown()
            GL11.glDeleteTextures(glTexture)
            GL30.glDeleteRenderbuffers(glDepthRenderBuffer)
            GL30.glDeleteFramebuffers(glFrameBuffer)
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