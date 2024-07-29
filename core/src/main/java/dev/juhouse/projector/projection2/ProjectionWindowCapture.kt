package dev.juhouse.projector.projection2

import dev.juhouse.projector.utils.promise.JavaFxExecutor
import dev.juhouse.projector.utils.promise.Promise
import dev.juhouse.projector.utils.promise.Task

class ProjectionWindowCapture(private val delegate: CanvasDelegate): Projectable {
    private val renderFlag = BridgeRenderFlag(delegate)
    private var windowName: String? = null
    private var render: Boolean = false

    override fun init() {

    }

    override fun finish() {

    }

    override fun rebuild() {
        renderFlag.applyDefault { it.enableRenderVideo }
    }

    override fun setRender(render: Boolean) {
        this.render = render;
        updateRender()
    }

    override fun getRenderFlag(): BridgeRenderFlag = renderFlag

    fun setWindowCaptureName(name: String?) {
        this.windowName = name
        updateRender()
    }

    private fun updateRender() {
        if (render && !windowName.isNullOrBlank()) {
            delegate.bridge.setWindowCaptureWindowName(windowName)
            delegate.bridge.setWindowCaptureRender(renderFlag.value)
        } else {
            delegate.bridge.setWindowCaptureRender(BridgeRenderFlag.NO_RENDER)
        }
    }

    fun getWindowList(): Promise<List<String>> {
        return Promise.create(Task { _, callback ->
            delegate.bridge.getWindowList(object: BridgeWindowCaptureCallbacks {
                override fun onWindowListDone(windowList: Array<String>) {
                    callback.success(windowList.toList())
                }
            })
        }, JavaFxExecutor())
    }

    fun setCrop(newVal: Boolean) {
        delegate.bridge.setWindowCaptureCrop(newVal)
    }
}