package dev.juhouse.projector.scenes

import dev.juhouse.projector.forms.controllers.projection.DeviceCaptureController
import javafx.beans.value.ObservableValue
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import java.io.IOException

class DeviceCaptureSubScene(root: Parent, width: Double, height: Double) : ProjectionItemSubScene(root, width, height) {

    companion object {
        @Throws(IOException::class)
        fun createScene(width: Double, height: Double): DeviceCaptureSubScene {
            val url = DeviceCaptureSubScene::class.java.classLoader.getResource("fxml/device_capture.fxml")
            val loader = FXMLLoader(url)
            val root = loader.load<Parent>()
            val scene = DeviceCaptureSubScene(root, width, height)
            scene.setController(loader.getController())
            scene.visibleProperty().set(false)
            scene.visibleProperty()
                .addListener { _: ObservableValue<out Boolean?>?, _: Boolean?, newVal: Boolean? ->
                    scene.controller.setVisible(newVal ?: false)
                }
            return scene
        }
    }

    override fun getController(): DeviceCaptureController {
        return super.getController() as DeviceCaptureController
    }

    private fun setController(controller: DeviceCaptureController) {
        super.setController(controller)
    }
}