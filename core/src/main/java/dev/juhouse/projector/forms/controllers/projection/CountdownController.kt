package dev.juhouse.projector.forms.controllers.projection

import dev.juhouse.projector.projection2.ProjectionManager
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.control.ToggleButton
import java.net.URL
import java.util.*
import dev.juhouse.projector.projection2.countdown.ProjectionCountdown
import javafx.application.Platform

class CountdownController: ProjectionController(), Runnable {
    @FXML
    private lateinit var beginProjectionButton: Button

    @FXML
    private lateinit var endProjectionButton: Button

    @FXML
    private lateinit var countdownTextField: TextField

    @FXML
    private lateinit var countdownRun: ToggleButton

    @FXML
    private lateinit var countdownPause: ToggleButton

    private var countdownExecute = false

    private var thread: Thread? = null

    private var intervalMs: Long = 0
    private var endsAt: Long = 0

    private lateinit var projectionCountdown: ProjectionCountdown

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        endProjectionButton.disableProperty().value = true
        countdownPause.selectedProperty().value = false
        countdownRun.selectedProperty().value = false
    }

    override fun initWithProjectionManager(projectionManager: ProjectionManager?) {
        super.initWithProjectionManager(projectionManager)

        projectionCountdown = getProjectionManager().createCountdown()

        getProjectionManager().projectableProperty().addListener { _, _, newValue ->
            if (newValue == projectionCountdown) {
                endProjectionButton.disableProperty().value = false
                beginProjectionButton.disableProperty().value = true
            } else {
                endProjectionButton.disableProperty().value = true
                beginProjectionButton.disableProperty().value = false
            }
        }

        intervalMs = observer.getProperty("INTERVAL_MS").map { i -> i.toLong() }.orElse(5 * 60 * 1000L)
        countdownTextField.text = formatInterval(intervalMs)

        countdownTextField.textProperty().addListener { _, _, _ ->
            if (!countdownTextField.isDisable) {
                observer.updateProperty("INTERVAL_MS", getIntervalMilis().toString())
            }
        }
    }

    override fun onEscapeKeyPressed() {
        onEndProjection()
    }

    override fun stop() {
        onCountdownPause()
        getProjectionManager().stop(projectionCountdown)

        if (!countdownTextField.disableProperty().value) {
            intervalMs = getIntervalMilis()
        }
    }

    @FXML
    fun onBeginProjection() {
        onCountdownRun()
        getProjectionManager().setProjectable(projectionCountdown)
    }

    @FXML
    fun onEndProjection() {
        getProjectionManager().setProjectable(null)
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

            val formattedText = formatInterval(intervalMs)
            countdownTextField.text = formattedText
        }
    }

    override fun run() {
        while (countdownExecute) {
            Thread.sleep(200)

            val remainingTime = (endsAt - System.currentTimeMillis()).coerceAtLeast(0)

            val formattedText = formatInterval(remainingTime)

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
        val textSplit = countdownTextField.text.split(':').reversed()

        val secs = textSplit[0].ifEmpty { "0" }.toInt()
        val mins = textSplit.getOrNull(1)?.ifEmpty { "0" }?.toInt() ?: 0
        val hours = textSplit.getOrNull(2)?.ifEmpty { "0" }?.toInt() ?: 0

        return secs * 1000L + mins * 60 * 1000L + hours * 60 * 60 * 1000L
    }

    private fun formatInterval(ms: Long): String {
        val secs = (ms % (60 * 1000)) / 1000
        val mins = (ms % (60 * 60 * 1000)) / (60 * 1000)
        val hours = ms / (60 * 60 * 1000)

        return String.format("%02d:%02d:%02d", hours, mins, secs)
    }
}