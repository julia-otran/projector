package dev.juhouse.projector.projection2

import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper

class ProjectionWindowCapture(private val delegate: CanvasDelegate): Projectable {
    private val renderFlagProperty = ReadOnlyObjectWrapper<BridgeRenderFlag>()

    override fun init() {
        renderFlagProperty.set(BridgeRenderFlag(delegate))
    }

    override fun finish() {

    }

    override fun rebuild() {
        renderFlagProperty.get().applyDefault { it.enableRenderVideo }
    }

    override fun setRender(render: Boolean) {

    }

    override fun getRenderFlagProperty(): ReadOnlyObjectProperty<BridgeRenderFlag> {
        return renderFlagProperty.readOnlyProperty
    }

    fun getWindowList(): List<String> {
        return delegate.bridge.windowList.toList()
    }
}