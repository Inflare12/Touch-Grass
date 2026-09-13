package com.example.ml

import com.example.domain.model.VerificationState

/**
 * Conservative temporal verifier. The current local vision implementation is heuristic,
 * so verification requires multiple independent signals instead of trusting one frame.
 */
class ContactVerifier(
    private val requiredHoldSeconds: Int = 3
) {
    var currentState: VerificationState = VerificationState.Idle
        private set
    var cheatAttempts: Int = 0
        private set

    private var holdStartTimeMillis = 0L
    private var verificationStartTimeMillis = 0L
    private var liveFrames = 0
    private var grassFrames = 0
    private var handFrames = 0
    private var approachFrames = 0

    var isOutdoorPassed = false
        private set
    var isGrassPassed = false
        private set
    var isHandPassed = false
        private set
    var isContactPassed = false
        private set

    fun reset() {
        currentState = VerificationState.Idle
        isOutdoorPassed = false
        isGrassPassed = false
        isHandPassed = false
        isContactPassed = false
        holdStartTimeMillis = 0L
        verificationStartTimeMillis = 0L
        liveFrames = 0
        grassFrames = 0
        handFrames = 0
        approachFrames = 0
        cheatAttempts = 0
    }

    fun start() {
        reset()
        verificationStartTimeMillis = System.currentTimeMillis()
        currentState = VerificationState.LiveCamera
    }

    fun processFrame(
        grassResult: GrassDetectionResult,
        handResult: HandDetectionResult,
        livenessResult: LivenessResult
    ): VerificationState {
        if (currentState is VerificationState.Verified) return currentState

        if (livenessResult.isStaticPhoto || livenessResult.isScreenReplay) {
            cheatAttempts++
            holdStartTimeMillis = 0L
            currentState = VerificationState.CheatingDetected(
                livenessResult.warningMessage ?: "❌ LIVE ENVIRONMENT REQUIRED."
            )
            return currentState
        }

        if (grassResult.isArtificialTurf) {
            cheatAttempts++
            currentState = VerificationState.CheatingDetected("❌ Artificial grass detected. Find real grass!")
            return currentState
        }

        if (grassResult.isTreeCanopy) {
            cheatAttempts++
            currentState = VerificationState.CheatingDetected("🌳 That's a tree. Point down at grass!")
            return currentState
        }

        if (livenessResult.isLiveEnvironment) liveFrames++
        if (grassResult.isGrassDetected) grassFrames++ else grassFrames = (grassFrames - 1).coerceAtLeast(0)
        if (handResult.isHandDetected) handFrames++ else handFrames = (handFrames - 1).coerceAtLeast(0)

        // Require temporal confirmation, not a single lucky frame.
        if (liveFrames >= 6) isOutdoorPassed = true
        if (grassFrames >= 4 && grassResult.bottomGreenRatio >= 0.08f) isGrassPassed = true
        if (handFrames >= 3) isHandPassed = true

        val hasGroundEvidence = grassResult.isGrassDetected && grassResult.bottomGreenRatio >= 0.08f
        val isHandCloseToGrass = isGrassPassed && isHandPassed && handResult.handCentroidY > 0.45f
        val isContact = isGrassPassed && isHandPassed &&
            hasGroundEvidence &&
            handResult.handCentroidY > 0.58f &&
            handResult.isMovingDownTowardsGrass

        if (handResult.isMovingDownTowardsGrass) approachFrames++ else approachFrames = (approachFrames - 1).coerceAtLeast(0)
        val confirmedApproach = approachFrames >= 2
        val confirmedContact = isContact && confirmedApproach && livenessResult.isLiveEnvironment
        if (confirmedContact) isContactPassed = true

        fun beginHold() {
            if (holdStartTimeMillis == 0L) holdStartTimeMillis = System.currentTimeMillis()
        }

        fun holdingState(): VerificationState {
            val elapsed = System.currentTimeMillis() - holdStartTimeMillis
            val total = requiredHoldSeconds.coerceAtLeast(1) * 1000L
            if (elapsed >= total) {
                val duration = ((System.currentTimeMillis() - verificationStartTimeMillis) / 1000L)
                    .toInt().coerceAtLeast(1)
                return VerificationState.Verified(duration)
            }
            val remaining = kotlin.math.ceil((total - elapsed) / 1000.0).toInt().coerceAtLeast(1)
            val progress = (elapsed.toFloat() / total).coerceIn(0f, 1f)
            return VerificationState.Holding(remaining, progress)
        }

        when (currentState) {
            VerificationState.Idle -> currentState = VerificationState.LiveCamera
            VerificationState.LiveCamera -> if (isOutdoorPassed) currentState = VerificationState.OutdoorDetected
            VerificationState.OutdoorDetected -> if (isGrassPassed) currentState = VerificationState.GrassDetected
            VerificationState.GrassDetected -> if (isHandPassed) currentState = VerificationState.HandDetected
            VerificationState.HandDetected -> when {
                confirmedContact -> {
                    beginHold()
                    currentState = VerificationState.ContactDetected
                }
                isHandCloseToGrass -> currentState = VerificationState.HandApproaching
            }
            VerificationState.HandApproaching -> when {
                confirmedContact -> {
                    beginHold()
                    currentState = VerificationState.ContactDetected
                }
                !isHandPassed -> currentState = VerificationState.GrassDetected
            }
            VerificationState.ContactDetected, is VerificationState.Holding -> {
                if (confirmedContact) {
                    beginHold()
                    currentState = holdingState()
                } else {
                    holdStartTimeMillis = 0L
                    isContactPassed = false
                    currentState = VerificationState.HandApproaching
                }
            }
            is VerificationState.CheatingDetected -> {
                if (!grassResult.isArtificialTurf && !grassResult.isTreeCanopy &&
                    !livenessResult.isStaticPhoto && !livenessResult.isScreenReplay) {
                    currentState = when {
                        confirmedContact -> VerificationState.ContactDetected
                        isHandPassed -> VerificationState.HandApproaching
                        isGrassPassed -> VerificationState.GrassDetected
                        isOutdoorPassed -> VerificationState.OutdoorDetected
                        else -> VerificationState.LiveCamera
                    }
                }
            }
            is VerificationState.Verified -> Unit
        }

        return currentState
    }
}
