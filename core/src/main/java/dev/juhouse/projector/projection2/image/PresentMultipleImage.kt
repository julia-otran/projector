package dev.juhouse.projector.projection2.image

import dev.juhouse.projector.projection2.Bridge
import dev.juhouse.projector.projection2.BridgeRenderFlag
import javafx.beans.value.ChangeListener
import kotlin.collections.HashMap

class PresentMultipleImage(private val renderFlag: BridgeRenderFlag, val bridge: Bridge) {
    private val currentImages = HashMap<Int, PresentUniqueImage>()
    var render = false
        set(value) {
            field = value
            updateImages()
        }

    init {
        bridge.renderSettings.forEach {
            currentImages[it.renderId] = PresentUniqueImage(it.renderId, bridge)
        }

        val changeListener = ChangeListener<Number> { _, _, _ ->
            updateImages()
        }

        renderFlag.flagValueProperty.addListener(changeListener);
    }

    private fun updateImages() {
        currentImages.forEach { (renderId, image) ->
            if (renderFlag.isRenderEnabled(renderId) && render) {
                image.present()
            } else {
                image.hide()
            }
        }
    }

    fun setCrop(crop: Boolean) {
        currentImages.forEach { (_, image) ->
            image.crop = crop
        }
    }

    fun update(data: IntArray?, width: Int, height: Int, crop: Boolean) {
        currentImages.forEach { (_, image) ->
            image.update(data, width, height, crop)
        }
    }
}