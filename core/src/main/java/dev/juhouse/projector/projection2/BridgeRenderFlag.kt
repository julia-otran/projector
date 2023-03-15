package dev.juhouse.projector.projection2

import javafx.beans.property.ReadOnlyIntegerProperty
import javafx.beans.property.ReadOnlyIntegerWrapper

class BridgeRenderFlag(private val delegate: CanvasDelegate) {
    companion object {
        const val NO_RENDER = 0
        const val RENDER_ALL = Int.MAX_VALUE
    }

    private val flagValueIntProperty = ReadOnlyIntegerWrapper(0)

    val flagValueProperty: ReadOnlyIntegerProperty = flagValueIntProperty.readOnlyProperty

    val flagValue: Int get() { return flagValueIntProperty.value }

    fun isRenderEnabled(renderId: Int): Boolean {
        return (flagValueIntProperty.get() and (1 shl renderId)) > 0
    }

    fun enableRenderId(renderId: Int) {
        flagValueIntProperty.set(flagValueIntProperty.get() or (1 shl renderId))
    }

    fun disableRenderId(renderId: Int) {
        flagValueIntProperty.set(flagValueIntProperty.get() and (1 shl renderId).inv())
    }

    fun renderToAll() {
        flagValueIntProperty.set(RENDER_ALL)
    }

    fun applyDefault(configMapper: (config: BridgeRender) -> Boolean) {
        delegate.bridge.renderSettings.forEach {
            if (configMapper(it)) {
                enableRenderId(it.renderId)
            } else {
                disableRenderId(it.renderId)
            }
        }
    }
}