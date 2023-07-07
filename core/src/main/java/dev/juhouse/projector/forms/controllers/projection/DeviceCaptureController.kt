package dev.juhouse.projector.forms.controllers.projection

import dev.juhouse.projector.projection2.BridgeCaptureDevice
import dev.juhouse.projector.projection2.BridgeCaptureDeviceResolution
import dev.juhouse.projector.projection2.ProjectionManager
import dev.juhouse.projector.projection2.ProjectionVideoCapture
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

    @FXML
    private lateinit var resolutionsComboBox: ComboBox<BridgeCaptureDeviceResolution>

    @FXML
    private lateinit var activateCheckBox: CheckBox

    // Controls
    @FXML
    private lateinit var projectionControlPane: Pane

    private val controlBar = ProjectionBarControl()

    @FXML
    private lateinit var playerContainer: BorderPane

    @FXML
    private lateinit var fullScreenCheckBox: CheckBox

    private var devices: List<BridgeCaptureDevice> = Collections.emptyList()

    override fun onProjectionBegin() {
        getProjectionManager().setProjectable(projectionVideoCapture)
    }

    override fun onProjectionEnd() {
        getProjectionManager().setProjectable(null)
    }

    private fun getRecommendedResolution(): BridgeCaptureDeviceResolution? {
        return resolutionsComboBox.items
            .filter { it.width <= 1920 }
            .filter { it.height <= 1080 }
            .maxByOrNull { it.width * it.height }
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

        fullScreenCheckBox.isSelected = projectionVideoCapture.cropVideo
        controlBar.projectable = projectionVideoCapture
        controlBar.callback = this
        controlBar.manager = projectionManager
        controlBar.attach(projectionControlPane)

        devicesComboBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            activateCheckBox.isSelected = false
            resolutionsComboBox.selectionModel.clearSelection()
            resolutionsComboBox.items.clear()
            devices.find { it.deviceName == newValue }?.let {
                resolutionsComboBox.items.addAll(it.resolutions)
                getRecommendedResolution()?.let { r -> resolutionsComboBox.selectionModel.select(r) }
            }
        }

        resolutionsComboBox.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            activateCheckBox.isSelected = false
        }

        onRefreshDevices()

        val deviceName = observer.getProperty("VIDEO_DEVICE")
        val deviceWidth = observer.getProperty("VIDEO_DEVICE_WIDTH").map { it.toInt() }.orElse(null)
        val deviceHeight = observer.getProperty("VIDEO_DEVICE_HEIGHT").map { it.toInt() }.orElse(null)

        deviceName.ifPresent {
            if (devicesComboBox.items.contains(it)) {
                devicesComboBox.selectionModel.select(it)

                val resolution = resolutionsComboBox.items.find { r -> r.width == deviceWidth && r.height == deviceHeight }

                resolution?.let {
                    resolutionsComboBox.selectionModel.select(resolution)
                }
            }
        }
    }

    override fun onEscapeKeyPressed() {
        if (controlBar.projecting) {
            onProjectionEnd()
        }
    }

    override fun stop() {
        onEscapeKeyPressed()
        projectionVideoCapture.enabled = false
        projectionManager.stop(projectionVideoCapture)
        projectionVideoCapture.previewPanel.stopPreview()
    }

    private fun activateReproduction() {
        val deviceName = devicesComboBox.selectionModel.selectedItem
        val resolution = resolutionsComboBox.selectionModel.selectedItem

        observer.updateProperty("VIDEO_DEVICE", deviceName)
        observer.updateProperty("VIDEO_DEVICE_WIDTH", resolution.width.toString())
        observer.updateProperty("VIDEO_DEVICE_HEIGHT", resolution.height.toString())

        projectionVideoCapture.setDevice(deviceName, resolution.width, resolution.height)
        projectionVideoCapture.enabled = true
    }

    @FXML
    fun onFullScreenAction() {
        projectionVideoCapture.cropVideo = fullScreenCheckBox.isSelected
    }

    @FXML
    fun onRefreshDevices() {
        activateCheckBox.isSelected = false

        devicesComboBox.selectionModel.clearSelection()
        resolutionsComboBox.selectionModel.clearSelection()

        devicesComboBox.items.clear()
        resolutionsComboBox.items.clear()

        devices = projectionVideoCapture.getDevices()

        devicesComboBox.items.addAll(devices.map { it.deviceName })
    }

    @FXML
    fun onChangeActivate() {
        if (activateCheckBox.isSelected) {
            if (devicesComboBox.selectionModel.selectedItem.isNullOrBlank()) {
                activateCheckBox.isSelected = false
                projectionVideoCapture.enabled = false
            } else if (resolutionsComboBox.selectionModel.selectedItem == null) {
                activateCheckBox.isSelected = false
                projectionVideoCapture.enabled = false
            } else {
                activateReproduction()
            }
        } else {
            projectionVideoCapture.enabled = false
        }
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