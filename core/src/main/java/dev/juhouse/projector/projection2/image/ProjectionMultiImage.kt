package dev.juhouse.projector.projection2.image

import dev.juhouse.projector.projection2.BridgeRenderFlag
import dev.juhouse.projector.projection2.CanvasDelegate
import dev.juhouse.projector.projection2.Projectable
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.scene.image.Image
import javafx.scene.image.PixelFormat
import java.lang.RuntimeException
import java.nio.IntBuffer
import kotlin.math.ceil

class ProjectionMultiImage(private val delegate: CanvasDelegate): Projectable {
    private val imagesMap = HashMap<Int, Image>()
    private var render = false
    private var cleared = true
    private var buffer: IntBuffer? = null

    override fun init() {

    }

    override fun finish() {

    }

    override fun rebuild() {

    }

    override fun setRender(render: Boolean) {
        this.render = render
        updateBridge()
    }

    override fun getRenderFlagProperty(): ReadOnlyObjectProperty<BridgeRenderFlag> {
        throw RuntimeException("MultiImage does not have render flag")
    }

    fun setImages(imagesMap: Map<Int, Image>) {
        this.imagesMap.clear()
        this.imagesMap.putAll(imagesMap)

        imagesMap.values.maxOfOrNull { it.width * it.height }?.let {
            buffer = IntBuffer.allocate(ceil(it).toInt())
        }

        updateBridge()
    }

    private fun updateBridge() {
        delegate.bridge.renderSettings.forEach {
            if (render) {
                imagesMap[it.renderId]?.let { image ->
                    val w = image.width.toInt()
                    val h = image.height.toInt()

                    buffer?.let { buffer ->
                        image.pixelReader.getPixels(0, 0, w, h, PixelFormat.getIntArgbPreInstance(), buffer.array(), 0, w)
                        delegate.bridge.setMultiImageAsset(buffer.array(), w, h, it.renderId)
                        cleared = false
                        true
                    }
                } ?: run {
                    delegate.bridge.setMultiImageAsset(null, 0, 0, it.renderId)
                    cleared = true
                }
            } else {
                if (!cleared) {
                    cleared = true
                    delegate.bridge.setMultiImageAsset(null, 0, 0, it.renderId)
                }
            }
        }

    }
}