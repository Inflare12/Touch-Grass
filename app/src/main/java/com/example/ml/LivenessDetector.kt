package com.example.ml

import androidx.camera.core.ImageProxy

data class LivenessResult(
    val isLiveEnvironment: Boolean,
    val motionScore: Float,
    val isStaticPhoto: Boolean,
    val isScreenReplay: Boolean,
    val warningMessage: String? = null
)

interface LivenessDetector {
    fun checkLiveness(imageProxy: ImageProxy): LivenessResult
}
