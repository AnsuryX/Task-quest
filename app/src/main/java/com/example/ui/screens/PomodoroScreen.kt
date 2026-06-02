package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.example.data.model.Task
import com.example.ui.theme.*
import com.example.ui.viewmodel.QuestViewModel
import com.example.util.NotificationAndSoundHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    viewModel: QuestViewModel,
    modifier: Modifier = Modifier
) {
    val NeonCyan = getDynamicCyan()
    val NeonPurple = getDynamicPurple()
    val NeonAmber = getDynamicAmber()
    val NeonRose = getDynamicRose()
    val NeonGreen = getDynamicGreen()

    val tasks by viewModel.tasks.collectAsState()
    val isTimerRunning = viewModel.isTimerRunning
    val countDownSeconds = viewModel.countDownSeconds
    val pomodoroMode = viewModel.pomodoroMode
    val selectedTaskId = viewModel.selectedTaskId

    val pendingTasks = tasks.filter { !it.completed }

    // Total Duration helper
    val totalSeconds = when (pomodoroMode) {
        "work" -> 25 * 60
        "short_break" -> 5 * 60
        "long_break" -> 15 * 60
        else -> 25 * 60
    }

    val progressFraction = if (totalSeconds > 0) {
        (countDownSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }

    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "timerProgress")

    val modeColor by animateColorAsState(
        targetValue = when (pomodoroMode) {
            "work" -> NeonPurple
            "short_break" -> NeonCyan
            "long_break" -> NeonAmber
            else -> NeonPurple
        },
        label = "modeColor"
    )

    var showTaskDropdown by remember { mutableStateOf(false) }
    val activeFocusTask = pendingTasks.find { it.id == selectedTaskId }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 90.dp, top = 8.dp)
    ) {
        // State Banner Modifiers
        item {
            Text(
                text = "Focus Chamber",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = TextAlign.Start
            )
        }

        // Mode Navigation Switches
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val modes = listOf(
                    Triple("work", "Work Epoch", NeonPurple),
                    Triple("short_break", "Short Rest", NeonCyan),
                    Triple("long_break", "Long Rest", NeonAmber)
                )
                modes.forEach { (mode, title, color) ->
                    val isSel = pomodoroMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) color.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { viewModel.setTimerMode(mode) }
                            .padding(vertical = 10.dp)
                            .testTag("mode_tab_$mode"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isSel) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Timer Dial
        item {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track Glow Circle
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    modeColor.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = modeColor,
                    strokeWidth = 10.dp,
                    trackColor = modeColor.copy(alpha = 0.12f),
                    strokeCap = StrokeCap.Round
                )

                // Countdown Labels Inside Inner bounds
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val minutes = countDownSeconds / 60
                    val seconds = countDownSeconds % 60
                    val timerText = String.format("%02d:%02d", minutes, seconds)
                    
                    Text(
                        text = timerText,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 44.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when (pomodoroMode) {
                            "work" -> "COGNITIVE FOCUS"
                            "short_break" -> "ENERGY BREATHING"
                            "long_break" -> "CHILL INTEGRATION"
                            else -> "READY"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        color = modeColor
                    )
                }
            }
        }

        // Focus Zones Shield Switch
        item {
            val isSuppressionActive = viewModel.isSuppressionActive
            val isShieldEnabled = viewModel.isFocusZoneEnabled

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSuppressionActive) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSuppressionActive) NeonCyan else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isSuppressionActive) "🛡️" else "📳", fontSize = 24.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Focus Shield (DND Zone)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (isSuppressionActive) "Alert distraction barrier ACTIVE" else "Blocks notification triggers & grants 3x XP multiplier",
                                    fontSize = 11.sp,
                                    color = if (isSuppressionActive) NeonCyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Switch(
                            checked = isShieldEnabled,
                            onCheckedChange = { viewModel.isFocusZoneEnabled = it },
                            modifier = Modifier.testTag("focus_shield_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonCyan
                            )
                        )
                    }

                    if (isSuppressionActive) {
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = "Shield Active",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "OUTSIDE NOTIFICATIONS SILENCED • Focus Zone Engaged",
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    // Goal displaying inside Focus Zone
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                "CURRENT CONCENTRATION OBJECTIVE:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = modeColor
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = activeFocusTask?.title ?: "No target linked. Select target quest below to optimize rewards!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeFocusTask != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Native permission status checks and shortcut action prompts
                    val context = LocalContext.current
                    val isDndPermissionGranted = NotificationAndSoundHelper.isDndPermissionGranted(context)
                    val isNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

                    if (isShieldEnabled && !isDndPermissionGranted) {
                        Spacer(Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "⚠️ Notification Policy Access Needed",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "To let Focus Shield automatically silence notification alerts on your device, please grant 'Notification Policy Access' in settings.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        NotificationAndSoundHelper.requestDndPermission(context)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp).align(Alignment.End)
                                ) {
                                    Text("Authorize Shield", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    if (!isNotificationsEnabled) {
                        Spacer(Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "🔔 Notifications Are Blocked",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Timer complete alerts and sound triggers are blocked. Please allow notifications in system settings to receive bells & notification banners.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                                            context.startActivity(fallbackIntent)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp).align(Alignment.End)
                                ) {
                                    Text("Enable Alerts", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Toggles / Controllers Flow
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                IconButton(
                    onClick = { viewModel.resetTimer() },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("reset_timer_button")
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset Timer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Play / Pause Action Button
                Button(
                    onClick = { viewModel.toggleTimer() },
                    modifier = Modifier
                        .height(60.dp)
                        .width(150.dp)
                        .testTag("toggle_timer_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = modeColor,
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isTimerRunning) "Pause" else "Start",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.background
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isTimerRunning) "Pause" else "Start",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.background
                        )
                    }
                }

                // Skip Mode Button
                IconButton(
                    onClick = {
                        if (pomodoroMode == "work") {
                            viewModel.setTimerMode("short_break")
                        } else {
                            viewModel.setTimerMode("work")
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("skip_timer_button")
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Skip Mode",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Task Binder Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Focus Target Bind",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = modeColor
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Link active focus intervals to an Eisenhower quest. Double XP is granted on completed targets, along with sector points.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                            .clickable { showTaskDropdown = !showTaskDropdown }
                            .padding(12.dp)
                            .testTag("bind_task_selector")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (activeFocusTask != null) getSectorIcon(activeFocusTask.sector) else Icons.Default.Adjust,
                                    contentDescription = null,
                                    tint = if (activeFocusTask != null) getSectorColor(activeFocusTask.sector) else modeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = activeFocusTask?.title ?: "Select Target Quest Epic",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (activeFocusTask != null) "Sector: ${activeFocusTask.sector} • Reward: +${activeFocusTask.xpReward} XP" else "Click to choose from active targets",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    }

                    // Task Dropdown Dialog Simulation Sheets
                    if (showTaskDropdown) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Select Quest Link Target:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        if (pendingTasks.isEmpty()) {
                            Text(
                                "No active quests found in your Quest Log. Forge some tasks first!",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            pendingTasks.forEach { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.selectedTaskId = task.id
                                            showTaskDropdown = false
                                        }
                                        .padding(10.dp)
                                        .testTag("select_bind_task_${task.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        getSectorIcon(task.sector),
                                        contentDescription = task.sector,
                                        tint = getSectorColor(task.sector),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = task.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Q${task.matrixQuadrant}",
                                        fontSize = 10.sp,
                                        color = getSectorColor(task.sector),
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
