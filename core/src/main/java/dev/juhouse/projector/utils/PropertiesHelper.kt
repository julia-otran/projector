package dev.juhouse.projector.utils

import java.util.*

class PropertiesHelper {
    companion object {
        fun getVersion(): String {
            val propsResource = this::class.java.getResourceAsStream("/maven.properties")
            val p = Properties()
            p.load(propsResource)
            return p.getProperty("revision")
        }
    }
}