package com.example

import com.example.domain.model.VerificationState
import com.example.ml.ContactVerifier
import com.example.ml.GrassDetectionResult
import com.example.ml.HandDetectionResult
import com.example.ml.LivenessResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchGrassVerificationTest {
    private fun live() = LivenessResult(
        isLiveEnvironment = true,
        motionScore = 0.02f,
        isStaticPhoto = false,
        isScreenReplay = false
    )

    private fun grass() = GrassDetectionResult(
        isGrassDetected = true,
        greenRatio = 0.35f,
        bottomGreenRatio = 0.25f,
        confidence = 0.7f
    )

    @Test
    fun contact_does_not_verify_without_downward_approach() {
        val verifier = ContactVerifier(requiredHoldSeconds = 1)
        verifier.start()

        repeat(8) {
            verifier.processFrame(
                grass(),
                HandDetectionResult(isHandDetected = true, handCentroidY = 0.70f, isMovingDownTowardsGrass = false),
                live()
            )
        }

        assertFalse(verifier.currentState is VerificationState.Verified)
        assertFalse(verifier.isContactPassed)
    }

    @Test
    fun contact_requires_repeated_approach_frames() {
        val verifier = ContactVerifier(requiredHoldSeconds = 1)
        verifier.start()

        repeat(6) {
            verifier.processFrame(
                grass(),
                HandDetectionResult(isHandDetected = true, handCentroidY = 0.50f, isMovingDownTowardsGrass = true),
                live()
            )
        }

        repeat(3) {
            verifier.processFrame(
                grass(),
                HandDetectionResult(isHandDetected = true, handCentroidY = 0.65f, isMovingDownTowardsGrass = true),
                live()
            )
        }

        assertTrue(verifier.isContactPassed)
        assertTrue(verifier.currentState is VerificationState.ContactDetected || verifier.currentState is VerificationState.Holding || verifier.currentState is VerificationState.Verified)
    }

    @Test
    fun static_photo_is_rejected_before_any_contact_logic() {
        val verifier = ContactVerifier()
        verifier.start()
        val state = verifier.processFrame(
            grass(),
            HandDetectionResult(isHandDetected = true, handCentroidY = 0.9f, isMovingDownTowardsGrass = true),
            LivenessResult(false, 0f, true, false)
        )
        assertTrue(state is VerificationState.CheatingDetected)
    }
}
