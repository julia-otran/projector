package dev.juhouse.projector.projection2

import dev.juhouse.projector.other.GraphicsFinder
import dev.juhouse.projector.services.SettingsService
import dev.juhouse.projector.utils.WindowConfigsObserver
import java.awt.GraphicsDevice

class WindowManager(private val settingsService: SettingsService) : CanvasDelegate, WindowConfigsObserver.WindowConfigsObserverCallback {
    private val bridge: Bridge = Bridge()

    val configsObserver: WindowConfigsObserver = WindowConfigsObserver(this)
    val preview: PreviewImageView = PreviewImageView(this)
    val manager: ProjectionManager = ProjectionManagerImpl(this)

    private var running: Boolean = false

    fun startEngine() {
        if (!running) {
            running = true
            bridge.initialize()
            configsObserver.start()
            manager.init()
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
        manager.rebuild()
        preview.start()
    }
}