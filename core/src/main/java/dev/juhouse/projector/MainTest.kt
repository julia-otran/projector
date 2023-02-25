package dev.juhouse.projector

import dev.juhouse.projector.projection2.Bridge
import dev.juhouse.projector.utils.WindowConfigsObserver

class MainTest {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val bridge = Bridge()

            bridge.initialize()

            var configObserver = WindowConfigsObserver(WindowConfigsObserver.WindowConfigsObserverCallback {
                bridge.load_config(it);
            })

            configObserver.start()

            Thread.sleep(15000)

            configObserver.stop()

            println("Terminating....")
            bridge.shutdown()
        }
    }
}