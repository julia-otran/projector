package dev.juhouse.projector.scenes

import dev.juhouse.projector.forms.controllers.projection.SolidColorController
import javafx.beans.value.ObservableValue
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import java.io.IOException

class SolidColorSubScene(root: Parent, width: Double, height: Double) : ProjectionItemSubScene(root, width, height) {

    companion object {
        @Throws(IOException::class)
        fun createScene(width: Double, height: Double): SolidColorSubScene {
            val url = SolidColorSubScene::class.java.classLoader.getResource("fxml/solid_color_form.fxml")
            val loader = FXMLLoader(url)
            val root = loader.load<Parent>()
            val scene = SolidColorSubScene(root, width, height)

            scene.setController(loader.getController())
            scene.visibleProperty().set(false)

            scene.visibleProperty()
                .addListener { _: ObservableValue<out Boolean?>?, _: Boolean?, newVal: Boolean? ->
                    scene.controller.setVisible(newVal ?: false)
                }

            return scene
        }
    }

    override fun getController(): SolidColorController {
        return super.getController() as SolidColorController
    }

    private fun setController(controller: SolidColorController) {
        super.setController(controller)
    }
}
