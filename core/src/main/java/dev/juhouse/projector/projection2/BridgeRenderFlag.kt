package dev.juhouse.projector.projection2

import javafx.beans.property.ReadOnlyIntegerProperty
import javafx.beans.property.ReadOnlyIntegerWrapper

class BridgeRenderFlag(private val delegate: CanvasDelegate?) {
    companion object {
        const val NO_RENDER = 0
        const val RENDER_ALL = Int.MAX_VALUE
    }

    private val flagValueIntProperty = ReadOnlyIntegerWrapper(0)

    val flagValueProperty: ReadOnlyIntegerProperty = flagValueIntProperty.readOnlyProperty

    var flagValue: Int get() { return flagValueIntProperty.value } set(value) { flagValueIntProperty.value = value }

    fun getRenders(): List<Int> {
        val result = ArrayList<Int>()

        for (i in 1..31) {
            if (isRenderEnabled(i)) {
                result.add(i)
            }
        }

        return result
    }

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

    fun renderToNone() {
        flagValueIntProperty.set(NO_RENDER)
    }

    fun hasAnyRender(): Boolean {
        return flagValueIntProperty.get() != NO_RENDER
    }

    fun applyDefault(configMapper: (config: BridgeRender) -> Boolean) {
        delegate?.bridge?.renderSettings?.forEach {
            if (configMapper(it)) {
                enableRenderId(it.renderId)
            } else {
                disableRenderId(it.renderId)
            }
        }
    }

    fun exclude(other: BridgeRenderFlag): Int {
        return this.flagValue and (other.flagValue.inv())
    }
}