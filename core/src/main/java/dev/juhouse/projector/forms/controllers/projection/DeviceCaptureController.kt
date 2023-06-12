package dev.juhouse.projector.forms.controllers.projection

import dev.juhouse.projector.projection2.ProjectionManager
import dev.juhouse.projector.projection2.video.ProjectionVideoCapture
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.*
import java.net.URL
import java.util.*

class DeviceCaptureController: ProjectionController(),
    ProjectionBarControlCallbacks {

    /**
     * Initializes the controller class.
     */
    override fun initialize(url: URL?, rb: ResourceBundle?) {}

    private lateinit var projectionVideoCapture: ProjectionVideoCapture

    @FXML
    private lateinit var playerBox: VBox

    @FXML
    private lateinit var devicesComboBox: ComboBox<String>

    // Controls
    @FXML
    private lateinit var projectionControlPane: Pane

    private val controlBar = ProjectionBarControl()

    @FXML
    private lateinit var playerContainer: BorderPane

    @FXML
    private lateinit var fullScreenCheckBox: CheckBox

    override fun onProjectionBegin() {
        getProjectionManager().setProjectable(projectionVideoCapture)
    }

    override fun onProjectionEnd() {
        getProjectionManager().setProjectable(null)
    }

    override fun initWithProjectionManager(projectionManager: ProjectionManager) {
        super.initWithProjectionManager(projectionManager)

        try {
            projectionVideoCapture = projectionManager.createVideoCapture()
        } catch (ex: Exception) {
            ex.printStackTrace()
            // TODO: Show Error State
            return
        }

        playerContainer.center = projectionVideoCapture.previewPanel
        playerContainer.maxWidthProperty().bind(playerBox.widthProperty())
        projectionVideoCapture.previewPanel.maxWidthProperty().bind(playerContainer.widthProperty().subtract(1))
        projectionVideoCapture.previewPanel.prefWidthProperty().bind(playerContainer.widthProperty().subtract(1))
        projectionVideoCapture.previewPanel.maxHeightProperty().bind(playerContainer.heightProperty().subtract(1))
        projectionVideoCapture.previewPanel.prefHeightProperty().bind(playerContainer.heightProperty().subtract(1))

        fullScreenCheckBox.isSelected = projectionVideoCapture.isCropVideo
        controlBar.projectable = projectionVideoCapture
        controlBar.callback = this
        controlBar.manager = projectionManager
        controlBar.attach(projectionControlPane)

        devicesComboBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (newValue.isNullOrBlank()) {
                projectionVideoCapture.player.controls().stop()
            } else {
                projectionVideoCapture.player.media().play(newValue)
            }
        }

        onRefreshDevices()
    }

    override fun onEscapeKeyPressed() {
        if (controlBar.projecting) {
            onProjectionEnd()
        }
    }

    override fun stop() {
        onEscapeKeyPressed()
        projectionVideoCapture.player.controls().stop()
        projectionManager.stop(projectionVideoCapture)
        projectionVideoCapture.previewPanel.stopPreview()
    }
    @FXML
    fun onFullScreenAction() {
        projectionVideoCapture.isCropVideo = fullScreenCheckBox.isSelected
    }

    @FXML
    fun onRefreshDevices() {
        devicesComboBox.selectionModel.clearSelection()
        devicesComboBox.items.clear()
        devicesComboBox.items.addAll(projectionVideoCapture.getDevices())
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)
        if (visible) {
            projectionVideoCapture.previewPanel.startPreview()
        } else {
            projectionVideoCapture.previewPanel.stopPreview()
        }
    }
}