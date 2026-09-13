package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.VerificationState
import com.example.ml.ContactVerifier
import com.example.ml.GrassDetectionResult
import com.example.ml.HandDetectionResult
import com.example.ml.LivenessResult
import com.example.utils.FunnyQuotes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Touch Grass", appName)
    }

    @Test
    fun `funny quotes return valid strings`() {
        val blockedQuote = FunnyQuotes.getRandomBlockedQuote()
        val cheatQuote = FunnyQuotes.getRandomCheatQuote()
        val successQuote = FunnyQuotes.getRandomSuccessQuote()

        assertNotNull(blockedQuote)
        assertNotNull(cheatQuote)
        assertNotNull(successQuote)
        assertTrue(blockedQuote.isNotEmpty())
    }

    @Test
    fun `verifier detects static photo cheating`() {
        val verifier = ContactVerifier(requiredHoldSeconds = 3)
        verifier.start()

        val state = verifier.processFrame(
            grassResult = GrassDetectionResult(isGrassDetected = true, greenRatio = 0.5f),
            handResult = HandDetectionResult(isHandDetected = true),
            livenessResult = LivenessResult(
                isLiveEnvironment = false,
                motionScore = 0f,
                isStaticPhoto = true,
                isScreenReplay = false
            )
        )

        assertTrue(state is VerificationState.CheatingDetected)
        assertEquals(1, verifier.cheatAttempts)
    }

    @Test
    fun `verifier detects artificial turf cheating`() {
        val verifier = ContactVerifier(requiredHoldSeconds = 3)
        verifier.start()

        val state = verifier.processFrame(
            grassResult = GrassDetectionResult(isGrassDetected = false, greenRatio = 0.5f, isArtificialTurf = true),
            handResult = HandDetectionResult(isHandDetected = true),
            livenessResult = LivenessResult(
                isLiveEnvironment = true,
                motionScore = 0.05f,
                isStaticPhoto = false,
                isScreenReplay = false
            )
        )

        assertTrue(state is VerificationState.CheatingDetected)
    }
}

