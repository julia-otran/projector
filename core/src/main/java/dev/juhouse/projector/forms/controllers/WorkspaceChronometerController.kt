package dev.juhouse.projector.forms.controllers

import dev.juhouse.projector.forms.controllers.projection.RenderFlagBox
import dev.juhouse.projector.projection2.ProjectionManager
import dev.juhouse.projector.projection2.time.ProjectionClock
import dev.juhouse.projector.utils.TimeFormatUtils
import javafx.scene.layout.Pane
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.control.TextField

class WorkspaceChronometerController(val projectionManager: ProjectionManager, private val countdownTextField: TextField, private val countdownButton: Button, private val controlBarPane: Pane): Runnable {
    private lateinit var projectable: ProjectionClock
    private var run: Boolean = false
    private val renderFlagBox = RenderFlagBox()
    private var timeUpdateThread: Thread? = null

    private var countdownEndMs: Long = 0

    private fun beginCountdown() {
        countdownTextField.disableProperty().set(true)
        countdownEndMs = System.currentTimeMillis() + TimeFormatUtils.formatTimeStringToMs(countdownTextField.text)

        run = true
        timeUpdateThread = Thread(this)
        timeUpdateThread?.start()

        countdownButton.text = "⏸"
    }

    fun start() {
        projectable = projectionManager.createClock()
        projectionManager.addCallback(renderFlagBox)

        renderFlagBox.renderFlag = projectable.renderFlag

        controlBarPane.children.add(renderFlagBox)

        countdownButton.onAction = EventHandler { _ ->
            if (run) {
                run = false
                timeUpdateThread?.join()
                timeUpdateThread = null
                countdownTextField.disableProperty().set(false)
                countdownButton.text = "▶"
            } else {
                beginCountdown()
            }
        }

        projectionManager.concurrentProjectableProperty().addListener { _, _, newProjectable ->
            if (newProjectable != projectable) {
                projectable.renderFlag.renderToNone()
            }
        }

        projectable.renderFlag.flagValueProperty.addListener { _ ->
            if (projectable.renderFlag.hasAnyRender()) {
                projectionManager.setConcurrentProjectable(projectable)

                if (!run) {
                    beginCountdown()
                }
            }

            if (!projectable.renderFlag.hasAnyRender()) {
                if (projectionManager.concurrentProjectableProperty().value == projectable) {
                    projectionManager.setConcurrentProjectable(null)
                }
            }
        }
    }

    fun stop() {
        run = false
        projectionManager.removeCallback(renderFlagBox)
        timeUpdateThread?.join()
    }

    override fun run() {
        while (run) {
            val remaining = countdownEndMs - System.currentTimeMillis()
            val remainingText = TimeFormatUtils.formatMsToTime(remaining)

            Platform.runLater {
                countdownTextField.text = remainingText
            }

            projectable.setText(remainingText)

            Thread.sleep(300)
        }
    }
}