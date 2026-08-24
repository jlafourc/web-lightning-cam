package com.lightningcam.camera

import androidx.camera.core.ImageProxy

class ImageProxyInput(private val image: ImageProxy) : LuminanceInput {
    private val plane = image.planes[0]
    private val buffer = plane.buffer.duplicate()
    private val basePosition = buffer.position()
    override val width: Int = image.width
    override val height: Int = image.height
    override val rowStride: Int = plane.rowStride
    override val pixelStride: Int = plane.pixelStride
    override val timestampMs: Long = image.imageInfo.timestamp / 1_000_000L
    override fun luminanceAt(byteIndex: Int): Int = buffer.get(basePosition + byteIndex).toInt() and 0xff
    override fun close() = image.close()
}
