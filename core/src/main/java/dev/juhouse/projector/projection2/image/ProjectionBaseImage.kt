package dev.juhouse.projector.projection2.image

import dev.juhouse.projector.projection2.BridgeRender
import dev.juhouse.projector.projection2.BridgeRenderFlag
import dev.juhouse.projector.projection2.CanvasDelegate
import dev.juhouse.projector.projection2.Projectable
import dev.juhouse.projector.projection2.models.BackgroundProvide
import javafx.scene.image.WritablePixelFormat
import java.nio.IntBuffer
import kotlin.math.roundToInt

abstract class ProjectionBaseImage(protected val canvasDelegate: CanvasDelegate): Projectable {
    private val renderFlag: BridgeRenderFlag = BridgeRenderFlag(canvasDelegate)

    private var cropBackground = false

    private var render = false

    private var model: BackgroundProvide? = null

    private lateinit var presentImage: PresentMultipleImage

    override fun init() {
        presentImage = PresentMultipleImage(renderFlag, canvasDelegate.bridge)
    }

    override fun finish() {}

    override fun rebuild() {
        presentImage.rebuild()
    }

    override fun getRenderFlag(): BridgeRenderFlag {
        return renderFlag
    }

    fun getCropBackground(): Boolean {
        return cropBackground
    }

    override fun setRender(value: Boolean) {
        if (this.render != value) {
            this.render = value
            update()
        }
    }

    fun setCropBackground(cropBackground: Boolean) {
        if (this.cropBackground != cropBackground) {
            this.cropBackground = cropBackground

            presentImage.setCrop(cropBackground)
        }
    }

    fun getModel(): BackgroundProvide? {
        return model
    }

    fun setModel(model: BackgroundProvide?) {
        this.model = model
        updateModel()
        update()
    }

    private fun updateModel() {
        val model = getModel()

        if (model == null) {
            presentImage.update(null, 0, 0, cropBackground)
            return
        }

        val image = model.staticBackground

        if (image == null) {
            presentImage.update(null, 0, 0, cropBackground)
            return
        }

        val w = image.width.roundToInt()
        val h = image.height.roundToInt()

        val buffer = IntBuffer.allocate(w * h)

        image.pixelReader.getPixels(0, 0, w, h, WritablePixelFormat.getIntArgbInstance(), buffer.array(), 0, w)
        presentImage.update(buffer.array(), w, h, cropBackground)
    }

    private fun update() {
        presentImage.render = render
    }
}