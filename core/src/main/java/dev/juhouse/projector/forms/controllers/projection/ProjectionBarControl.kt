package dev.juhouse.projector.forms.controllers.projection

import dev.juhouse.projector.projection2.Projectable
import dev.juhouse.projector.projection2.ProjectionManager
import javafx.beans.value.ChangeListener
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

interface ProjectionBarControlCallbacks {
    fun onProjectionBegin()
    fun onProjectionEnd()
}

class ProjectionBarControl: VBox() {
    private val mainControlsBox = HBox()
    private val beginProjectionButton = Button()
    private val endProjectionButton = Button()

    private val renderFlagsBox = RenderFlagBox()

    private val projectablePropertyListener =  ChangeListener<Projectable> { _, _, newProjectable ->
        if (newProjectable == projectable) {
            beginProjectionButton.disableProperty().value = true
            endProjectionButton.disableProperty().value = false
        } else {
            beginProjectionButton.disableProperty().value = false
            endProjectionButton.disableProperty().value = true
        }
    }

    var projectable: Projectable? = null
        set(value) {
            field = value
            renderFlagsBox.renderFlag = value?.renderFlagProperty?.get()
        }

    var callback: ProjectionBarControlCallbacks? = null

    var manager: ProjectionManager? = null
        set(value) {
            field?.removeCallback(renderFlagsBox)
            field?.projectableProperty()?.removeListener(projectablePropertyListener)

            field = value

            value?.projectableProperty()?.addListener(projectablePropertyListener)
            value?.addCallback(renderFlagsBox)
        }

    var canProject: Boolean = true
        set(value) {
            field = value

            beginProjectionButton.disableProperty().value = !value
        }

    val projecting: Boolean
        get() {
            return manager?.projectableProperty()?.value == projectable
        }

    init {
        fillWidthProperty().value = true

        children.add(mainControlsBox)

        beginProjectionButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE)
        endProjectionButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE)

        endProjectionButton.disableProperty().value = true

        HBox.setMargin(beginProjectionButton, Insets(0.0, 8.0, 0.0, 0.0))
        HBox.setHgrow(beginProjectionButton, Priority.ALWAYS)
        HBox.setHgrow(endProjectionButton, Priority.ALWAYS)

        mainControlsBox.children.add(beginProjectionButton)
        mainControlsBox.children.add(endProjectionButton)

        beginProjectionButton.text = "Projetar na Tela"
        endProjectionButton.text = "Remover da Tela (ESC)"

        beginProjectionButton.onAction = EventHandler {
            callback?.onProjectionBegin()
        }

        endProjectionButton.onAction = EventHandler {
            callback?.onProjectionEnd()
        }

        setMargin(renderFlagsBox, Insets(8.0, 0.0, 0.0, 0.0))
        children.add(renderFlagsBox)
    }

    fun attach(pane: Pane) {
        pane.children.add(this)
        this.prefWidthProperty().bind(pane.widthProperty())
    }

    fun stop() {
        manager?.removeCallback(renderFlagsBox)
    }
}