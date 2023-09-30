package dev.juhouse.projector.projection2

import dev.juhouse.projector.projection2.Bridge.VideoPreviewOutputBufferTooSmall
import dev.juhouse.projector.projection2.video.PlayerPreview
import dev.juhouse.projector.projection2.video.PlayerPreviewCallback
import dev.juhouse.projector.projection2.video.PlayerPreviewCallbackFrameSize
import javafx.beans.Observable
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import lombok.Getter
import java.nio.ByteBuffer
import java.util.*

class ProjectionVideoCapture(private val delegate: CanvasDelegate): Projectable {
    @Getter
    private val render: BooleanProperty = SimpleBooleanProperty(false)
    private val renderFlag = BridgeRenderFlag(delegate)

    private var width: Int = 0
    private var height: Int = 0

    private val previewCallback = object : PlayerPreviewCallback {
        override fun getFrame(buffer: ByteBuffer): PlayerPreviewCallbackFrameSize {
            if (buffer.capacity() < width * height * 4) {
                throw VideoPreviewOutputBufferTooSmall()
            }

            delegate.bridge.downloadVideoCapturePreview(buffer)

            return PlayerPreviewCallbackFrameSize(width, height)
        }

        override fun isRender(): Boolean {
            return render.get()
        }
    }

    val previewPanel = PlayerPreview(previewCallback, delegate)

    var enabled = false
        set(value) {
            if (field != value) {
                field = value
                delegate.bridge.setVideoCaptureEnabled(value)
            }
        }

    var cropVideo = false
        set(value) {
            field = value
            delegate.bridge.setVideoCaptureCrop(value)
        }

    init {
        renderFlag.flagValueProperty.addListener { _: Observable? -> updateRender() }
    }

    override fun getRenderFlag(): BridgeRenderFlag {
        return renderFlag
    }

    override fun init() {
        renderFlag.applyDefault(BridgeRender::enableRenderBackgroundAssets)
    }

    override fun rebuild() {
        renderFlag.applyDefault(BridgeRender::enableRenderBackgroundAssets)
    }

    override fun setRender(render: Boolean) {
        if (this.render.get() != render) {
            this.render.value = render
            updateRender()
        }
    }

    private fun updateRender() {
        if (render.get()) {
            delegate.bridge.setVideoCaptureRender(renderFlag.flagValue)
        } else {
            delegate.bridge.setVideoCaptureRender(BridgeRenderFlag.NO_RENDER)
        }
    }

    fun getDevices(): List<BridgeCaptureDevice> {
        return delegate.bridge.videoCaptureDevices?.asList() ?: Collections.emptyList()
    }

    fun setDevice(name: String, width: Int, height: Int) {
        this.width = width
        this.height = height
        delegate.bridge.setVideoCaptureDevice(name, width, height)
    }

    override fun finish() {
        enabled = false
    }
}