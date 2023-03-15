package dev.juhouse.projector.scenes

import dev.juhouse.projector.forms.controllers.AddMusicCallback
import dev.juhouse.projector.forms.controllers.MusicListController
import dev.juhouse.projector.forms.controllers.projection.CountdownController
import dev.juhouse.projector.forms.controllers.projection.ImageController
import dev.juhouse.projector.services.ManageMusicService
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.stage.Stage
import java.io.IOException

class TimerSubScene(root: Parent?, width: Double, height: Double) : ProjectionItemSubScene(root, width, height) {
    companion object {
        @Throws(IOException::class)
        fun createTimerScene(
            width: Double,
            height: Double
        ): ProjectionItemSubScene {
            val url = TimerSubScene::class.java.classLoader.getResource("fxml/countdown.fxml")
            val loader = FXMLLoader(url)

            val root = loader.load<Parent>()

            val scene = TimerSubScene(root, width, height)

            val ctrl = loader.getController<CountdownController>()

            scene.controller = ctrl

            return scene
        }
    }
}