package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.launch
import com.example.ui.theme.*
import com.example.ui.viewmodel.QuestViewModel
import com.example.util.NotificationAndSoundHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: QuestViewModel,
    onDismiss: () -> Unit
) {
    val NeonCyan = getDynamicCyan()
    val NeonPurple = getDynamicPurple()
    val NeonAmber = getDynamicAmber()
    val NeonRose = getDynamicRose()
    val NeonGreen = getDynamicGreen()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var hasCalendarPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val calendarPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val writeGranted = permissions[android.Manifest.permission.WRITE_CALENDAR] ?: false
        val readGranted = permissions[android.Manifest.permission.READ_CALENDAR] ?: false
        if (writeGranted && readGranted) {
            hasCalendarPermission = true
            viewModel.saveCalendarSyncSetting(context, true)
            Toast.makeText(context, "Google Calendar Auto Sync Enabled!", Toast.LENGTH_SHORT).show()
        } else {
            hasCalendarPermission = false
            viewModel.saveCalendarSyncSetting(context, false)
            Toast.makeText(context, "Calendar permissions are required for background sync.", Toast.LENGTH_SHORT).show()
        }
    }

    var activeTab by remember { mutableStateOf(0) } // 0 = Audio & Alert Permissions, 1 = Cloud Sync Account, 2 = Scroll Backup

    // Form states for Account Sync Registration
    var registerEmail by remember { mutableStateOf("") }
    var registerName by remember { mutableStateOf("") }
    var registerPassword by remember { mutableStateOf("") }

    // Text backup string state
    var textBackupAreaInput by remember { mutableStateOf("") }

    // Read initial DND and notification status
    var isDndGranted by remember { mutableStateOf(NotificationAndSoundHelper.isDndPermissionGranted(context)) }
    var areNotificationsEnabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }

    // Poll permission updates slightly on active display
    LaunchedEffect(Unit) {
        viewModel.loadPreferences(context)
        isDndGranted = NotificationAndSoundHelper.isDndPermissionGranted(context)
        areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(16.dp)
            ) {
                // Header Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = NeonAmber,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Castle Armory & Sync Portal",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dismiss_settings_dialog")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close Settings",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val tabModifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp)
                    
                    Column(
                        modifier = tabModifier
                            .clickable { activeTab = 0 }
                            .testTag("settings_tab_alerts"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Visage & Chimes",
                            tint = if (activeTab == 0) NeonAmber else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Visage & Chimes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 0) NeonAmber else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Column(
                        modifier = tabModifier
                            .clickable { activeTab = 1 }
                            .testTag("settings_tab_cloud"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = "Cloud",
                            tint = if (activeTab == 1) NeonCyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Cloud Sync",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 1) NeonCyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Column(
                        modifier = tabModifier
                            .clickable { activeTab = 2 }
                            .testTag("settings_tab_scrolls"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = "Scrolls",
                            tint = if (activeTab == 2) NeonPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Vault Scrolls",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 2) NeonPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Scrollable Content Pane
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        when (activeTab) {
                            0 -> {
                                // --- APP VISAGE & LIGHT SYSTEM THEME ---
                                Text(
                                    "🎨 KINGDOM VISAGE (THEME SELECTION)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "App Color Scheme Presets",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Toggle the application theme. Forge high contrast alignments for day and night cycles.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        
                                        Spacer(Modifier.height(14.dp))

                                        // 3 Theme Modes
                                        val modesList = listOf(
                                            Triple(0, "🛡️ Follow System Aegis", "Aligns dynamically with host device's theme settings"),
                                            Triple(1, "🕊️ Great White Mode", "Pristine, ultra high-contrast light mode for daytime matrix planning"),
                                            Triple(2, "🌌 Cosmic Obsidian", "Epic twilight universe dark mode to guard your eyes at night")
                                        )

                                        modesList.forEach { (modeIdx, title, doc) ->
                                            val isSelected = viewModel.appThemeMode == modeIdx
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                        else Color.Transparent
                                                    )
                                                    .clickable {
                                                        viewModel.saveThemeSetting(context, modeIdx)
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = {
                                                        viewModel.saveThemeSetting(context, modeIdx)
                                                    },
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = title,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = doc,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                // --- GOOGLE CALENDAR AUTOMATED SYNCHRONIZATION ---
                                Text(
                                    "📅 GOOGLE CALENDAR AUTOMATED BRIDGE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.CalendarMonth,
                                                    contentDescription = "Google Calendar",
                                                    tint = NeonCyan,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "Auto-Sync New Quests",
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Sync newly created tasks with Google Calendar",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                            Switch(
                                                checked = viewModel.isCalendarSyncEnabled,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        if (hasCalendarPermission) {
                                                            viewModel.saveCalendarSyncSetting(context, true)
                                                        } else {
                                                            calendarPermissionLauncher.launch(
                                                                arrayOf(
                                                                    android.Manifest.permission.READ_CALENDAR,
                                                                    android.Manifest.permission.WRITE_CALENDAR
                                                                )
                                                            )
                                                        }
                                                    } else {
                                                        viewModel.saveCalendarSyncSetting(context, false)
                                                    }
                                                },
                                                modifier = Modifier.testTag("calendar_sync_switch")
                                            )
                                        }

                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            text = "When activated, any newly enlisted quest is instantly integrated into your primary Google Calendar in the background, complete with notes, scheduled date, and XP rewards. You can also manual sync any quest by clicking the 📅 button on its card.",
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // --- AUDIO & NOTIFICATION SYSTEMS ---
                                Text(
                                    "🚨 FOCUS SHIELD & EMULATOR PERMISSIONS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonAmber,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                // Check notification permission
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Notification Alerts Status",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = if (areNotificationsEnabled) "🟢 ACTIVE" else "❌ BLOCKED",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                color = if (areNotificationsEnabled) NeonCyan else Color.Red
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "Critical for task chimes, level up banners, and interval completions directly inside the emulator workspace status bars.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        if (!areNotificationsEnabled) {
                                            Spacer(Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                    }
                                                    try {
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS))
                                                    }
                                                },
                                                modifier = Modifier.align(Alignment.End),
                                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                                            ) {
                                                Text("Enable Alerts in Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Check DND Policy access
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "DND Policy Suppression",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = if (isDndGranted) "🟢 AUTHORIZED" else "❌ UNAUTHORIZED",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                color = if (isDndGranted) NeonCyan else Color.Red
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "Allows Focus Shield DND to suppress physical hardware buzzes and sound streams during Pomodoro timers automatically.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Test alert buttons
                                            OutlinedButton(
                                                onClick = {
                                                    NotificationAndSoundHelper.sendNotification(
                                                        context = context,
                                                        channelId = NotificationAndSoundHelper.CHANNEL_ALERTS_ID,
                                                        notificationId = 999,
                                                        title = "🎮 Test Alert Fire!",
                                                        text = "TaskQuest Alerts sound is active. System sensors fully responsive!"
                                                    )
                                                    Toast.makeText(context, "Test Notification emitted!", Toast.LENGTH_SHORT).show()
                                                },
                                                border = BorderStroke(1.dp, NeonCyan)
                                            ) {
                                                Text("🔊 Test alert banner", fontSize = 11.sp, color = NeonCyan)
                                            }

                                            if (!isDndGranted) {
                                                Button(
                                                    onClick = {
                                                        NotificationAndSoundHelper.requestDndPermission(context)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                                                ) {
                                                    Text("Authorize DND Policy", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                // --- TIMER SESSION AUDIO CUSTOMIZATION ---
                                Text(
                                    "✨ CHOOSE POMODORO CYCLE CHIMES",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                val soundThemes = listOf(
                                    "Arcade Victory (Triumphant Beeps)",
                                    "Classic Digital (Crisp Beeper)",
                                    "Ethereal Zen (Mellow Gong Chime)",
                                    "Cosmic Wave (Hyper Pulse Engine)"
                                )

                                // Work Sound settings selector
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("🔥 Completed Work Session Alarm:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(Modifier.height(8.dp))
                                        soundThemes.forEachIndexed { idx, label ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.saveSoundSettings(context, idx, viewModel.breakSoundThemeSetting)
                                                    }
                                                    .padding(vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = viewModel.workSoundThemeSetting == idx,
                                                    onClick = {
                                                        viewModel.saveSoundSettings(context, idx, viewModel.breakSoundThemeSetting)
                                                    }
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(label, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                                
                                                if (viewModel.workSoundThemeSetting == idx) {
                                                    IconButton(
                                                        onClick = {
                                                            NotificationAndSoundHelper.playWorkFinishedCustomSound(context, idx)
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = "Test play sound", tint = NeonCyan)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Break Sound settings selector
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("☕ Rest/Break Cycle Alarm:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(Modifier.height(8.dp))
                                        soundThemes.forEachIndexed { idx, label ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.saveSoundSettings(context, viewModel.workSoundThemeSetting, idx)
                                                    }
                                                    .padding(vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = viewModel.breakSoundThemeSetting == idx,
                                                    onClick = {
                                                        viewModel.saveSoundSettings(context, viewModel.workSoundThemeSetting, idx)
                                                    }
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(label, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                                
                                                if (viewModel.breakSoundThemeSetting == idx) {
                                                    IconButton(
                                                        onClick = {
                                                            NotificationAndSoundHelper.playBreakFinishedCustomSound(context, idx)
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = "Test play sound", tint = NeonCyan)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            1 -> {
                                // --- CLOUD SYNC PORTAL ---
                                Text(
                                    "☁️ ANSURY CLOUD ACCOUNT HUB",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                if (viewModel.isLoggedIn) {
                                    // Logged in card
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        ),
                                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = NeonCyan,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        "ACTIVE PROFILE",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Black,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = "Welcome, ${viewModel.cloudUserNickname}!",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 16.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                "Linked Email: ${viewModel.cloudUserEmail}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            
                                            Spacer(Modifier.height(12.dp))
                                            Text(
                                                "SYNC CENTRAL LOGS:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = NeonCyan
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.Black.copy(alpha = 0.4f))
                                                    .padding(8.dp)
                                            ) {
                                                Text(
                                                    text = viewModel.syncLogs,
                                                    fontSize = 11.sp,
                                                    color = Color.LightGray
                                                )
                                            }
                                            
                                            Spacer(Modifier.height(16.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        viewModel.backupToCloudAndLocal(context) { success ->
                                                            if (success) {
                                                                Toast.makeText(context, "Transmitted securely to Cloud Vault!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    enabled = !viewModel.isSyncInProgress,
                                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.CloudUpload, contentDescription = "Upload", modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Upload DB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                                }

                                                Button(
                                                    onClick = {
                                                        viewModel.restoreFromCloudOrLocal(context) { success ->
                                                            if (success) {
                                                                Toast.makeText(context, "Sync complete! Environment updated.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    enabled = !viewModel.isSyncInProgress,
                                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.CloudDownload, contentDescription = "Download", modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Download DB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }

                                            Spacer(Modifier.height(8.dp))

                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.logoutCloud(context)
                                                },
                                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Unlink Cloud Profile (Log Out)", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else {
                                    // Signed out, register form
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                "Link an RPG Identity to secure cloud synchronize backups automatically without losing level or streaks.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                            Spacer(Modifier.height(12.dp))

                                            TextField(
                                                value = registerName,
                                                onValueChange = { registerName = it },
                                                label = { Text("Hero Nickname", fontSize = 11.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            Spacer(Modifier.height(8.dp))

                                            TextField(
                                                value = registerEmail,
                                                onValueChange = { registerEmail = it },
                                                label = { Text("Email Address", fontSize = 11.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            Spacer(Modifier.height(8.dp))

                                            TextField(
                                                value = registerPassword,
                                                onValueChange = { registerPassword = it },
                                                label = { Text("Master Spell Password", fontSize = 11.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            Spacer(Modifier.height(16.dp))

                                            Button(
                                                onClick = {
                                                    if (registerEmail.isBlank() || registerName.isBlank()) {
                                                        Toast.makeText(context, "Please enter your Nickname and Email!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        viewModel.registerOrLoginCloud(context, registerEmail, registerName)
                                                        viewModel.backupToCloudAndLocal(context)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Create Cloud Account & Link Database", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> {
                                // --- VAULT SCROLLS (JSON INPUT/OUTPUT) ---
                                Text(
                                    "📜 VAULT SCROLL OF SALVATION (OFFLINE BACKUPS)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonPurple,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            "Export your entire levels, streak stats, goals, intentions, and quest book history as an encrypted text 'Scroll of Salvation'. Paste it below to restore it anywhere instantly, even offline!",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )

                                        Spacer(Modifier.height(14.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            var backupLoadingState by remember { mutableStateOf(false) }

                                            Button(
                                                onClick = {
                                                    backupLoadingState = true
                                                    scope.launch {
                                                        try {
                                                            val dataStr = viewModel.getBackupJsonScroll()
                                                            // Obfuscator to Base64 Scroll representation
                                                            val bytes = dataStr.toByteArray(java.nio.charset.StandardCharsets.UTF_8)
                                                            val b64Scroll = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                                                            textBackupAreaInput = b64Scroll.trim()
                                                            clipboardManager.setText(AnnotatedString(textBackupAreaInput))
                                                            Toast.makeText(context, "💾 Scroll Copied to Clipboard!", Toast.LENGTH_LONG).show()
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        } finally {
                                                            backupLoadingState = false
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                                Spacer(Modifier.width(4.dp))
                                                Text("Export Scroll", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }

                                            Button(
                                                onClick = {
                                                    if (textBackupAreaInput.isBlank()) {
                                                        Toast.makeText(context, "Please paste an Exported Scroll string first!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        scope.launch {
                                                            try {
                                                                val rawBytes = android.util.Base64.decode(textBackupAreaInput.trim(), android.util.Base64.DEFAULT)
                                                                val decodedJson = String(rawBytes, java.nio.charset.StandardCharsets.UTF_8)
                                                                val success = viewModel.restoreBackupJsonScroll(decodedJson)
                                                                if (success) {
                                                                    Toast.makeText(context, "🔮 Scroll Unfurled! Database Restored successfully.", Toast.LENGTH_LONG).show()
                                                                    onDismiss()
                                                                } else {
                                                                    Toast.makeText(context, "❌ Error: Invalid scroll pattern.", Toast.LENGTH_LONG).show()
                                                                }
                                                            } catch (e: Exception) {
                                                                Toast.makeText(context, "❌ Format error: Not a valid Cryptic Scroll format.", Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Input, contentDescription = "Import")
                                                Spacer(Modifier.width(4.dp))
                                                Text("Restore Scroll", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                            }
                                        }

                                        Spacer(Modifier.height(12.dp))

                                        OutlinedTextField(
                                            value = textBackupAreaInput,
                                            onValueChange = { textBackupAreaInput = it },
                                            placeholder = { Text("Paste your raw Scroll text here, or copy generated Scroll directly...", fontSize = 11.sp) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp)
                                                .testTag("scroll_text_input"),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                        )
                                        
                                        Spacer(Modifier.height(8.dp))
                                        
                                        OutlinedButton(
                                            onClick = {
                                                textBackupAreaInput = ""
                                            },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Clear Console", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Vault Footer Quote
                Text(
                    text = "🧙‍♂️ Save often, Hero! The darkness of system restarts cannot pierce a Synchronized Soul.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
