package com.example.ui.challenge

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.camera.CameraManager
import com.example.domain.model.VerificationState
import com.example.ui.theme.GrassGreenPrimary
import com.example.ui.viewmodel.TouchGrassViewModel

@Composable
fun ChallengeScreen(viewModel: TouchGrassViewModel, onChallengeCompleted: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsState()
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        viewModel.setCameraPermissionGranted(granted)
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
        viewModel.startChallengeSession()
    }
    LaunchedEffect(state.currentVerificationState) {
        if (state.currentVerificationState is VerificationState.Verified) onChallengeCompleted()
    }

    val verifier = viewModel.contactVerifier
    val cameraManager = remember {
        CameraManager(context, verifier) { grass, hand, liveness ->
            viewModel.onFrameProcessed(grass, hand, liveness)
        }
    }
    DisposableEffect(Unit) { onDispose { cameraManager.shutdown() } }

    Box(Modifier.fillMaxSize().background(Color.Black).testTag("challenge_screen")) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        cameraManager.bindCamera(lifecycleOwner, this)
                    }
                }
            )
        } else {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("📷", fontSize = 54.sp)
                Spacer(Modifier.height(16.dp))
                Text("Camera Permission Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Touch Grass needs live camera access to verify the challenge. Frames are analyzed on-device and are not saved by the app.", textAlign = TextAlign.Center, color = Color(0xFFCFD8DC), fontSize = 14.sp)
                Spacer(Modifier.height(24.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, colors = ButtonDefaults.buttonColors(containerColor = GrassGreenPrimary)) {
                    Text("Grant Camera Permission")
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel, modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x66000000)).testTag("challenge_close_button")) {
                Icon(Icons.Default.Close, contentDescription = "Cancel Challenge", tint = Color.White)
            }
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0x88000000)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00E676)))
                    Spacer(Modifier.width(6.dp))
                    Text("ON-DEVICE VISION", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.currentVerificationState is VerificationState.CheatingDetected) {
                val reason = (state.currentVerificationState as VerificationState.CheatingDetected).reason
                Card(Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("anti_cheat_warning_card"), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F))) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(reason, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            if (state.currentVerificationState is VerificationState.Holding || state.currentVerificationState is VerificationState.ContactDetected) {
                val remainingSec = (state.currentVerificationState as? VerificationState.Holding)?.remainingSeconds
                    ?: state.settings.holdDurationSeconds
                val progress = (state.currentVerificationState as? VerificationState.Holding)?.progress ?: 0.05f
                Box(Modifier.padding(bottom = 16.dp).size(110.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), color = Color(0xFF69F0AE), strokeWidth = 8.dp, trackColor = Color(0x44FFFFFF))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$remainingSec", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
                        Text("HOLD CONTACT", color = Color(0xFFB9F6CA), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            Card(Modifier.fillMaxWidth().testTag("verification_checklist_card"), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xCC111E14))) {
                Column(Modifier.padding(18.dp)) {
                    val guidanceText = when (state.currentVerificationState) {
                        VerificationState.Idle -> "Initializing camera..."
                        VerificationState.LiveCamera -> "Move the camera slowly, then aim down at real grass..."
                        VerificationState.OutdoorDetected -> "Live scene detected! Point down at real grass 🌱"
                        VerificationState.GrassDetected -> "Grass detected! Bring your hand into view 🖐️"
                        VerificationState.HandDetected -> "Hand detected! Move down toward the grass 📏"
                        VerificationState.HandApproaching -> "Almost touching! Reach down and touch the grass 🌱"
                        VerificationState.ContactDetected -> "🖐️ → 🌱 CONTACT DETECTED! Hold still..."
                        is VerificationState.Holding -> "Hold contact with the grass..."
                        is VerificationState.Verified -> "🌱 Grass touched! Challenge complete!"
                        is VerificationState.CheatingDetected -> "Cheat detected! Follow real nature rules."
                    }
                    Text(guidanceText, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        HudCheckItem("Outdoor", "🌳", verifier.isOutdoorPassed)
                        HudCheckItem("Grass", "🌱", verifier.isGrassPassed)
                        HudCheckItem("Hand", "🖐️", verifier.isHandPassed)
                        HudCheckItem("Contact", "🤝", verifier.isContactPassed)
                    }
                }
            }
        }
    }
}

@Composable
fun HudCheckItem(label: String, icon: String, isPassed: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(if (isPassed) Color(0xFF2E7D32) else Color(0x44FFFFFF)).border(1.5.dp, if (isPassed) Color(0xFF81C784) else Color(0x33FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isPassed) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            else Text(icon, fontSize = 16.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = if (isPassed) Color(0xFFC8E6C9) else Color(0xFFB0BEC5), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
