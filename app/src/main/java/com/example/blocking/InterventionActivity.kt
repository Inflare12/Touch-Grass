package com.example.blocking

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.TouchGrassApp
import com.example.ui.theme.GrassGreenPrimary
import com.example.ui.theme.TouchGrassTheme
import com.example.utils.FunnyQuotes
import com.example.utils.SoundVibrationHelper
import kotlinx.coroutines.launch

class InterventionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val targetAppName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Scrolling App"
        val scrollingMinutes = intent.getIntExtra(EXTRA_SCROLLING_MINUTES, 0)

        setContent {
            TouchGrassTheme(darkTheme = true) {
                InterventionContent(
                    appName = targetAppName,
                    scrollingMinutes = scrollingMinutes,
                    onStartChallenge = {
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(MainActivity.EXTRA_START_CHALLENGE, true)
                            putExtra(EXTRA_PACKAGE_NAME, targetPackage)
                            putExtra(EXTRA_APP_NAME, targetAppName)
                        })
                        finish()
                    },
                    onWatchAdBypass = {
                        SoundVibrationHelper(this).apply {
                            vibrateSuccess()
                            release()
                        }
                        finish()
                    },
                    onGoHome = {
                        startActivity(Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_SCROLLING_MINUTES = "extra_scrolling_minutes"
    }
}

@Composable
fun InterventionContent(appName: String, scrollingMinutes: Int, onStartChallenge: () -> Unit, onWatchAdBypass: () -> Unit, onGoHome: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isShowingAdDialog by remember { mutableStateOf(false) }
    val funnyQuote = remember { FunnyQuotes.getRandomBlockedQuote() }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF09140B), Color(0xFF0E2214), Color(0xFF050B06)))).padding(24.dp).testTag("intervention_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(88.dp).clip(CircleShape).background(Color(0xFF2E7D32).copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lock, contentDescription = "Screen Lock", tint = Color(0xFF81C784), modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("🔒 TOUCH GRASS", fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("You've been on $appName for $scrollingMinutes minutes.", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color(0xFFA5D6A7), textAlign = TextAlign.Center)
            Text("Your phone needs a break.", fontSize = 15.sp, color = Color(0xFFC8E6C9).copy(alpha = 0.8f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(20.dp))

            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF15281A)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\"$funnyQuote\"", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE8F5E9), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(32.dp))

            Button(onClick = onStartChallenge, Modifier.fillMaxWidth().height(56.dp).testTag("intervention_touch_grass_button"), colors = ButtonDefaults.buttonColors(containerColor = GrassGreenPrimary), shape = RoundedCornerShape(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Spa, contentDescription = null, modifier = Modifier.size(22.dp)); Spacer(Modifier.size(8.dp)); Text("🌱 Touch Grass Now", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = { isShowingAdDialog = true }, Modifier.fillMaxWidth().height(52.dp).testTag("intervention_bypass_button"), shape = RoundedCornerShape(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = Color(0xFFB0BEC5), modifier = Modifier.size(20.dp)); Spacer(Modifier.size(8.dp)); Text("📺 Watch an ad to bypass", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFECEFF1))
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onGoHome, Modifier.fillMaxWidth().height(48.dp).testTag("intervention_put_phone_down_button"), shape = RoundedCornerShape(16.dp)) {
                Text("Put phone down & walk outside", fontSize = 13.sp, color = Color(0xFF81C784))
            }
        }

        if (isShowingAdDialog) {
            com.example.ads.RewardedAdDialog(
                appName = appName,
                onDismiss = { isShowingAdDialog = false },
                onRewardEarned = {
                    coroutineScope.launch {
                        TouchGrassApp.instance.repository.recordAdBypass(targetAppName = appName)
                        isShowingAdDialog = false
                        onWatchAdBypass()
                    }
                }
            )
        }
    }
}
