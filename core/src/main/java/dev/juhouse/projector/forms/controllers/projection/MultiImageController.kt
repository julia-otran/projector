package dev.juhouse.projector.forms.controllers.projection

import dev.juhouse.projector.projection2.ProjectionManager
import dev.juhouse.projector.projection2.image.ProjectionMultiImage
import javafx.fxml.FXML
import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.image.Image
import javafx.scene.layout.*
import javafx.scene.paint.Color
import java.io.File
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MultiImageController: ProjectionController(), SingleImageControl.Callbacks {
    @FXML
    private lateinit var beginProjectionButton: Button

    @FXML
    private lateinit var endProjectionButton: Button

    @FXML
    private lateinit var imagesVBox: VBox

    private lateinit var multiImage: ProjectionMultiImage

    private val imageControls = ArrayList<SingleImageControl>()

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        beginProjectionButton.isDisable = true
        endProjectionButton.isDisable = true
    }

    override fun initWithProjectionManager(projectionManager: ProjectionManager) {
        super.initWithProjectionManager(projectionManager)

        multiImage = projectionManager.createMultiImage()

        projectionManager.projectableProperty().addListener { _, _, newValue ->
            if (newValue == multiImage) {
                endProjectionButton.isDisable = false
                beginProjectionButton.isDisable = true
            } else {
                endProjectionButton.isDisable = true
                beginProjectionButton.isDisable = imageControls.isEmpty()
            }
        }

        loadImages()
        imagesVBox.children.add(createSingleControl())
    }

    override fun onEscapeKeyPressed() {
        if (!endProjectionButton.isDisable) {
            endProjection()
        }
    }

    override fun stop() {
        projectionManager.stop(multiImage)
    }

    @FXML
    fun beginProjection() {
        projectionManager.setProjectable(multiImage)
    }

    @FXML
    fun endProjection() {
        projectionManager.setProjectable(null)
    }

    private fun loadImages() {
        val countStr = observer.getProperty("IMAGES_COUNT").orElse(null)

        if (countStr != null) {
            val count = countStr.toInt()
            for (i in 0 until count) {
                val imgPath = observer.getProperty("IMAGE[$i]").orElse(null)
                val renderFlagValue = observer.getProperty("RENDER_FLAG[$i]").orElse("0").toInt()

                if (imgPath != null) {
                    val file = File(imgPath)
                    if (file.canRead()) {
                        try {
                            val control = createSingleControl()
                            control.callback = null

                            control.renderFlag?.value = renderFlagValue

                            if (control.loadImage(file)) {
                                imageControls.add(control)
                            }

                            control.callback = this
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }

        imagesVBox.children.addAll(imageControls)

        if (imageControls.size > 0) {
            beginProjectionButton.isDisable = false
            onRenderingChanged()
        }
    }

    private fun saveOpenedImages() {
        observer.beginPropertiesUpdate()

        observer.updateProperty("IMAGES_COUNT", imageControls.size.toString())

        for ((fileIndex, imageControl) in imageControls.withIndex()) {
            observer.updateProperty("IMAGE[$fileIndex]", imageControl.currentFile?.toString())
        }

        observer.finishPropertiesUpdate()
    }

    private fun saveRenderFlags() {
        observer.beginPropertiesUpdate()

        for ((fileIndex, imageControl) in imageControls.withIndex()) {
            observer.updateProperty("RENDER_FLAG[$fileIndex]", imageControl.renderFlag?.value?.toString())
        }

        observer.finishPropertiesUpdate()
    }

    override fun onImageChanged(singleImageControl: SingleImageControl) {
        if (!imageControls.contains(singleImageControl)) {
            imageControls.add(singleImageControl)
            imagesVBox.children.add(createSingleControl())
            beginProjectionButton.isDisable = false
        }

        saveOpenedImages()
    }

    override fun onRenderingChanged() {
        val imagesMap = HashMap<Int, Image>()

        for (imageControl in imageControls) {
            imageControl.currentImage?.let {
                for (renderId in imageControl.renderFlag?.getRenders() ?: Collections.emptyList()) {
                    imagesMap[renderId] = it
                }
            }
        }

        multiImage.setImages(imagesMap)
        saveRenderFlags()
    }

    private fun createSingleControl(): SingleImageControl {
        val ctrl = SingleImageControl()

        ctrl.manager = projectionManager
        ctrl.callback = this
        ctrl.defaultBorder = Border(BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT))

        VBox.setMargin(ctrl, Insets(8.0))

        return ctrl
    }
}