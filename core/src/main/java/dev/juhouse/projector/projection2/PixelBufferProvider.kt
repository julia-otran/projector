package dev.juhouse.projector.projection2

interface PixelBufferProvider {
    fun getWidth(): Int
    fun getHeight(): Int

    fun getRawData(): Array<Byte>
}