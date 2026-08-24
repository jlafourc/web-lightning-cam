package com.lightningcam.detector

data class LuminanceFrame(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
    val timestampMs: Long,
) {
    init {
        require(width > 0 && height > 0)
        require(pixels.size == width * height)
        require(pixels.all { it in 0..255 })
    }

    override fun equals(other: Any?): Boolean =
        other is LuminanceFrame &&
            width == other.width &&
            height == other.height &&
            pixels.contentEquals(other.pixels) &&
            timestampMs == other.timestampMs

    override fun hashCode(): Int = 31 * (31 * width + height) + pixels.contentHashCode()
}
