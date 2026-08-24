package com.lightningcam.camera

import com.lightningcam.detector.LuminanceFrame

object YPlaneSampler {
    fun sample(input: LuminanceInput, targetWidth: Int, targetHeight: Int): LuminanceFrame = sample(
        lumaAt = input::luminanceAt,
        sourceWidth = input.width,
        sourceHeight = input.height,
        rowStride = input.rowStride,
        pixelStride = input.pixelStride,
        targetWidth = targetWidth,
        targetHeight = targetHeight,
        timestampMs = input.timestampMs,
    )

    fun sample(
        bytes: ByteArray,
        sourceWidth: Int,
        sourceHeight: Int,
        rowStride: Int,
        pixelStride: Int,
        targetWidth: Int,
        targetHeight: Int,
        timestampMs: Long,
    ): LuminanceFrame = sample(
        lumaAt = { index -> bytes[index].toInt() and 0xff },
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        rowStride = rowStride,
        pixelStride = pixelStride,
        targetWidth = targetWidth,
        targetHeight = targetHeight,
        timestampMs = timestampMs,
    )

    private fun sample(
        lumaAt: (Int) -> Int,
        sourceWidth: Int,
        sourceHeight: Int,
        rowStride: Int,
        pixelStride: Int,
        targetWidth: Int,
        targetHeight: Int,
        timestampMs: Long,
    ): LuminanceFrame {
        require(sourceWidth > 0 && sourceHeight > 0)
        require(rowStride > 0 && pixelStride > 0)
        require(targetWidth in 1..sourceWidth && targetHeight in 1..sourceHeight)
        val output = IntArray(targetWidth * targetHeight)
        for (targetY in 0 until targetHeight) {
            val sourceY = targetY * sourceHeight / targetHeight
            for (targetX in 0 until targetWidth) {
                val startX = targetX * sourceWidth / targetWidth
                val endX = maxOf(startX + 1, (targetX + 1) * sourceWidth / targetWidth)
                val endY = maxOf(sourceY + 1, (targetY + 1) * sourceHeight / targetHeight)
                var brightest = 0
                var secondBrightest = 0
                for (sampleY in sourceY until endY) {
                    for (sampleX in startX until endX) {
                        val value = lumaAt(sampleY * rowStride + sampleX * pixelStride)
                        if (value >= brightest) {
                            secondBrightest = brightest
                            brightest = value
                        } else if (value > secondBrightest) {
                            secondBrightest = value
                        }
                    }
                }
                output[targetY * targetWidth + targetX] = if ((endX - startX) * (endY - sourceY) > 1) secondBrightest else brightest
            }
        }
        return LuminanceFrame(targetWidth, targetHeight, output, timestampMs)
    }
}
