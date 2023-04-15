package dev.juhouse.projector.projection2

import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper

class ProjectionWindowCapture(private val delegate: CanvasDelegate): Projectable {
    private val renderFlagProperty = ReadOnlyObjectWrapper<BridgeRenderFlag>()
    private var windowName: String? = null
    private var render: Boolean = false

    override fun init() {
        renderFlagProperty.set(BridgeRenderFlag(delegate))
    }

    override fun finish() {

    }

    override fun rebuild() {
        renderFlagProperty.get().applyDefault { it.enableRenderVideo }
    }

    override fun setRender(render: Boolean) {
        this.render = render;
        updateRender()
    }

    fun setWindowCaptureName(name: String?) {
        this.windowName = name
        updateRender()
    }

    private fun updateRender() {
        if (render && !windowName.isNullOrBlank()) {
            delegate.bridge.setWindowCaptureWindowName(windowName)
            delegate.bridge.setWindowCaptureRender(renderFlagProperty.get().flagValue);
        } else {
            delegate.bridge.setWindowCaptureRender(BridgeRenderFlag.NO_RENDER);
        }
    }

    override fun getRenderFlagProperty(): ReadOnlyObjectProperty<BridgeRenderFlag> {
        return renderFlagProperty.readOnlyProperty
    }

    fun getWindowList(): List<String> {
        return delegate.bridge.windowList.toList()
    }

    fun setCrop(newVal: Boolean) {
        delegate.bridge.setWindowCaptureCrop(newVal)
    }
}