package com.example.ml

import androidx.camera.core.ImageProxy

data class GrassDetectionResult(
    val isGrassDetected: Boolean,
    val greenRatio: Float,
    val bottomGreenRatio: Float = 0f,
    val isArtificialTurf: Boolean = false,
    val isTreeCanopy: Boolean = false,
    val confidence: Float = 0f
)

interface GrassDetector {
    fun detectGrass(imageProxy: ImageProxy): GrassDetectionResult
}
