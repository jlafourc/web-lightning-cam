package com.lightningcam.camera

import com.lightningcam.detector.LuminanceFrame

object YPlaneSampler {
    fun sample(
        bytes: ByteArray,
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
                val sourceX = targetX * sourceWidth / targetWidth
                val sourceIndex = sourceY * rowStride + sourceX * pixelStride
                output[targetY * targetWidth + targetX] = bytes[sourceIndex].toInt() and 0xff
            }
        }
        return LuminanceFrame(targetWidth, targetHeight, output, timestampMs)
    }
}
