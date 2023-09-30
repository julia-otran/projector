package dev.juhouse.projector.forms.controllers.projection

import dev.juhouse.projector.projection2.BridgeRender
import dev.juhouse.projector.projection2.BridgeRenderFlag
import dev.juhouse.projector.projection2.ProjectionManagerCallbacks
import javafx.beans.value.ChangeListener
import javafx.geometry.Insets
import javafx.scene.control.CheckBox
import javafx.scene.layout.HBox

class RenderFlagBox: HBox(), ProjectionManagerCallbacks {
    private val checkBoxesMap = HashMap<Int, CheckBox>()

    private val renderFlagListener: ChangeListener<in Number> = ChangeListener { _, _, _ ->
        checkBoxesMap.forEach {
            it.value.selectedProperty().value = renderFlag?.isRenderEnabled(it.key) ?: false
        }
    }

    var renderFlag: BridgeRenderFlag? = null
        set(value) {
            field?.property?.removeListener(renderFlagListener)
            field = value
            value?.property?.addListener(renderFlagListener)
        }

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
        checkBoxesMap.clear()

        val renderCheckBoxes = renders.map {
            val checkBox = CheckBox()

            checkBox.text = "#" + it.renderId + " (" + it.renderName + ")"
            attachCheckBox(checkBox, it.renderId)

            setMargin(checkBox, Insets(0.0, 12.0, 0.0, 0.0))

            checkBoxesMap[it.renderId] = checkBox

            checkBox
        }

        children.addAll(renderCheckBoxes)
    }

}