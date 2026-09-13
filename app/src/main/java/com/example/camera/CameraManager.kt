package com.example.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.ml.ContactVerifier
import com.example.ml.GrassDetectionResult
import com.example.ml.HandDetectionResult
import com.example.ml.LivenessResult
import com.example.ml.LocalGrassDetector
import com.example.ml.LocalHandDetector
import com.example.ml.LocalLivenessDetector
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val contactVerifier: ContactVerifier,
    private val onFrameProcessed: (
        grass: GrassDetectionResult,
        hand: HandDetectionResult,
        liveness: LivenessResult
    ) -> Unit
) {

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val grassDetector = LocalGrassDetector()
    private val handDetector = LocalHandDetector()
    private val livenessDetector = LocalLivenessDetector()

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    val grass = grassDetector.detectGrass(imageProxy)
                    val hand = handDetector.detectHand(imageProxy)
                    val liveness = livenessDetector.checkLiveness(imageProxy)

                    contactVerifier.processFrame(grass, hand, liveness)

                    onFrameProcessed(grass, hand, liveness)
                } catch (_: Exception) {
                } finally {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}
