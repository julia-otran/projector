package dev.juhouse.projector.projection2.image

import dev.juhouse.projector.projection2.BridgeRenderFlag
import dev.juhouse.projector.projection2.CanvasDelegate
import dev.juhouse.projector.projection2.Projectable
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.scene.image.Image
import javafx.scene.image.PixelFormat
import java.lang.RuntimeException
import java.nio.IntBuffer
import kotlin.math.ceil

class ProjectionMultiImage(private val delegate: CanvasDelegate): Projectable {
    private val imagesMap = HashMap<Int, Image>()
    private val presentUniqueImageMap = HashMap<Int, PresentUniqueImage>()

    private var render = false
    private val renderFlagProperty = ReadOnlyObjectWrapper<BridgeRenderFlag>()

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
        delegate.bridge.renderSettings.forEach {
            presentUniqueImageMap[it.renderId] = PresentUniqueImage(it.renderId, delegate.bridge)
        }
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

    fun setImages(imagesMap: Map<Int, Image>) {
        this.imagesMap.clear()
        this.imagesMap.putAll(imagesMap)

        updateImages()
        updateBridge()
    }

    private fun updateImages() {
        val buffer = imagesMap.values.maxOfOrNull { it.width * it.height }?.let { IntBuffer.allocate(ceil(it).toInt()) }

        presentUniqueImageMap.forEach { (renderId, presentImage) ->
            imagesMap[renderId]?.let { image ->
                val w = image.width.toInt()
                val h = image.height.toInt()

                renderFlagProperty.get().enableRenderId(renderId)

                buffer?.let { buffer ->
                    image.pixelReader.getPixels(0, 0, w, h, PixelFormat.getIntArgbPreInstance(), buffer.array(), 0, w)
                    presentImage.update(buffer.array(), w, h)
                }

                true
            } ?: kotlin.run {
                presentImage.update(null, 0, 0)
                renderFlagProperty.get().disableRenderId(renderId)
            }
        }
    }

    private fun updateBridge() {
        presentUniqueImageMap.forEach { (renderId, presentImage) ->
            if (imagesMap[renderId] != null && render) {
                presentImage.present()
            } else {
                presentImage.hide()
            }
        }
    }
}