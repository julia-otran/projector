package dev.juhouse.projector.forms.controllers.projection

import dev.juhouse.projector.projection2.BridgeRenderFlag
import dev.juhouse.projector.projection2.ProjectionManager
import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.TransferMode
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import java.io.File

class SingleImageControl: BorderPane() {
    interface Callbacks {
        fun onImageChanged(singleImageControl: SingleImageControl)
        fun onRenderingChanged()
    }

    private val addImageLabel = Label()
    private val imageView = ImageView()
    private val renderFlagBox = RenderFlagBox()

    private var addingFile: File? = null
    private var addingImage: Image? = null
    var currentFile: File? = null
    var currentImage: Image? = null

    var callback: Callbacks? = null
    var renderFlag: BridgeRenderFlag? = null
    var defaultBorder: Border? = null
        set(value) {
            field = value
            border = value
        }

    var manager: ProjectionManager? = null
        set(value) {
            field?.removeCallback(renderFlagBox)
            field = value

            if (renderFlagBox.renderFlag == null) {
                renderFlag = value?.createRenderFlag()

                renderFlag?.property?.addListener { _, _, _ -> callback?.onRenderingChanged() }

                renderFlagBox.renderFlag = renderFlag
            }

            value?.addCallback(renderFlagBox)
        }

    init {
        addImageLabel.text = "Arraste aqui uma imagem"
        addImageLabel.textAlignment = TextAlignment.CENTER
        setMargin(addImageLabel, Insets(8.0))
        setMargin(renderFlagBox, Insets(8.0))
        setMargin(imageView, Insets(0.0, 8.0, 8.0, 8.0))

        top = addImageLabel
        center = imageView

        imageView.fitWidth = 177.0
        imageView.fitHeight = 100.0

        imageView.isPreserveRatio = true

        setOnDragOver {
            top = addImageLabel

            if (it.dragboard.hasFiles()) {
                val files = it.dragboard.files

                if (files.size == 1) {
                    val file = files[0]

                    if (file.toString() == addingFile?.toString()) {
                        it.acceptTransferModes(TransferMode.LINK)
                        return@setOnDragOver
                    }

                    val image = Image(file.toURI().toURL().toExternalForm())

                    if (image.isError) {
                        border = Border(BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT))
                        addImageLabel.text = "Falha ao carregar imagem"
                    } else {
                        border = Border(BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT))
                        addImageLabel.text = "Solte na área demarcada"
                        addingFile = file
                        addingImage = image
                        imageView.image = image
                        it.acceptTransferModes(TransferMode.LINK)
                    }
                } else {
                    border = Border(BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT))
                    addImageLabel.text = "Insira apenas 1 arquivo"
                }
            } else {
                border = Border(BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT))
                addImageLabel.text = "Não é um arquivo"
            }

            it.consume()
        }

        setOnDragExited {
            border = defaultBorder
            addingImage = null
            addingFile = null

            imageView.image = currentImage

            if (currentImage != null) {
                top = renderFlagBox
            } else {
                addImageLabel.text = "Arraste aqui uma imagem"
            }

            it.consume()
        }

        setOnDragDropped {
            border = defaultBorder

            val file = addingFile
            val image = addingImage

            if (file == null || image == null) {
                return@setOnDragDropped
            }

            top = renderFlagBox
            currentImage = image
            currentFile = file

            imageView.image = image
            callback?.onImageChanged(this)
            callback?.onRenderingChanged()

            addingImage = null
            addingFile = null

            it.consume()
        }
    }

    fun loadImage(file: File): Boolean {
        val image = Image(file.toURI().toURL().toExternalForm())

        if (image.isError) {
            return false
        }

        imageView.image = image
        currentImage = image
        currentFile = file
        top = renderFlagBox

        callback?.onRenderingChanged()

        return true
    }


}