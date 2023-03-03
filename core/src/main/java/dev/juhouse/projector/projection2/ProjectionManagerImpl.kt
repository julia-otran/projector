package dev.juhouse.projector.projection2

import dev.juhouse.projector.projection2.models.BackgroundModel
import dev.juhouse.projector.projection2.text.WrappedText
import dev.juhouse.projector.projection2.text.WrapperFactory
import dev.juhouse.projector.projection2.video.ProjectionBackgroundVideo
import dev.juhouse.projector.projection2.video.ProjectionPlayer
import dev.juhouse.projector.projection2.video.ProjectionVideo
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyProperty
import java.awt.Font
import java.io.File

class ProjectionManagerImpl(private val delegate: CanvasDelegate):
    ProjectionManager {
    private val label = ProjectionLabel(delegate)
    private val backgroundVideo = ProjectionBackgroundVideo(ProjectionVideo(delegate))
    private val background = ProjectionBackground(delegate)
    private val currentProjectable = ReadOnlyObjectWrapper<Projectable?>()
    private val projectablesList = ArrayList<Projectable>()

    init {
        projectablesList.add(label)
        projectablesList.add(backgroundVideo)
        projectablesList.add(background)
    }

    override fun init() {
        projectablesList.forEach { it.init() }
    }

    override fun finish() {
        projectablesList.forEach { it.finish() }
    }

    override fun rebuild() {
        projectablesList.forEach { it.rebuild() }
    }

    override fun setText(text: WrappedText?) {
        label.setText(text)
    }

    override fun getTextFont(): Font {
        return label.font
    }

    override fun setTextFont(font: Font?) {
        label.font = font
    }

    override fun addTextWrapperChangeListener(wrapperChangeListener: TextWrapperFactoryChangeListener?) {
        label.addWrapperChangeListener(wrapperChangeListener)
    }

    override fun createWebView(): ProjectionWebView {
        val webView = ProjectionWebView(delegate)
        webView.init()
        projectablesList.add(webView)
        return webView
    }

    override fun createImage(): ProjectionImage {
        val image = ProjectionImage(delegate)

        projectablesList.add(image)

        image.init()
        return image
    }

    override fun setProjectable(projectable: Projectable?) {
        currentProjectable.get()?.setRender(false)

        currentProjectable.set(projectable)

        backgroundVideo.setRender(projectable == null)
        projectable?.setRender(true)
    }

    override fun getWrapperFactory(): WrapperFactory {
        return label.wrapperFactory
    }

    override fun getBackgroundModel(): BackgroundModel {
        return background.model as BackgroundModel
    }

    override fun projectableProperty(): ReadOnlyProperty<Projectable?> {
        return currentProjectable.readOnlyProperty
    }

    override fun setBackgroundModel(background: BackgroundModel?) {
        this.background.setModel(background)
    }

    override fun setCropBackground(selected: Boolean) {
        background.cropBackground = selected
    }

    override fun createPlayer(): ProjectionPlayer {
        val player = ProjectionPlayer(ProjectionVideo(delegate))

        projectablesList.add(player)

        player.init()
        return player;
    }

    override fun getDarkenBackground(): Boolean {
        return false
    }

    override fun setDarkenBackground(darkenBg: Boolean) {
        // TODO
    }

    override fun stop(projectable: Projectable?) {
        projectable?.finish()
        projectablesList.remove(projectable)
    }

    override fun setMusicForBackground(musicId: Int?, preferred: File?) {
        backgroundVideo.setRender(currentProjectable.get() == null)

        if (musicId != null) {
            backgroundVideo.startBackground(musicId, preferred)
        } else {
            backgroundVideo.stopBackground()
        }
    }

    override fun getCropBackground(): Boolean {
        return background.cropBackground
    }
}