package com.example.ml

import androidx.camera.core.ImageProxy
import kotlin.math.abs

/**
 * Lightweight temporal liveness gate. It rejects static frames and requires a short
 * sequence of real camera motion before the challenge can progress. It is not a
 * cryptographic anti-replay system; a sophisticated video replay can still defeat it.
 */
class LocalLivenessDetector : LivenessDetector {
    private var previousFrameYBuffer: ByteArray? = null
    private var staticFrameCount = 0
    private var liveFrameCount = 0
    private val motionHistory = ArrayDeque<Float>()

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
                val value = yPlane.get(y * rowStride + x)
                currentSample[index++] = value
                totalLum += value.toInt() and 0xFF
            }
        }

        val averageLuminance = totalLum.toFloat() / currentSample.size
        val previous = previousFrameYBuffer
        previousFrameYBuffer = currentSample

        if (previous == null) {
            return LivenessResult(
                isLiveEnvironment = false,
                motionScore = 0f,
                isStaticPhoto = false,
                isScreenReplay = false,
                warningMessage = "Move the camera slowly to prove this is live."
            )
        }

        var diffSum = 0L
        for (i in currentSample.indices) {
            diffSum += abs(
                (currentSample[i].toInt() and 0xFF) - (previous[i].toInt() and 0xFF)
            )
        }
        val normalizedDiff = diffSum.toFloat() / (currentSample.size * 255f)
        motionHistory.addLast(normalizedDiff)
        if (motionHistory.size > 15) motionHistory.removeFirst()
        val averageMotion = motionHistory.average().toFloat()

        if (normalizedDiff < 0.0025f) {
            staticFrameCount++
        } else {
            staticFrameCount = (staticFrameCount - 1).coerceAtLeast(0)
        }

        if (normalizedDiff >= 0.0025f && normalizedDiff <= 0.20f) {
            liveFrameCount++
        } else {
            liveFrameCount = (liveFrameCount - 1).coerceAtLeast(0)
        }

        val isStaticPhoto = staticFrameCount >= 8
        // This remains a heuristic. It catches very bright, nearly static displays but
        // deliberately does not claim that motion alone proves a real-world environment.
        val isScreenReplay = isStaticPhoto || (averageLuminance > 245f && averageMotion < 0.004f)
        val isLive = !isStaticPhoto && !isScreenReplay && liveFrameCount >= 6

        val warning = when {
            isStaticPhoto -> "❌ LIVE ENVIRONMENT REQUIRED. Photos don't count."
            isScreenReplay -> "❌ That looks like a screen replay. Use the live camera."
            !isLive -> "Move the camera slowly so we can verify a live environment."
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
