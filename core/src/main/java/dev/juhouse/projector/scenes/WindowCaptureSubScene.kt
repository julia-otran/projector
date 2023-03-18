package dev.juhouse.projector.scenes

import dev.juhouse.projector.forms.controllers.projection.WindowCaptureController
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import java.io.IOException

class WindowCaptureSubScene(root: Parent?, width: Double, height: Double) : ProjectionItemSubScene(root, width, height) {
        companion object {
        @Throws(IOException::class)
        fun createWindowCaptureScene(
            width: Double,
            height: Double
        ): ProjectionItemSubScene {
            val url = TimerSubScene::class.java.classLoader.getResource("fxml/window_capture.fxml")
            val loader = FXMLLoader(url)

            val root = loader.load<Parent>()

            val scene = TimerSubScene(root, width, height)

            val ctrl = loader.getController<WindowCaptureController>()

            scene.controller = ctrl

            return scene
        }
    }
}
