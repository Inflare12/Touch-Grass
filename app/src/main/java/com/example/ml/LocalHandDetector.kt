package com.example.ml

import androidx.camera.core.ImageProxy

class LocalHandDetector : HandDetector {

    private var previousHandY: Float? = null

    override fun detectHand(imageProxy: ImageProxy): HandDetectionResult {
        val yPlane = imageProxy.planes[0].buffer
        val uPlane = imageProxy.planes[1].buffer
        val vPlane = imageProxy.planes[2].buffer

        val width = imageProxy.width
        val height = imageProxy.height

        val yRowStride = imageProxy.planes[0].rowStride
        val uvRowStride = imageProxy.planes[1].rowStride
        val uvPixelStride = imageProxy.planes[1].pixelStride

        val step = 8
        var totalSamples = 0
        var skinSamples = 0
        var sumX = 0L
        var sumY = 0L

        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                totalSamples++

                val yIndex = y * yRowStride + x
                val uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride

                val yVal = yPlane.get(yIndex).toInt() and 0xFF
                val cb = uPlane.get(uvIndex).toInt() and 0xFF
                val cr = vPlane.get(uvIndex).toInt() and 0xFF

                // Standard YCbCr skin color cluster
                // Cb in [77..127], Cr in [133..173], Y > 60
                if (cb in 77..127 && cr in 133..173 && yVal > 50) {
                    skinSamples++
                    sumX += x
                    sumY += y
                }
            }
        }

        if (totalSamples == 0 || skinSamples < 25) {
            previousHandY = null
            return HandDetectionResult(
                isHandDetected = false,
                handSkinRatio = 0f
            )
        }

        val skinRatio = skinSamples.toFloat() / totalSamples
        val isHand = skinRatio in 0.03f..0.65f // hand present without occupying whole screen
        val centroidX = (sumX.toFloat() / skinSamples) / width
        val centroidY = (sumY.toFloat() / skinSamples) / height

        val isMovingDown = previousHandY?.let { prevY ->
            centroidY > prevY + 0.015f // Moving downwards towards bottom of frame where grass is
        } ?: false

        previousHandY = centroidY

        return HandDetectionResult(
            isHandDetected = isHand,
            handCentroidX = centroidX,
            handCentroidY = centroidY,
            handSkinRatio = skinRatio,
            isMovingDownTowardsGrass = isMovingDown,
            confidence = (skinRatio * 3f).coerceIn(0f, 1f)
        )
    }
}
