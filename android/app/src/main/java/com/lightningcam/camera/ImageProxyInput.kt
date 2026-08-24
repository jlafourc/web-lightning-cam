package com.lightningcam.camera

import androidx.camera.core.ImageProxy

class ImageProxyInput(private val image: ImageProxy) : LuminanceInput {
    private val plane = image.planes[0]
    override val bytes: ByteArray = plane.buffer.let { buffer ->
        val duplicate = buffer.duplicate()
        ByteArray(duplicate.remaining()).also(duplicate::get)
    }
    override val width: Int = image.width
    override val height: Int = image.height
    override val rowStride: Int = plane.rowStride
    override val pixelStride: Int = plane.pixelStride
    override val timestampMs: Long = image.imageInfo.timestamp / 1_000_000L
    override fun close() = image.close()
}
