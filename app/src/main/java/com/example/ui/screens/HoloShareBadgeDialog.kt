package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserStats
import com.example.data.model.Task
import com.example.ui.theme.*

@Composable
fun HoloShareBadgeDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    userStats: UserStats?,
    tasksCompletedToday: Int,
    focusMinutesToday: Int
) {
    if (!show) return

    val context = LocalContext.current
    val level = userStats?.level ?: 1
    val currentXp = userStats?.xp ?: 0
    val xpNeeded = userStats?.xpForNextLevel ?: 250
    val totalQuests = userStats?.questsCompleted ?: 0
    val totalFocus = userStats?.focusMinutesTotal ?: 0
    val activeStreak = userStats?.streakDays ?: 1
    
    // Choose RPG Title based on level
    val rpgClass = remember(level) {
        when {
            level >= 10 -> "Chronos Arch-Mage"
            level >= 6 -> "Eisenhower Paladin"
            level >= 4 -> "Quantum Ranger"
            level >= 2 -> "Acolyte of Order"
            else -> "Initiate Vagabond"
        }
    }

    val xpProgress = remember(currentXp, xpNeeded) {
        if (xpNeeded > 0) (currentXp.toFloat() / xpNeeded.toFloat()).coerceIn(0f, 1f) else 0f
    }

    val gradientBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E1F30),
                Color(0xFF0F101A)
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .wrapContentHeight()
                .border(
                    BorderStroke(2.dp, Brush.horizontalGradient(listOf(NeonCyan, NeonPurple))),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("holo_share_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F101A),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .background(gradientBrush)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌌 HOLOGRAPHIC ID",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = NeonCyan,
                        letterSpacing = 1.5.sp
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .size(32.dp)
                            .testTag("holo_dialog_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // The Collector Card Boundary Box (The Visual Shareable)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF2A2D40), RoundedCornerShape(16.dp))
                        .background(Color(0xFF151724), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // RPG Insignia Avatar Ring
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .border(
                                    BorderStroke(2.dp, Brush.radialGradient(listOf(NeonCyan, NeonPurple))),
                                    CircleShape
                                )
                                .background(Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1C1D30))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🦉", fontSize = 38.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Character Name / Class Title
                        Text(
                            text = "HERO ANONYMOUS",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = "RANK: $rpgClass".uppercase(),
                            color = NeonAmber,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        // Stats Summary Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0C0E18), RoundedCornerShape(10.dp))
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("LEVEL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text("$level", fontSize = 16.sp, fontWeight = FontWeight.Black, color = NeonCyan)
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF23D573).copy(alpha = 0.2f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("STREAK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text("🔥 $activeStreak", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Quadrant1Color)
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF23D573).copy(alpha = 0.2f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("FOCUS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text("$totalFocus m", fontSize = 16.sp, fontWeight = FontWeight.Black, color = NeonPurple)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Progress to next rank
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("EXPERIENCE PROGRESSION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text("$currentXp / $xpNeeded XP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { xpProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = NeonPurple,
                                trackColor = Color(0xFF0C0E18),
                                strokeCap = StrokeCap.Round
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Daily Feates Checklist Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🛡️ TODAY'S CHRONICLES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFF2A2D40)))
                        }

                        Spacer(Modifier.height(8.dp))

                        // Daily stats: Tasks completed today & Focus today
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Quests Completed Today:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            Text("$tasksCompletedToday Completed ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pomodoro Chamber Focus Today:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            Text("$focusMinutesToday Mins 🧘", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                        }

                        Spacer(Modifier.height(16.dp))

                        // Chronos Inspirational Signature Prompt
                        Text(
                            text = "\"A true Time Guardian designs life symmetrically. Aligning Q2 Important goals shapes the trajectory of our futures.\"",
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Actions: Copy Clipboard / Share Layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF2A2D40))
                    ) {
                        Text("Dismiss", color = Color.White)
                    }

                    Button(
                        onClick = {
                            // Construct elegant copyable block
                            val visualBar = "█".repeat((xpProgress * 10).toInt()) + "░".repeat(10 - (xpProgress * 10).toInt())
                            val copyText = """
                                🌌 COMPASS TEMPORAL RECONCILIATION 🌌
                                ==============================
                                HERO CLASS: LEVEL $level $rpgClass
                                🔥 STREAK: $activeStreak Days
                                🧘 TODAY'S FOCUS: $focusMinutesToday min
                                ⚔️ TODAY'S QUESTS: $tasksCompletedToday Cleared
                                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                                EXPERIENCE: [$visualBar] ${(xpProgress * 100).toInt()}%
                                total focus: $totalFocus min | total quests: $totalQuests
                                ==============================
                                "A true Time Guardian designs life symmetrically. Aligning Q2 Important goals shapes the trajectory of our futures."
                                Sync your goals with Ansury Quest!
                            """.trimIndent()

                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Ranger Character Badge", copyText)
                            clipboard.setPrimaryClip(clip)

                            Toast.makeText(context, "Temporal Credentials copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("copy_holo_id_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy ID Code", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
