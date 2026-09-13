package com.example.ml

import androidx.camera.core.ImageProxy

/**
 * Lightweight, offline foliage heuristic used until a trained grass segmentation
 * model is bundled. It is intentionally conservative and exposes spatial metrics
 * so contact verification does not rely on a single green-pixel threshold.
 */
class LocalGrassDetector : GrassDetector {

    override fun detectGrass(imageProxy: ImageProxy): GrassDetectionResult {
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
        var greenSamples = 0
        var topGreenSamples = 0
        var middleGreenSamples = 0
        var bottomGreenSamples = 0
        val greenHues = ArrayList<Float>(256)

        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                totalSamples++
                val yIndex = y * yRowStride + x
                val uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride
                val yVal = yPlane.get(yIndex).toInt() and 0xFF
                val uVal = (uPlane.get(uvIndex).toInt() and 0xFF) - 128
                val vVal = (vPlane.get(uvIndex).toInt() and 0xFF) - 128

                val r = (yVal + 1.402f * vVal).coerceIn(0f, 255f)
                val g = (yVal - 0.344136f * uVal - 0.714136f * vVal).coerceIn(0f, 255f)
                val b = (yVal + 1.772f * uVal).coerceIn(0f, 255f)
                val max = maxOf(r, g, b)
                val min = minOf(r, g, b)
                val delta = max - min

                if (delta <= 18f || max <= 35f) continue

                val hue = when (max) {
                    r -> 60f * ((g - b) / delta)
                    g -> 60f * ((b - r) / delta + 2f)
                    else -> 60f * ((r - g) / delta + 4f)
                }.let { if (it < 0f) it + 360f else it }
                val saturation = delta / max

                if (hue in 68f..165f && saturation > 0.22f && g > r * 1.05f) {
                    greenSamples++
                    if (greenHues.size < 4000) greenHues.add(hue)
                    when {
                        y < height / 3 -> topGreenSamples++
                        y < height * 2 / 3 -> middleGreenSamples++
                        else -> bottomGreenSamples++
                    }
                }
            }
        }

        if (totalSamples == 0) {
            return GrassDetectionResult(isGrassDetected = false, greenRatio = 0f)
        }

        val greenRatio = greenSamples.toFloat() / totalSamples
        val bottomGreenRatio = bottomGreenSamples.toFloat() / (totalSamples / 3f).coerceAtLeast(1f)
        val topGreenRatio = topGreenSamples.toFloat() / (totalSamples / 3f).coerceAtLeast(1f)
        val isGreenScene = greenRatio >= 0.18f

        // A tree canopy is mostly green in the upper third while the lower third has little foliage.
        val isTreeCanopy = isGreenScene && topGreenRatio > 0.20f && bottomGreenRatio < topGreenRatio * 0.45f

        // Heuristic only: this catches a narrow class of unnaturally uniform plastic turf.
        var isArtificialTurf = false
        if (greenHues.size > 50) {
            val meanHue = greenHues.average().toFloat()
            val variance = greenHues.map { (it - meanHue) * (it - meanHue) }.average()
            val stdDev = kotlin.math.sqrt(variance).toFloat()
            if (stdDev < 3.2f && meanHue in 115f..125f && bottomGreenRatio > 0.12f) {
                isArtificialTurf = true
            }
        }

        // Require meaningful green coverage in the lower part of the frame. This makes
        // pointing at a green wall/tree substantially less likely to qualify as ground.
        val lowerGroundEvidence = bottomGreenRatio >= 0.10f ||
            (bottomGreenRatio >= 0.06f && middleGreenSamples > topGreenSamples * 0.8f)
        val isGrassDetected = isGreenScene && lowerGroundEvidence && !isTreeCanopy && !isArtificialTurf

        val confidence = (
            (greenRatio * 1.5f) *
                (0.55f + (bottomGreenRatio.coerceIn(0f, 1f) * 0.45f))
            ).coerceIn(0f, 1f)

        return GrassDetectionResult(
            isGrassDetected = isGrassDetected,
            greenRatio = greenRatio,
            bottomGreenRatio = bottomGreenRatio.coerceIn(0f, 1f),
            isArtificialTurf = isArtificialTurf,
            isTreeCanopy = isTreeCanopy,
            confidence = confidence
        )
    }
}
