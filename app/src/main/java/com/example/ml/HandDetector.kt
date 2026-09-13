package com.example.ml

import androidx.camera.core.ImageProxy

data class HandDetectionResult(
    val isHandDetected: Boolean,
    val handCentroidX: Float = 0.5f,
    val handCentroidY: Float = 0.5f,
    val handSkinRatio: Float = 0f,
    val isMovingDownTowardsGrass: Boolean = false,
    val confidence: Float = 0f
)

interface HandDetector {
    fun detectHand(imageProxy: ImageProxy): HandDetectionResult
}
