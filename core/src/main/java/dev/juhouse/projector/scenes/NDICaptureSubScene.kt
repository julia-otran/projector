package dev.juhouse.projector.scenes

import dev.juhouse.projector.forms.controllers.projection.NDICaptureController
import javafx.beans.value.ObservableValue
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import java.io.IOException

class NDICaptureSubScene(root: Parent, width: Double, height: Double) : ProjectionItemSubScene(root, width, height) {

    companion object {
        @Throws(IOException::class)
        fun createScene(width: Double, height: Double): NDICaptureSubScene {
            val url = NDICaptureSubScene::class.java.classLoader.getResource("fxml/ndi_capture.fxml")
            val loader = FXMLLoader(url)
            val root = loader.load<Parent>()
            val scene = NDICaptureSubScene(root, width, height)
            scene.setController(loader.getController())
            scene.visibleProperty().set(false)
            scene.visibleProperty()
                .addListener { _: ObservableValue<out Boolean?>?, _: Boolean?, newVal: Boolean? ->
                    scene.controller.setVisible(newVal ?: false)
                }
            return scene
        }
    }

    override fun getController(): NDICaptureController {
        return super.getController() as NDICaptureController
    }

    private fun setController(controller: NDICaptureController) {
        super.setController(controller)
    }
}