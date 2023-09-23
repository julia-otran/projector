package dev.juhouse.projector.projection2.image

import dev.juhouse.projector.projection2.BridgeRender
import dev.juhouse.projector.projection2.BridgeRenderFlag
import dev.juhouse.projector.projection2.CanvasDelegate
import dev.juhouse.projector.projection2.Projectable
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.scene.image.Image
import javafx.scene.image.PixelFormat
import java.lang.RuntimeException
import java.nio.IntBuffer
import java.util.Arrays
import kotlin.math.ceil
import kotlin.math.roundToInt

class ProjectionSolidColor(private val delegate: CanvasDelegate): Projectable {
    private val presentUniqueImageMap = HashMap<Int, PresentUniqueImage>()

    private var render = false
    private val renderFlagProperty = ReadOnlyObjectWrapper<BridgeRenderFlag>()

    private var colorRGBA = 0u

    private var buffer: IntBuffer? = null
    private val renderSettings: ArrayList<BridgeRender> = ArrayList()

    override fun init() {
        renderFlagProperty.set(BridgeRenderFlag(delegate))

        delegate.bridge.renderSettings.forEach {
            presentUniqueImageMap[it.renderId] = PresentUniqueImage(it.renderId, delegate.bridge)
        }
    }

    override fun finish() {

    }

    override fun rebuild() {
        presentUniqueImageMap.clear()

        renderSettings.clear()
        renderSettings.addAll(delegate.bridge.renderSettings)

        renderSettings.forEach {
            presentUniqueImageMap[it.renderId] = PresentUniqueImage(it.renderId, delegate.bridge)
        }

        buffer = renderSettings
            .toList()
            .maxOfOrNull { it.width * it.height }?.let { IntBuffer.allocate(it) }

        updateImages()
        updateBridge()
    }

    override fun setRender(render: Boolean) {
        this.render = render
        updateBridge()
    }

    override fun getRenderFlagProperty(): ReadOnlyObjectProperty<BridgeRenderFlag> {
        return renderFlagProperty.readOnlyProperty
    }

    fun setColor(rgb: DoubleArray) {
        colorRGBA = (((rgb[0] * 255.0).roundToInt().toUInt() shl 16) and 0x00FF0000u) or
                    (((rgb[1] * 255.0).roundToInt().toUInt() shl 8) and 0x0000FF00u) or
                    (((rgb[2] * 255.0).roundToInt().toUInt() shl 0) and 0x000000FFu) or
                    0xFF000000u

        updateImages()
        updateBridge()
    }

    private fun updateImages() {
        val arr = buffer?.array()

        arr?.let { data ->
            for (i in data.indices) {
                arr[i] = colorRGBA.toInt()
            }
        }

        presentUniqueImageMap.forEach { (renderId, presentImage) ->
            arr?.let { data ->
                renderSettings.find { it.renderId == renderId }?.let {
                    if (renderFlagProperty.get().isRenderEnabled(renderId)) {
                        presentImage.update(data, it.width, it.height)
                        true
                    } else {
                        false
                    }
                }
            } ?: kotlin.run {
                presentImage.update(null, 0, 0)
                renderFlagProperty.get().disableRenderId(renderId)
            }
        }
    }

    private fun updateBridge() {
        presentUniqueImageMap.forEach { (_, presentImage) ->
            if (render) {
                presentImage.present()
            } else {
                presentImage.hide()
            }
        }
    }
}