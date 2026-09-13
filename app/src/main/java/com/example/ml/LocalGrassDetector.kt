package com.example.ml

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

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

        val step = 8 // Sample every 8th pixel for speed & efficiency
        var totalSamples = 0
        var greenSamples = 0
        var topGreenSamples = 0
        var bottomGreenSamples = 0

        val greenHues = mutableListOf<Float>()

        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                totalSamples++

                val yIndex = y * yRowStride + x
                val uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride

                val yVal = (yPlane.get(yIndex).toInt() and 0xFF)
                val uVal = (uPlane.get(uvIndex).toInt() and 0xFF) - 128
                val vVal = (vPlane.get(uvIndex).toInt() and 0xFF) - 128

                // Approximate RGB
                val r = (yVal + 1.402f * vVal).coerceIn(0f, 255f)
                val g = (yVal - 0.344136f * uVal - 0.714136f * vVal).coerceIn(0f, 255f)
                val b = (yVal + 1.772f * uVal).coerceIn(0f, 255f)

                val max = maxOf(r, g, b)
                val min = minOf(r, g, b)
                val delta = max - min

                if (delta > 18f && max > 35f) {
                    val hue = when (max) {
                        r -> (60f * ((g - b) / delta + 0f)) % 360f
                        g -> 60f * ((b - r) / delta + 2f)
                        else -> 60f * ((r - g) / delta + 4f)
                    }.let { if (it < 0) it + 360f else it }

                    val saturation = delta / max

                    // Natural foliage/grass hue: ~65 deg to ~165 deg, with healthy green dominance
                    if (hue in 68f..165f && saturation > 0.22f && g > r * 1.05f) {
                        greenSamples++
                        greenHues.add(hue)

                        if (y < height / 3) {
                            topGreenSamples++
                        } else {
                            bottomGreenSamples++
                        }
                    }
                }
            }
        }

        if (totalSamples == 0) {
            return GrassDetectionResult(isGrassDetected = false, greenRatio = 0f)
        }

        val greenRatio = greenSamples.toFloat() / totalSamples
        val isGrassDetected = greenRatio >= 0.18f

        // Anti-cheat: Check if user is pointing up at a tree canopy instead of down at grass
        val isTreeCanopy = isGrassDetected && (topGreenSamples > bottomGreenSamples * 2.5f)

        // Anti-cheat: Artificial turf check
        // Real grass has natural chlorophyll variance (> 6.5 hue variance across blades)
        // Artificial plastic turf has monochromatic uniform dye (variance < 3.5)
        var isArtificialTurf = false
        if (greenHues.size > 50) {
            val meanHue = greenHues.average().toFloat()
            val variance = greenHues.map { (it - meanHue) * (it - meanHue) }.average()
            val stdDev = Math.sqrt(variance).toFloat()
            // Unnaturally uniform neon plastic green
            if (stdDev < 3.2f && meanHue in 115f..125f) {
                isArtificialTurf = true
            }
        }

        return GrassDetectionResult(
            isGrassDetected = isGrassDetected && !isTreeCanopy && !isArtificialTurf,
            greenRatio = greenRatio,
            isArtificialTurf = isArtificialTurf,
            isTreeCanopy = isTreeCanopy,
            confidence = (greenRatio * 2f).coerceIn(0f, 1f)
        )
    }
}
