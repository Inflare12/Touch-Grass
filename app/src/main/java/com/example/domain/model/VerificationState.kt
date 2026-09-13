package com.example.domain.model

sealed class VerificationState {
    object Idle : VerificationState()
    object LiveCamera : VerificationState()
    object OutdoorDetected : VerificationState()
    object GrassDetected : VerificationState()
    object HandDetected : VerificationState()
    object HandApproaching : VerificationState()
    object ContactDetected : VerificationState()
    data class Holding(val remainingSeconds: Int, val progress: Float) : VerificationState()
    data class Verified(val durationSeconds: Int) : VerificationState()
    data class CheatingDetected(val reason: String) : VerificationState()
}

data class VisionMetrics(
    val greenRatio: Float = 0f,
    val skinRatio: Float = 0f,
    val motionScore: Float = 0f,
    val isOutdoorLighting: Boolean = false,
    val isRealGrassTexture: Boolean = false,
    val isLiveMotion: Boolean = false,
    val handGrassProximity: Float = 0f // 0.0 (far) to 1.0 (touching)
)
