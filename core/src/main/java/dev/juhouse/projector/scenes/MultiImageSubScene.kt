package dev.juhouse.projector.scenes

import dev.juhouse.projector.forms.controllers.projection.MultiImageController
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import java.io.IOException

class MultiImageSubScene(root: Parent?, width: Double, height: Double) : ProjectionItemSubScene(root, width, height) {
        companion object {
        @Throws(IOException::class)
        fun createMultiImageScene(
            width: Double,
            height: Double
        ): ProjectionItemSubScene {
            val url = MultiImageSubScene::class.java.classLoader.getResource("fxml/multi_image.fxml")
            val loader = FXMLLoader(url)

            val root = loader.load<Parent>()

            val scene = MultiImageSubScene(root, width, height)

            val ctrl = loader.getController<MultiImageController>()

            scene.controller = ctrl

            return scene
        }
    }
}