package com.example.camera

import android.content.Context
import android.util.Log
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

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
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
                        ContextCompat.getMainExecutor(context).execute {
                            onFrameProcessed(grass, hand, liveness)
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "Frame analysis failed", t)
                    } finally {
                        imageProxy.close()
                    }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (t: Throwable) {
                Log.e(TAG, "Unable to bind camera", t)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun shutdown() {
        cameraExecutor.shutdownNow()
    }

    private companion object {
        const val TAG = "TouchGrassCamera"
    }
}
