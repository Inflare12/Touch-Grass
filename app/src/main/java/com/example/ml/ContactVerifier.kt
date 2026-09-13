package com.example.ml

import com.example.domain.model.VerificationState
import com.example.utils.FunnyQuotes

class ContactVerifier(
    private val requiredHoldSeconds: Int = 3
) {

    var currentState: VerificationState = VerificationState.Idle
        private set

    var cheatAttempts: Int = 0
        private set

    private var holdStartTimeMillis: Long = 0L
    private var verificationStartTimeMillis: Long = 0L

    var isOutdoorPassed: Boolean = false
        private set
    var isGrassPassed: Boolean = false
        private set
    var isHandPassed: Boolean = false
        private set
    var isContactPassed: Boolean = false
        private set

    fun reset() {
        currentState = VerificationState.Idle
        isOutdoorPassed = false
        isGrassPassed = false
        isHandPassed = false
        isContactPassed = false
        holdStartTimeMillis = 0L
        verificationStartTimeMillis = System.currentTimeMillis()
    }

    fun start() {
        reset()
        currentState = VerificationState.LiveCamera
    }

    fun processFrame(
        grassResult: GrassDetectionResult,
        handResult: HandDetectionResult,
        livenessResult: LivenessResult
    ): VerificationState {
        // If already verified, stay verified
        if (currentState is VerificationState.Verified) {
            return currentState
        }

        // 1. Anti-cheat check
        if (livenessResult.isStaticPhoto) {
            cheatAttempts++
            currentState = VerificationState.CheatingDetected("❌ Live environment required. Photos don't count.")
            return currentState
        }

        if (grassResult.isArtificialTurf) {
            cheatAttempts++
            currentState = VerificationState.CheatingDetected("❌ That appears to be artificial turf. Find real soil!")
            return currentState
        }

        if (grassResult.isTreeCanopy) {
            cheatAttempts++
            currentState = VerificationState.CheatingDetected("🌳 Tree detected. That's a tree, not grass!")
            return currentState
        }

        // 2. Check Liveness / Outdoor environment
        if (livenessResult.isLiveEnvironment) {
            isOutdoorPassed = true
        }

        // 3. Grass detection
        if (grassResult.isGrassDetected) {
            isGrassPassed = true
        }

        // 4. Hand detection
        if (handResult.isHandDetected) {
            isHandPassed = true
        }

        // 5. Hand approaching vs contact
        val isHandCloseToGrass = isGrassPassed && isHandPassed && (handResult.handCentroidY > 0.45f)
        val isContact = isGrassPassed && isHandPassed && (handResult.handCentroidY > 0.58f)

        if (isContact) {
            isContactPassed = true
        }

        // State Machine Transition Logic
        when (currentState) {
            is VerificationState.Idle -> {
                currentState = VerificationState.LiveCamera
            }
            is VerificationState.LiveCamera -> {
                if (isOutdoorPassed) {
                    currentState = VerificationState.OutdoorDetected
                }
            }
            is VerificationState.OutdoorDetected -> {
                if (isGrassPassed) {
                    currentState = VerificationState.GrassDetected
                }
            }
            is VerificationState.GrassDetected -> {
                if (isHandPassed) {
                    currentState = VerificationState.HandDetected
                }
            }
            is VerificationState.HandDetected -> {
                if (isContact) {
                    currentState = VerificationState.ContactDetected
                    holdStartTimeMillis = System.currentTimeMillis()
                } else if (isHandCloseToGrass) {
                    currentState = VerificationState.HandApproaching
                }
            }
            is VerificationState.HandApproaching -> {
                if (isContact) {
                    currentState = VerificationState.ContactDetected
                    holdStartTimeMillis = System.currentTimeMillis()
                } else if (!isHandPassed) {
                    currentState = VerificationState.GrassDetected
                }
            }
            is VerificationState.ContactDetected -> {
                if (isContact) {
                    if (holdStartTimeMillis == 0L) holdStartTimeMillis = System.currentTimeMillis()
                    val elapsedMillis = System.currentTimeMillis() - holdStartTimeMillis
                    val totalHoldMillis = requiredHoldSeconds * 1000L
                    val remainingSeconds = (Math.ceil((totalHoldMillis - elapsedMillis) / 1000.0)).toInt().coerceAtLeast(1)
                    val progress = (elapsedMillis.toFloat() / totalHoldMillis).coerceIn(0f, 1f)

                    if (elapsedMillis >= totalHoldMillis) {
                        val durationSeconds = ((System.currentTimeMillis() - verificationStartTimeMillis) / 1000L).toInt().coerceAtLeast(1)
                        currentState = VerificationState.Verified(durationSeconds)
                    } else {
                        currentState = VerificationState.Holding(remainingSeconds, progress)
                    }
                } else {
                    // Contact broken
                    holdStartTimeMillis = 0L
                    currentState = VerificationState.HandApproaching
                }
            }
            is VerificationState.Holding -> {
                if (isContact) {
                    val elapsedMillis = System.currentTimeMillis() - holdStartTimeMillis
                    val totalHoldMillis = requiredHoldSeconds * 1000L
                    val remainingSeconds = (Math.ceil((totalHoldMillis - elapsedMillis) / 1000.0)).toInt().coerceAtLeast(1)
                    val progress = (elapsedMillis.toFloat() / totalHoldMillis).coerceIn(0f, 1f)

                    if (elapsedMillis >= totalHoldMillis) {
                        val durationSeconds = ((System.currentTimeMillis() - verificationStartTimeMillis) / 1000L).toInt().coerceAtLeast(1)
                        currentState = VerificationState.Verified(durationSeconds)
                    } else {
                        currentState = VerificationState.Holding(remainingSeconds, progress)
                    }
                } else {
                    // Contact slipped off
                    holdStartTimeMillis = 0L
                    currentState = VerificationState.HandApproaching
                }
            }
            is VerificationState.CheatingDetected -> {
                // Recover if conditions normalized
                if (!grassResult.isTreeCanopy && !grassResult.isArtificialTurf && !livenessResult.isStaticPhoto) {
                    currentState = if (isContact) VerificationState.ContactDetected else VerificationState.GrassDetected
                }
            }
            is VerificationState.Verified -> {}
        }

        return currentState
    }
}
