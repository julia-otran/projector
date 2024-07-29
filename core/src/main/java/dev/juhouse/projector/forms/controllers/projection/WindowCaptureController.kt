package dev.juhouse.projector.forms.controllers.projection

import dev.juhouse.projector.projection2.ProjectionManager
import dev.juhouse.projector.projection2.ProjectionWindowCapture
import dev.juhouse.projector.utils.promise.JavaFxExecutor
import dev.juhouse.projector.utils.promise.Task
import javafx.fxml.FXML
import javafx.scene.control.CheckBox
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

    @FXML
    private lateinit var cropImageCheckBox: CheckBox

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
        controlBar.canProject = false

        windowListChoiceBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            controlBar.canProject = !newValue.isNullOrBlank()
            projectable.setWindowCaptureName(newValue)
        }

        cropImageCheckBox.selectedProperty().addListener { _, _, newValue ->
            projectable.setCrop(newValue)
        }

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
        controlBar.canProject = false
        windowListChoiceBox.selectionModel.clearSelection()
        windowListChoiceBox.items.clear()

        projectable.getWindowList().then(Task<List<String>, Void>{ list, _ ->
            windowListChoiceBox.items.addAll(list)
        }, JavaFxExecutor()).execute()

    }
}