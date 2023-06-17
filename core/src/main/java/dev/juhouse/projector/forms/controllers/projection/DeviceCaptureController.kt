package dev.juhouse.projector.forms.controllers.projection

import dev.juhouse.projector.other.OsCheck
import dev.juhouse.projector.other.OsCheck.OSType
import dev.juhouse.projector.projection2.BridgeCaptureDevice
import dev.juhouse.projector.projection2.BridgeCaptureDeviceResolution
import dev.juhouse.projector.projection2.ProjectionManager
import dev.juhouse.projector.projection2.video.ProjectionVideoCapture
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.*
import java.net.URL
import java.util.*
import java.util.regex.Pattern

const val WINDOWS_MEDIA_URL_PATTERN = "dshow:// :dshow-vdev=%s :dshow-adev= :dshow-size=%s"
const val LINUX_MEDIA_URL_PATTERN = "v4l2://%s"

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

        fullScreenCheckBox.isSelected = projectionVideoCapture.isCropVideo
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

    private fun getMediaOptionsWindows(device: String, resolution: String): Array<String> {
        return arrayOf(
            "dshow-vdev=" + device.replace(":", "\\:"),
            "dshow-adev=none",
            "dshow-size=$resolution"
        )
    }

    private fun activateReproduction() {
        val deviceName = devicesComboBox.selectionModel.selectedItem
        val resolution = resolutionsComboBox.selectionModel.selectedItem.toString()
        var mediaUrl = ""
        var options: Array<String> = arrayOf()

        if (OsCheck.getOperatingSystemType() == OSType.Windows) {
            mediaUrl = "dshow://"
            options = getMediaOptionsWindows(deviceName, resolution)
        }

        if (OsCheck.getOperatingSystemType() == OSType.Linux) {
            mediaUrl = String.format(LINUX_MEDIA_URL_PATTERN, deviceName)
        }

        if (mediaUrl.isNotBlank()) {
            projectionVideoCapture.player.media().play(mediaUrl, *options)
        }
    }

    @FXML
    fun onFullScreenAction() {
        projectionVideoCapture.isCropVideo = fullScreenCheckBox.isSelected
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
                projectionVideoCapture.player.controls().stop()
            } else if (resolutionsComboBox.selectionModel.selectedItem == null) {
                activateCheckBox.isSelected = false
                projectionVideoCapture.player.controls().stop()
            } else {
                activateReproduction()
            }
        } else {
            projectionVideoCapture.player.controls().stop()
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