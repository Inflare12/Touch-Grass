package com.example.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GrassGreenPrimary

@Composable
fun RewardedAdDialog(
    appName: String,
    onDismiss: () -> Unit,
    onRewardEarned: () -> Unit
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("A real rewarded ad will open. You only get the bypass after the ad network confirms the reward.") }
    var launching by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!launching) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = GrassGreenPrimary)
                Spacer(Modifier.size(8.dp))
                Text("Watch an ad to bypass")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Watch one rewarded ad to unlock one bypass for $appName.",
                    fontWeight = FontWeight.SemiBold
                )
                Text(status, fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Button(
                enabled = !launching,
                onClick = {
                    val activity = context as? android.app.Activity
                    if (activity == null) {
                        status = "Unable to open the ad from this screen."
                        return@Button
                    }
                    launching = true
                    RewardedAdManager(activity).showRewardedAd(
                        onRewardEarned = {
                            onRewardEarned()
                        },
                        onAdClosed = {
                            launching = false
                            status = "Ad closed. No reward was granted unless the network confirmed it."
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GrassGreenPrimary)
            ) {
                Text(if (launching) "Opening…" else "Watch ad")
            }
        },
        dismissButton = {
            TextButton(enabled = !launching, onClick = onDismiss) { Text("Cancel") }
        }
    )
}
