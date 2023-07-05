package dev.juhouse.projector.projection2

import javafx.application.Platform
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import kotlin.collections.ArrayList

class MultiPreviewVBox(val delegate: CanvasDelegate): Runnable, VBox() {
    private var repainting = false
    private var running = false
    private var updateThread: Thread? = null
    private val previewPanes: ArrayList<PreviewPane> = ArrayList()

    override fun run() {
        while (running) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                running = false
                e.printStackTrace()
            }
            if (repainting) {
                continue
            }
            repainting = true

            previewPanes.forEach { it.downloadImage() }

            Platform.runLater {
                previewPanes.forEach { it.updateImage() }
                repainting = false
            }
        }
    }


    fun stop() {
        if (running) {
            running = false
            try {
                updateThread?.join()
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }
    }

    fun start() {
        Platform.runLater {
            previewPanes.clear()
            children.clear()

            var renderSettings = delegate.bridge.renderSettings

            listOf(*renderSettings).forEach {
                val pane = PreviewPane(delegate)
                pane.setBridgeRender(it)

                pane.minWidth = 0.0
                pane.minHeight = 0.0

                pane.prefWidthProperty().bind(widthProperty())
                pane.prefHeightProperty().bind(heightProperty().divide(renderSettings.size))

                pane.maxWidthProperty().bind(widthProperty())
                pane.maxHeightProperty().bind(heightProperty().divide(renderSettings.size))

                setVgrow(pane, Priority.SOMETIMES)

                previewPanes.add(pane)
                children.add(pane)
            }

            if (!running) {
                running = true
                updateThread = Thread(this)
                updateThread?.start()
            }
        }
    }
}