package dev.juhouse.projector.forms.controllers.projection

import dev.juhouse.projector.projection2.BridgeRender
import dev.juhouse.projector.projection2.BridgeRenderFlag
import dev.juhouse.projector.projection2.ProjectionManagerCallbacks
import javafx.geometry.Insets
import javafx.scene.control.CheckBox
import javafx.scene.layout.HBox

class RenderFlagBox: HBox(), ProjectionManagerCallbacks {
    var renderFlag: BridgeRenderFlag? = null

    private fun attachCheckBox(checkBox: CheckBox, renderId: Int) {
        checkBox.selectedProperty().value = renderFlag?.isRenderEnabled(renderId) ?: false

        checkBox.selectedProperty().addListener { _, _, newValue ->
            val active = renderFlag?.isRenderEnabled(renderId) ?: false

            if (newValue != active) {
                if (newValue) {
                    renderFlag?.enableRenderId(renderId)
                } else {
                    renderFlag?.disableRenderId(renderId)
                }
            }
        }
    }
    override fun onRebuild(renders: Array<out BridgeRender>) {
        children.clear()

        val renderCheckBoxes = renders.map {
            val checkBox = CheckBox()

            checkBox.text = "#" + it.renderId + " (" + it.renderName + ")"
            attachCheckBox(checkBox, it.renderId)

            setMargin(checkBox, Insets(0.0, 12.0, 0.0, 0.0))

            checkBox
        }

        children.addAll(renderCheckBoxes)
    }

}