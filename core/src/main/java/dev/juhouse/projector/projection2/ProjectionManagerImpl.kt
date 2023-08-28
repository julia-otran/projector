package dev.juhouse.projector.projection2

import dev.juhouse.projector.other.ProjectorPreferences
import dev.juhouse.projector.projection2.time.ProjectionCountdown
import dev.juhouse.projector.projection2.image.ProjectionBackground
import dev.juhouse.projector.projection2.image.ProjectionImage
import dev.juhouse.projector.projection2.image.ProjectionMultiImage
import dev.juhouse.projector.projection2.models.BackgroundModel
import dev.juhouse.projector.projection2.text.ProjectionLabel
import dev.juhouse.projector.projection2.text.WrappedText
import dev.juhouse.projector.projection2.text.WrapperFactory
import dev.juhouse.projector.projection2.time.ProjectionClock
import dev.juhouse.projector.projection2.video.ProjectionBackgroundVideo
import dev.juhouse.projector.projection2.video.ProjectionPlayer
import dev.juhouse.projector.projection2.video.ProjectionVideo
import javafx.application.Platform
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
    private val concurrentProjectable = ReadOnlyObjectWrapper<Projectable?>()
    private val projectablesList = ArrayList<Projectable>()
    private val callbackList = ArrayList<ProjectionManagerCallbacks>()

    init {
        projectablesList.add(label)
        projectablesList.add(backgroundVideo)
        projectablesList.add(background)
    }

    override fun init() {
        projectablesList.forEach { it.init() }
        projectablesList.forEach { it.rebuild() }
    }

    override fun finish() {
        projectablesList.forEach { it.finish() }
    }

    override fun rebuild() {
        projectablesList.forEach { it.rebuild() }

        val renders = delegate.bridge.renderSettings

        Platform.runLater {
            callbackList.forEach { it.onRebuild(renders) }
        }
    }

    override fun setText(text: WrappedText?) {
        setText(text, null)
    }

    override fun setText(current: WrappedText?, ahead: WrappedText?) {
        concurrentProjectable.get()?.renderFlagProperty?.get()?.renderToNone()
        label.renderFlagProperty.get().renderToAll()
        label.setText(current, ahead)
    }

    override fun textClear() {
        label.setClear(true);
    }

    override fun getTextFont(): Font {
        return delegate.fontProperty.value
    }

    override fun setTextFont(font: Font?) {
        delegate.fontProperty.value = font
    }

    override fun addTextWrapperChangeListener(wrapperChangeListener: TextWrapperFactoryChangeListener?) {
        label.addWrapperChangeListener(wrapperChangeListener)
    }

    override fun createLabel(): ProjectionLabel {
        val label = ProjectionLabel(delegate)
        label.init()
        label.rebuild()

        projectablesList.add(label)

        return label
    }

    override fun createWebView(): ProjectionWebView {
        val webView = ProjectionWebView(delegate)

        webView.init()
        webView.rebuild()

        projectablesList.add(webView)
        return webView
    }

    override fun createImage(): ProjectionImage {
        val image = ProjectionImage(delegate)

        projectablesList.add(image)

        image.init()
        image.rebuild()

        return image
    }

    override fun createWindowCapture(): ProjectionWindowCapture {
        val windowCapture = ProjectionWindowCapture(delegate)

        projectablesList.add(windowCapture)

        windowCapture.init()
        windowCapture.rebuild()

        return windowCapture
    }

    override fun createMultiImage(): ProjectionMultiImage {
        val multiImage = ProjectionMultiImage(delegate)

        projectablesList.add(multiImage)

        multiImage.init()
        multiImage.rebuild()

        return multiImage
    }

    override fun createVideoCapture(): ProjectionVideoCapture {
        val videoCapturePlayer = ProjectionVideoCapture(delegate)

        projectablesList.add(videoCapturePlayer)

        videoCapturePlayer.init()
        videoCapturePlayer.rebuild()

        return videoCapturePlayer
    }

    override fun createClock(): ProjectionClock {
        val clock = ProjectionClock(delegate)

        projectablesList.add(clock)

        clock.init()
        clock.rebuild()

        return clock
    }

    private fun setBackgroundExcludeFlag() {
        val renderFlagProperty =
            currentProjectable.get()?.renderFlagProperty ?:
            concurrentProjectable.get()?.renderFlagProperty

        background.setExcludeRenderFlag(renderFlagProperty?.get())
    }

    override fun setConcurrentProjectable(projectable: Projectable?) {
        concurrentProjectable.get()?.setRender(false)

        concurrentProjectable.set(projectable)

        setBackgroundExcludeFlag()

        projectable?.setRender(true)
    }

    override fun setProjectable(projectable: Projectable?) {
        currentProjectable.get()?.setRender(false)

        currentProjectable.set(projectable)

        setBackgroundExcludeFlag()

        backgroundVideo.setRender(projectable == null)

        projectable?.setRender(true)
    }

    override fun getWrapperFactory(): WrapperFactory {
        return label.wrapperFactory
    }

    override fun getBackgroundModel(): BackgroundModel {
        return background.model as BackgroundModel
    }

    override fun concurrentProjectableProperty(): ReadOnlyProperty<Projectable?> {
        return concurrentProjectable.readOnlyProperty
    }

    override fun projectableProperty(): ReadOnlyProperty<Projectable?> {
        return currentProjectable.readOnlyProperty
    }

    override fun setBackgroundModel(background: BackgroundModel?) {
        this.background.setModel(background)
    }

    override fun setCropBackground(selected: Boolean) {
        ProjectorPreferences.setCropBackground(selected)
        background.cropBackground = selected
    }

    override fun createPlayer(): ProjectionPlayer {
        val player = ProjectionPlayer(ProjectionVideo(delegate), delegate)

        projectablesList.add(player)

        player.init()
        player.rebuild()

        return player
    }

    override fun createCountdown(): ProjectionCountdown {
        val countdown = ProjectionCountdown(delegate)

        projectablesList.add(countdown)

        countdown.init()
        countdown.rebuild()

        return countdown
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
        if (musicId != null) {
            if (currentProjectable.get() != null) {
                setProjectable(null)
            }

            backgroundVideo.startBackground(musicId, preferred)
            backgroundVideo.setRender(true)
            background.render = false
        } else {
            backgroundVideo.stopBackground()
            background.render = true
        }
    }

    override fun getCropBackground(): Boolean {
        return background.cropBackground
    }

    override fun addCallback(callback: ProjectionManagerCallbacks) {
        callbackList.add(callback)
        callback.onRebuild(delegate.bridge.renderSettings)
    }

    override fun removeCallback(callback: ProjectionManagerCallbacks) {
        callbackList.remove(callback)
    }

    override fun createRenderFlag(): BridgeRenderFlag {
        return BridgeRenderFlag(delegate)
    }
}