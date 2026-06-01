package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserStats
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PomodoroScreen
import com.example.ui.screens.QuestLogScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.QuestViewModel
import com.example.ui.viewmodel.QuestViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: QuestViewModel by viewModels {
        QuestViewModelFactory((application as TaskQuestApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Dark mode state
            var isDarkTheme by remember { mutableStateOf(true) }

            TaskQuestTheme(darkTheme = isDarkTheme) {
                val stats by viewModel.userStats.collectAsState()
                var currentNavIndex by remember { mutableStateOf(0) } // 0 = Dashboard, 1 = Quest Log, 2 = Focus Chamber

                Scaffold(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    topBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Row 1: App Branding Title
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocalActivity,
                                        contentDescription = "TaskQuest Logo",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "TaskQuest",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                
                                // Reset stats button for gamified debugging
                                IconButton(
                                    onClick = { 
                                        // Just a nice gamified helper: tap app icon to refresh companion
                                        viewModel.triggerCompanionNudge() 
                                    },
                                    modifier = Modifier.testTag("app_logo_tap")
                                ) {
                                    Icon(
                                        Icons.Default.HelpOutline, 
                                        contentDescription = "Info",
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            // Row 2: Character RPG Stats Sheet
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = NeonAmber,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.padding(end = 8.dp)
                                            ) {
                                                Text(
                                                    text = "LVL ${stats?.level ?: 1}",
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.Black,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Text(
                                                text = "EXP: ${stats?.xp ?: 0} / ${stats?.xpForNextLevel ?: 250}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "🔥 ${stats?.streakDays ?: 1} Day Streak",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Quadrant1Color,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                            IconButton(
                                                onClick = { isDarkTheme = !isDarkTheme },
                                                modifier = Modifier.size(36.dp).testTag("theme_toggle")
                                            ) {
                                                Icon(
                                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                                    contentDescription = "Toggle Dark Mode",
                                                    tint = if (isDarkTheme) NeonAmber else NeonPurple,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    // XP Progress Indicators
                                    val expPercent = stats?.let {
                                        it.xp.toFloat() / it.xpForNextLevel.toFloat()
                                    } ?: 0f
                                    LinearProgressIndicator(
                                        progress = { expPercent.coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                        color = NeonCyan,
                                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                        strokeCap = StrokeCap.Round
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Focus: ${stats?.focusMinutesTotal ?: 0} min | 🛡️ Shield: ${stats?.totalFocusZoneMinutes ?: 0} min",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "Quests: ${stats?.questsCompleted ?: 0} done",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            // Row 3: Companion Dialogue Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("🦉", fontSize = 18.sp)
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "COMPANION CHRONOS:",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp,
                                                color = NeonCyan,
                                                letterSpacing = 0.5.sp
                                            )
                                            IconButton(
                                                onClick = { viewModel.triggerCompanionNudge() },
                                                modifier = Modifier.size(18.dp).testTag("refresh_companion_nudge")
                                            ) {
                                                Icon(
                                                    Icons.Default.Refresh,
                                                    contentDescription = "Trigger Nudge",
                                                    tint = NeonCyan,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "\"${viewModel.companionNudge}\"",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .testTag("bottom_navigation_bar"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentNavIndex == 0,
                                onClick = { currentNavIndex = 0 },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("nav_dashboard")
                            )
                            NavigationBarItem(
                                selected = currentNavIndex == 1,
                                onClick = { currentNavIndex = 1 },
                                icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "Quest Book") },
                                label = { Text("Quest Book", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("nav_quest_book")
                            )
                            NavigationBarItem(
                                selected = currentNavIndex == 2,
                                onClick = { currentNavIndex = 2 },
                                icon = { Icon(Icons.Default.Timer, contentDescription = "Focus Chamber") },
                                label = { Text("Focus Chamber", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("nav_focus")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedVisibility(
                            visible = currentNavIndex == 0,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            DashboardScreen(viewModel = viewModel)
                        }

                        // Shift to Matrix Planner Screen
                        AnimatedVisibility(
                            visible = currentNavIndex == 1,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            QuestLogScreen(viewModel = viewModel)
                        }

                        // Shift to Focus Pomodoro screen
                        AnimatedVisibility(
                            visible = currentNavIndex == 2,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            PomodoroScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
