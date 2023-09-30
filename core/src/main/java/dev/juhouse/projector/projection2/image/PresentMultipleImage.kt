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

        renderFlag.property.addListener(changeListener);
    }

    private fun updateImages() {
        updateImages(false)
    }

    private fun updateImages(force: Boolean) {
        currentImages.forEach { (renderId, image) ->
            val shouldPresent = renderFlag.isRenderEnabled(renderId) && render

            if (force && shouldPresent == image.isPresenting()) {
                image.update()
            } else if (shouldPresent) {
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

    fun rebuild() {
        bridge.renderSettings.forEach {
            if (currentImages[it.renderId] == null) {
                currentImages.values.first().let { img ->
                    currentImages[it.renderId] = img.cloneWithRenderId(it.renderId)
                }
            }
        }

        updateImages(true)
    }
}