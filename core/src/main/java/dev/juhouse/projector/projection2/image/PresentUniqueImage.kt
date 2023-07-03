package dev.juhouse.projector.projection2.image

import dev.juhouse.projector.projection2.Bridge

class PresentUniqueImage(val renderId: Int, val bridge: Bridge) {
    private var data: IntArray? = null
    private var width: Int = 0
    private var height: Int = 0
    var crop: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                update()
            }
        }

    companion object {
        private val presentingImageMap = HashMap<Int, PresentUniqueImage>()
    }

    fun update(data: IntArray?, width: Int, height: Int) {
        update(data, width, height, false)
    }

    fun update(data: IntArray?, width: Int, height: Int, crop: Boolean) {
        this.data = data
        this.width = width
        this.height = height
        this.crop = crop

        update()
    }

    fun update() {
        if (isPresenting()) {
            if (data == null) {
                hide()
            } else {
                bridge.setMultiImageAsset(data, width, height, renderId, crop)
            }
        }
    }

    private fun isPresenting(): Boolean {
        return presentingImageMap[renderId] == this
    }
    fun present() {
        if (!isPresenting() && data != null) {
            presentingImageMap[renderId] = this
            bridge.setMultiImageAsset(data, width, height, renderId, crop)
        }
    }
    fun hide() {
        if (isPresenting()) {
            presentingImageMap.remove(renderId)
            bridge.setMultiImageAsset(null, width, height, renderId, crop)
        }
    }
}