package dev.juhouse.projector.projection2

import dev.juhouse.projector.projection2.Bridge.VideoPreviewNoOutputData
import dev.juhouse.projector.projection2.Bridge.VideoPreviewOutputBufferTooSmall
import dev.juhouse.projector.projection2.video.PlayerPreview
import dev.juhouse.projector.projection2.video.PlayerPreviewCallback
import dev.juhouse.projector.projection2.video.PlayerPreviewCallbackFramePixelFormat
import dev.juhouse.projector.projection2.video.PlayerPreviewCallbackFrameSize
import javafx.beans.Observable
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import lombok.Getter
import java.nio.ByteBuffer

class ProjectionNDICapture(private val delegate: CanvasDelegate): Projectable {
    @Getter
    private val render: BooleanProperty = SimpleBooleanProperty(false)
    private val renderFlag = BridgeRenderFlag(delegate)

    private var previewInfo = BridgeNDIPreviewInfo(0, 0, 0, 0U)

    private val previewCallback = object : PlayerPreviewCallback {
        override fun getFrame(buffer: ByteBuffer): PlayerPreviewCallbackFrameSize {
            delegate.bridge.downloadNDIPreview(buffer, previewInfo)

            if (buffer.capacity() < previewInfo.width * previewInfo.height * previewInfo.bytesPerPixel) {
                throw VideoPreviewOutputBufferTooSmall()
            }

            if (previewInfo.width * previewInfo.height <= 0) {
                throw VideoPreviewNoOutputData()
            }

            return PlayerPreviewCallbackFrameSize(previewInfo.width, previewInfo.height, PlayerPreviewCallbackFramePixelFormat.fromGL(previewInfo.pixelFormat))
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
                delegate.bridge.setNDIEnabled(value)
            }
        }

    var cropVideo = false
        set(value) {
            field = value
            delegate.bridge.setNDICrop(value)
        }

    init {
        renderFlag.property.addListener { _: Observable? -> updateRender() }
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
            delegate.bridge.setNDIRender(renderFlag.value)
        } else {
            delegate.bridge.setNDIRender(BridgeRenderFlag.NO_RENDER)
        }
    }

    fun getDevices() {
        delegate.bridge.searchNDIDevices()
    }

    fun addDeviceChangeCallback(callback: BridgeNDIDeviceFindCallback) {
        delegate.bridge.addNDIDeviceFindCallback(callback)
    }

    fun removeDeviceChangeCallback(callback: BridgeNDIDeviceFindCallback) {
        delegate.bridge.removeNDIDeviceFindCallback(callback)
    }

    fun setDevice(name: String) {
        delegate.bridge.connectNDIDevice(name)
    }

    override fun finish() {
        enabled = false
    }
}