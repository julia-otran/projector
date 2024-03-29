package dev.juhouse.projector.forms.controllers.projection

import dev.juhouse.projector.projection2.ProjectionManager
import javafx.fxml.FXML
import javafx.scene.control.TextField
import javafx.scene.control.ToggleButton
import java.net.URL
import java.util.*
import dev.juhouse.projector.projection2.time.ProjectionCountdown
import dev.juhouse.projector.utils.TimeFormatUtils
import dev.juhouse.projector.utils.TimeFormatUtils.Companion.formatMsToTime
import javafx.application.Platform
import javafx.scene.layout.Pane

class CountdownController: ProjectionController(), Runnable, ProjectionBarControlCallbacks {
    @FXML
    private lateinit var countdownTextField: TextField

    @FXML
    private lateinit var countdownRun: ToggleButton

    @FXML
    private lateinit var countdownPause: ToggleButton

    @FXML
    private lateinit var projectionControls: Pane

    private val controlBar = ProjectionBarControl()

    private var countdownExecute = false

    private var thread: Thread? = null

    private var intervalMs: Long = 0
    private var endsAt: Long = 0

    private lateinit var projectionCountdown: ProjectionCountdown

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        countdownPause.selectedProperty().value = false
        countdownRun.selectedProperty().value = false
    }

    override fun initWithProjectionManager(projectionManager: ProjectionManager?) {
        super.initWithProjectionManager(projectionManager)

        projectionCountdown = getProjectionManager().createCountdown()

        intervalMs = observer.getProperty("INTERVAL_MS").map { i -> i.toLong() }.orElse(5 * 60 * 1000L)
        countdownTextField.text = formatMsToTime(intervalMs)

        countdownTextField.textProperty().addListener { _, _, _ ->
            if (!countdownTextField.isDisable) {
                observer.updateProperty("INTERVAL_MS", getIntervalMilis().toString())
            }
        }

        controlBar.projectable = projectionCountdown
        controlBar.callback = this
        controlBar.manager = projectionManager
        controlBar.attach(projectionControls)
    }

    override fun onEscapeKeyPressed() {
        onProjectionEnd()
    }

    override fun stop() {
        onCountdownPause()
        getProjectionManager().stop(projectionCountdown)

        if (!countdownTextField.disableProperty().value) {
            intervalMs = getIntervalMilis()
        }
    }

    @FXML
    fun onCountdownRun() {
        countdownRun.selectedProperty().value = true

        if (!countdownTextField.disableProperty().value) {
            intervalMs = getIntervalMilis()
            countdownTextField.disableProperty().value = true
        }

        endsAt = System.currentTimeMillis() + getIntervalMilis()

        countdownExecute = true

        thread = Thread(this)
        thread?.start()

    }

    @FXML
    fun onCountdownPause() {
        if (countdownExecute) {
            countdownPause.selectedProperty().value = true
            countdownExecute = false
            thread?.join()
        } else {
            countdownRun.selectedProperty().value = false
            countdownPause.selectedProperty().value = false
            countdownTextField.disableProperty().value = false

            val formattedText = formatMsToTime(intervalMs)
            countdownTextField.text = formattedText
        }
    }

    override fun run() {
        while (countdownExecute) {
            Thread.sleep(200)

            val remainingTime = (endsAt - System.currentTimeMillis()).coerceAtLeast(0)

            val formattedText = formatMsToTime(remainingTime)

            Platform.runLater {
                projectionCountdown.setText(formattedText)
                countdownTextField.text = formattedText
            }

            if (remainingTime <= 0) {
                countdownExecute = false

                Platform.runLater {
                    onCountdownPause()
                }
            }
        }
    }
    private fun getIntervalMilis(): Long {
        return TimeFormatUtils.formatTimeStringToMs(countdownTextField.text)
    }

    override fun onProjectionBegin() {
        onCountdownRun()
        getProjectionManager().setProjectable(projectionCountdown)
    }

    override fun onProjectionEnd() {
        getProjectionManager().setProjectable(null)
    }
}