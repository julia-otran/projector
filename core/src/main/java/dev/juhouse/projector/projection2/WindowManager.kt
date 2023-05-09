package dev.juhouse.projector.projection2

import dev.juhouse.projector.other.ProjectorPreferences
import dev.juhouse.projector.services.SettingsService
import dev.juhouse.projector.utils.FontCreatorUtil
import dev.juhouse.projector.utils.WindowConfigsObserver
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import java.awt.Font

class WindowManager(private val settingsService: SettingsService) : CanvasDelegate, WindowConfigsObserver.WindowConfigsObserverCallback {
    private val bridge: Bridge = Bridge()
    private val fontProperty = SimpleObjectProperty<Font>()

    val configsObserver: WindowConfigsObserver = WindowConfigsObserver(this)
    val preview: PreviewImageView = PreviewImageView(this)
    val manager: ProjectionManager = ProjectionManagerImpl(this)

    private var running: Boolean = false

    init {
        fontProperty.set(
            FontCreatorUtil.createFont(
                        ProjectorPreferences.getProjectionLabelFontName(),
                        ProjectorPreferences.getProjectionLabelFontStyle(),
                        ProjectorPreferences.getProjectionLabelFontSize()
            )
        )

        fontProperty.addListener { _, _, font ->
            ProjectorPreferences.setProjectionLabelFontName(font.family);
            ProjectorPreferences.setProjectionLabelFontStyle(font.style);
            ProjectorPreferences.setProjectionLabelFontSize(font.size);
        }
    }

    fun startEngine() {
        if (!running) {
            bridge.loadShaders()
            bridge.initialize()
            configsObserver.start()
            manager.init()
            running = true
        }
    }

    fun stopEngine() {
        if (running) {
            running = false
            configsObserver.stop()
            preview.stop()
            manager.finish()
            bridge.shutdown()
        }
    }

    override fun getMainWidth(): Int {
        return bridge.renderAreaWidth
    }

    override fun getMainHeight(): Int {
        return bridge.renderAreaHeight
    }

    override fun getTextWidth(): Int {
        return bridge.textAreaWidth
    }

    override fun getTextHeight(): Int {
        return bridge.textAreaHeight
    }

    override fun getFontProperty(): Property<Font> {
        return fontProperty
    }

    override fun getSettingsService(): SettingsService {
        return settingsService;
    }

    override fun getBridge(): Bridge {
        return bridge
    }

    override fun createConfigs(filePath: String?) {
        bridge.generateConfig(filePath);
    }

    override fun updateConfigs(filePath: String?) {
        preview.stop()
        bridge.loadConfig(filePath)

        if (running) {
            manager.rebuild()
        }

        preview.start()
    }

    fun reloadDevices() {
        preview.stop()

        bridge.reload()

        if (running) {
            manager.rebuild()
        }

        preview.start()
    }
}