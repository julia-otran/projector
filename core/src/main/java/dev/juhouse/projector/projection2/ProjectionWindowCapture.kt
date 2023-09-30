package dev.juhouse.projector.projection2

import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper

class ProjectionWindowCapture(private val delegate: CanvasDelegate): Projectable {
    private val renderFlag = BridgeRenderFlag(delegate)
    private var windowName: String? = null
    private var render: Boolean = false

    override fun init() {

    }

    override fun finish() {

    }

    override fun rebuild() {
        renderFlag.applyDefault { it.enableRenderVideo }
    }

    override fun setRender(render: Boolean) {
        this.render = render;
        updateRender()
    }

    override fun getRenderFlag(): BridgeRenderFlag = renderFlag

    fun setWindowCaptureName(name: String?) {
        this.windowName = name
        updateRender()
    }

    private fun updateRender() {
        if (render && !windowName.isNullOrBlank()) {
            delegate.bridge.setWindowCaptureWindowName(windowName)
            delegate.bridge.setWindowCaptureRender(renderFlag.flagValue);
        } else {
            delegate.bridge.setWindowCaptureRender(BridgeRenderFlag.NO_RENDER);
        }
    }

    fun getWindowList(): List<String> {
        return delegate.bridge.windowList.toList()
    }

    fun setCrop(newVal: Boolean) {
        delegate.bridge.setWindowCaptureCrop(newVal)
    }
}