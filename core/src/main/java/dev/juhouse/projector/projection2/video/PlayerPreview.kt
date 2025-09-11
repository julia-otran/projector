package dev.juhouse.projector.projection2.video

import dev.juhouse.projector.projection2.Bridge.VideoPreviewNoOutputData
import dev.juhouse.projector.projection2.Bridge.VideoPreviewOutputBufferTooSmall
import dev.juhouse.projector.projection2.CanvasDelegate
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Rectangle2D
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.util.Callback
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.jvm.Throws

enum class PlayerPreviewCallbackFramePixelFormat {
    GL_RGBA;

    companion object {
        fun fromGL(gluintPixelFormat: UInt): PlayerPreviewCallbackFramePixelFormat {
            return when (gluintPixelFormat) {
                6408U -> GL_RGBA
                else -> GL_RGBA
            }
        }
    }
}

data class PlayerPreviewCallbackFrameSize(val width: Int, val height: Int, val pixelFormat: PlayerPreviewCallbackFramePixelFormat = PlayerPreviewCallbackFramePixelFormat.GL_RGBA)

interface PlayerPreviewCallback {
    fun isRender(): Boolean
    @Throws(VideoPreviewOutputBufferTooSmall::class, VideoPreviewNoOutputData::class)
    fun getFrame(buffer: ByteBuffer): PlayerPreviewCallbackFrameSize
}

class PlayerPreview(private val previewCallback: PlayerPreviewCallback, delegate: CanvasDelegate) : AnchorPane(), Runnable {
    private var running = false
    private var previewImagePixelBuffer: PixelBuffer<IntBuffer>? = null
    private val previewImageBuffer: ByteBuffer
    private val previewImageBufferInt: IntBuffer
    private val delegate: CanvasDelegate
    private val previewImageView: ImageView = ImageView()
    private val previewErrorLabel: Label
    private val updateCallback =
        Callback<PixelBuffer<IntBuffer>, Rectangle2D> { _ ->
            updating = false
            null
        }

    private var updating = false

    init {
        previewImageView.isPreserveRatio = true
        setMinSize(0.0, 0.0)
        previewImageView.fitWidthProperty().bind(widthProperty().subtract(1))
        previewImageView.fitHeightProperty().bind(heightProperty().subtract(1))
        previewErrorLabel = Label()
        previewErrorLabel.textFill = Color.color(1.0, 1.0, 1.0)
        previewErrorLabel.padding = Insets(10.0)
        this.delegate = delegate
        previewImageBuffer = ByteBuffer.allocateDirect(1920 * 1920 * 4)
        previewImageBufferInt = previewImageBuffer.asIntBuffer()
    }

    fun startPreview() {
        running = true
        Thread(this).start()
    }

    fun stopPreview() {
        running = false
    }

    private fun showError(message: String) {
        Platform.runLater {
            previewErrorLabel.text = message
            children.clear()
            children.add(previewErrorLabel)
            updating = false
        }
    }

    private fun updatePreview() {
        if (updating) {
            return
        }
        if (previewCallback.isRender()) {
            showError("[Preview Indisponível] Projetando Vídeo....")
            return
        }
        updating = true

        try {
            val (width, height, pixelFormat) = previewCallback.getFrame(previewImageBuffer)

            Platform.runLater {
                if (!running) {
                    return@runLater
                }
                if (children.isEmpty() || children[0] !== previewImageView) {
                    children.clear()
                    children.add(previewImageView)
                }
                if (width == 0 || height == 0) {
                    updating = false
                    return@runLater
                }
                if (previewImagePixelBuffer == null || previewImagePixelBuffer!!.width != width || previewImagePixelBuffer!!.height != height) {
                    previewImagePixelBuffer = when (pixelFormat) {
                        PlayerPreviewCallbackFramePixelFormat.GL_RGBA -> PixelBuffer(
                            width,
                            height,
                            previewImageBufferInt,
                            PixelFormat.getIntArgbPreInstance()
                        )
                    }

                    previewImageView.image = WritableImage(previewImagePixelBuffer)
                    updating = false
                } else {
                    previewImagePixelBuffer!!.updateBuffer(updateCallback)

                }
            }
        } catch (e: VideoPreviewOutputBufferTooSmall) {
            showError("[Preview Indisponível] Resolução do video muito alta (max 1920x1080)....")
            updating = false
        } catch (e: VideoPreviewNoOutputData) {
            showError("[Preview Indisponível] Sem dados para exibir....")
            updating = false
        }
    }

    override fun run() {
        updating = false
        while (running) {
            updatePreview()
            try {
                Thread.sleep(50)
            } catch (e: InterruptedException) {
                running = false
            }
        }
    }
}
