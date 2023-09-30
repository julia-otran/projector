package dev.juhouse.projector.forms.controllers

import dev.juhouse.projector.forms.controllers.projection.RenderFlagBox
import dev.juhouse.projector.projection2.ProjectionManager
import dev.juhouse.projector.projection2.time.ProjectionClock
import dev.juhouse.projector.utils.TimeFormatUtils
import javafx.scene.control.Label
import javafx.scene.layout.Pane
import javafx.application.Platform
import java.util.*

class WorkspaceClockController(val projectionManager: ProjectionManager, private val clockLabel: Label, val clockControlBarPane: Pane): Runnable {
    private lateinit var projectable: ProjectionClock
    private var run: Boolean = false
    private val renderFlagBox = RenderFlagBox()
    private lateinit var timeUpdateThread: Thread

    fun start() {
        timeUpdateThread = Thread(this)
        run = true
        projectable = projectionManager.createClock()
        projectionManager.addCallback(renderFlagBox)
        renderFlagBox.renderFlag = projectable.renderFlag
        clockControlBarPane.children.add(renderFlagBox)
        timeUpdateThread.start()

        projectionManager.concurrentProjectableProperty().addListener { _, _, newProjectable ->
            if (newProjectable != projectable) {
                projectable.renderFlag.renderToNone()
            }
        }

        projectable.renderFlag.flagValueProperty.addListener { _ ->
            if (projectable.renderFlag.hasAnyRender()) {
                projectionManager.setConcurrentProjectable(projectable)
            } else {
                if (projectionManager.concurrentProjectableProperty().value == projectable) {
                    projectionManager.setConcurrentProjectable(null)
                }
            }
        }

    }

    fun stop() {
        run = false
        projectionManager.removeCallback(renderFlagBox)
        timeUpdateThread.interrupt()
        timeUpdateThread.join()
    }

    override fun run() {
        var currentTime: String? = null

        while (run) {
            val calendar = Calendar.getInstance(TimeZone.getDefault())

            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val second = calendar.get(Calendar.SECOND)

            val text = TimeFormatUtils.formatNumbersToTime(hour, minute, second)

            if (currentTime == text) {
                try {
                    Thread.sleep(100)
                    continue
                } catch (e: InterruptedException) {
                    break
                }
            }

            currentTime = text

            Platform.runLater {
                clockLabel.text = text
            }

            projectable.setText(text)


        }
    }
}
