package com.example.ui.challenge

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.ui.components.MetricStatCard
import com.example.ui.theme.GrassGreenPrimary
import com.example.ui.viewmodel.TouchGrassViewModel
import com.example.utils.FunnyQuotes

@Composable
fun SuccessScreen(
    viewModel: TouchGrassViewModel,
    onContinue: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val record = state.lastCompletedRecord
    val durationSeconds = record?.durationSeconds ?: 6
    val quote = remember { FunnyQuotes.getRandomSuccessQuote() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF071B0B),
                        Color(0xFF0F3116),
                        Color(0xFF041006)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("challenge_success_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Triumphant Nature Badge
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E7D32).copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🌱", fontSize = 56.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "🌱 GRASS TOUCHED",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Congratulations.\nYou have successfully interacted with nature.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFA5D6A7),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            // Stats Cards
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Duration",
                        value = "${durationSeconds}s",
                        icon = "⏱️"
                    )
                    MetricStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Grass Streak",
                        value = "${state.streak.currentStreak} days",
                        icon = "🔥"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Total Touched",
                        value = "${state.streak.totalChallengesCompleted}",
                        icon = "🌿"
                    )
                    MetricStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Brain Recalibrated",
                        value = "100%",
                        icon = "🧠"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Witty Quote Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x66183A20))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "\"$quote\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE8F5E9),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Continue Button
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("success_continue_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GrassGreenPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "🔓 Continue",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
