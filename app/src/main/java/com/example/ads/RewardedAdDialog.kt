package com.example.ads

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GrassGreenPrimary

@Composable
fun RewardedAdDialog(
    appName: String,
    adManager: RewardedAdManager,
    onDismiss: () -> Unit,
    onRewardEarned: () -> Unit
) {
    var status by remember { mutableStateOf("Watch the full rewarded ad. The bypass is granted only after the ad network confirms the reward.") }
    var launching by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!launching) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = GrassGreenPrimary)
                Spacer(Modifier.size(8.dp))
                Text("Take the ad route")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Watch one rewarded ad to unlock 10 minutes of access for $appName.", fontWeight = FontWeight.SemiBold)
                Text(status, fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                if (!adManager.isAdLoaded()) {
                    Text("Ad is still loading. If it is unavailable, Touch Grass remains available.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !launching,
                onClick = {
                    val activity = adManager.activityForShowing()
                    if (activity == null) {
                        status = "Unable to open the ad from this screen."
                        return@Button
                    }
                    if (!adManager.isAdLoaded()) {
                        status = "The ad is still loading. Please try again in a moment, or touch grass instead."
                        return@Button
                    }
                    launching = true
                    adManager.showRewardedAd(
                        onRewardEarned = { onRewardEarned() },
                        onAdClosed = {
                            launching = false
                            status = "Ad closed. No bypass is granted unless the network confirmed the reward."
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GrassGreenPrimary)
            ) { Text(if (launching) "Playing…" else "Watch ad") }
        },
        dismissButton = { TextButton(enabled = !launching, onClick = onDismiss) { Text("Cancel") } }
    )
}
