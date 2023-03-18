package dev.juhouse.projector.forms.controllers.projection

import dev.juhouse.projector.projection2.ProjectionManager
import dev.juhouse.projector.projection2.ProjectionWindowCapture
import javafx.fxml.FXML
import javafx.scene.control.ChoiceBox
import javafx.scene.layout.Pane
import java.net.URL
import java.util.*

class WindowCaptureController : ProjectionController(), ProjectionBarControlCallbacks {
    @FXML
    private lateinit var projectionControlsPane: Pane

    private val controlBar = ProjectionBarControl()

    private lateinit var projectable: ProjectionWindowCapture

    private lateinit var manager: ProjectionManager

    @FXML
    private lateinit var windowListChoiceBox: ChoiceBox<String>

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
    }

    override fun initWithProjectionManager(projectionManager: ProjectionManager) {
        super.initWithProjectionManager(projectionManager)

        manager = projectionManager
        projectable = projectionManager.createWindowCapture()

        controlBar.projectable = projectable
        controlBar.callback = this
        controlBar.manager = projectionManager
        controlBar.attach(projectionControlsPane)

        onRefreshWindowList()
    }

    override fun onEscapeKeyPressed() {
        if (controlBar.projecting) {
            onProjectionEnd()
        }
    }

    override fun stop() {
        manager.stop(projectable)
    }

    override fun onProjectionBegin() {
        manager.setProjectable(projectable)
    }

    override fun onProjectionEnd() {
        manager.setProjectable(null)
    }

    @FXML
    fun onRefreshWindowList() {
        windowListChoiceBox.items.clear()
        windowListChoiceBox.items.addAll(projectable.getWindowList())
    }
}