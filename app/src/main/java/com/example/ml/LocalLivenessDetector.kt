package com.example.ml

import androidx.camera.core.ImageProxy

class LocalLivenessDetector : LivenessDetector {

    private var previousFrameYBuffer: ByteArray? = null
    private var staticFrameCount = 0
    private var averageMotionHistory = mutableListOf<Float>()

    override fun checkLiveness(imageProxy: ImageProxy): LivenessResult {
        val yPlane = imageProxy.planes[0].buffer
        val width = imageProxy.width
        val height = imageProxy.height
        val rowStride = imageProxy.planes[0].rowStride

        val sampleWidth = 32
        val sampleHeight = 24
        val currentSample = ByteArray(sampleWidth * sampleHeight)

        val xStep = maxOf(1, width / sampleWidth)
        val yStep = maxOf(1, height / sampleHeight)

        var totalLum = 0L
        var index = 0

        for (sy in 0 until sampleHeight) {
            val y = (sy * yStep).coerceAtMost(height - 1)
            for (sx in 0 until sampleWidth) {
                val x = (sx * xStep).coerceAtMost(width - 1)
                val pixelVal = yPlane.get(y * rowStride + x)
                currentSample[index++] = pixelVal
                totalLum += (pixelVal.toInt() and 0xFF)
            }
        }

        val averageLuminance = totalLum.toFloat() / currentSample.size
        val prev = previousFrameYBuffer
        previousFrameYBuffer = currentSample

        if (prev == null) {
            return LivenessResult(
                isLiveEnvironment = true,
                motionScore = 0.05f,
                isStaticPhoto = false,
                isScreenReplay = false
            )
        }

        var diffSum = 0L
        for (i in currentSample.indices) {
            val d = Math.abs((currentSample[i].toInt() and 0xFF) - (prev[i].toInt() and 0xFF))
            diffSum += d
        }
        val normalizedDiff = diffSum.toFloat() / (currentSample.size * 255f)

        averageMotionHistory.add(normalizedDiff)
        if (averageMotionHistory.size > 15) {
            averageMotionHistory.removeAt(0)
        }

        val averageMotion = averageMotionHistory.average().toFloat()

        // Anti-cheat: Check for motionless photo
        // A human holding a phone outdoors always has natural micro-tremor (normalizedDiff > 0.003f)
        // A camera on a flat printed photo or screenshot has near 0 difference
        if (normalizedDiff < 0.0025f) {
            staticFrameCount++
        } else {
            staticFrameCount = maxOf(0, staticFrameCount - 1)
        }

        val isStaticPhoto = staticFrameCount >= 8 // ~1 second of zero motion

        // Screen replay hint: extreme low variance or typical backlight clipped luminance
        val isScreenReplay = isStaticPhoto || (averageLuminance > 240f && averageMotion < 0.004f)

        val isLive = !isStaticPhoto && !isScreenReplay

        val warning = when {
            isStaticPhoto -> "❌ Live environment required. Don't point at a photo!"
            isScreenReplay -> "❌ That appears to be a digital screen, not outdoors."
            else -> null
        }

        return LivenessResult(
            isLiveEnvironment = isLive,
            motionScore = normalizedDiff,
            isStaticPhoto = isStaticPhoto,
            isScreenReplay = isScreenReplay,
            warningMessage = warning
        )
    }
}
