package dev.juhouse.projector.projection2

import dev.juhouse.projector.other.GraphicsFinder
import dev.juhouse.projector.services.SettingsService
import dev.juhouse.projector.utils.WindowConfigsObserver
import java.awt.GraphicsDevice

class WindowManager(private val settingsService: SettingsService) : CanvasDelegate, WindowConfigsObserver.WindowConfigsObserverCallback {
    private val bridge: Bridge = Bridge()

    val configsObserver: WindowConfigsObserver = WindowConfigsObserver(this)
    val preview: PreviewImageView = PreviewImageView()
    val manager: ProjectionManager = ProjectionManagerImpl(this)

    private var running: Boolean = false

    init {
        // TODO: Fix me when buffer provider gets implemented
        preview.setPixelBufferProvider(null)
    }

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
        return bridge.textRenderAreaWidth
    }

    override fun getTextHeight(): Int {
        return bridge.textRenderAreaHeight
    }

    override fun getSettingsService(): SettingsService {
        return settingsService;
    }

    override fun getDefaultDevice(): GraphicsDevice {
        return GraphicsFinder.getDefaultDevice().device
    }

    override fun getBridge(): Bridge {
        return bridge
    }

    override fun updateConfigs(filePath: String?) {
        bridge.loadConfig(filePath)
        manager.rebuild()
    }
}