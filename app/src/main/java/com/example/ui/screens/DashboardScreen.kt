package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Goal
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiReportState
import com.example.ui.viewmodel.QuestViewModel

data class RangerBadge(
    val id: String,
    val title: String,
    val category: String,
    val icon: String,
    val description: String,
    val current: Int,
    val target: Int,
    val color: Color
) {
    val isUnlocked: Boolean get() = current >= target
    val progress: Float get() = if (target > 0) (current.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: QuestViewModel,
    modifier: Modifier = Modifier
) {
    val NeonCyan = getDynamicCyan()
    val NeonPurple = getDynamicPurple()
    val NeonAmber = getDynamicAmber()
    val NeonRose = getDynamicRose()
    val NeonGreen = getDynamicGreen()

    val goals by viewModel.goals.collectAsState()
    val stats by viewModel.userStats.collectAsState()
    val aiState by viewModel.aiReportState.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val intentions by viewModel.intentions.collectAsState()
    val commitmentContracts by viewModel.commitmentContracts.collectAsState()

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var isCodexExpanded by remember { mutableStateOf(false) }
    var codexFilterIndex by remember { mutableStateOf(0) } // 0 = All, 1 = Unlocked, 2 = Locked

    val badges = remember(stats, tasks, intentions, commitmentContracts, goals, NeonCyan, NeonPurple, NeonAmber, NeonRose, NeonGreen) {
        val completedQuestsCount = stats?.questsCompleted ?: 0
        val totalFocusMin = stats?.focusMinutesTotal ?: 0
        val totalFocusZoneMin = stats?.totalFocusZoneMinutes ?: 0
        val activeStreak = stats?.streakDays ?: 1
        val level = stats?.level ?: 1
        
        listOf(
            RangerBadge(
                id = "initiate_crest",
                title = "Initiate's Crest",
                category = "Quest Mastery",
                icon = "⚔️",
                description = "Enlist and resolve your first quest.",
                current = completedQuestsCount,
                target = 1,
                color = NeonCyan
            ),
            RangerBadge(
                id = "iron_ranger",
                title = "Iron Ranger",
                category = "Quest Mastery",
                icon = "🛡️",
                description = "Prove your mettle by completing 10 quests.",
                current = completedQuestsCount,
                target = 10,
                color = NeonPurple
            ),
            RangerBadge(
                id = "zen_disciple",
                title = "Zen Disciple",
                category = "Focus Meditation",
                icon = "🧘",
                description = "Focus inside the pomodoro chamber for 60 minutes.",
                current = totalFocusMin,
                target = 60,
                color = NeonAmber
            ),
            RangerBadge(
                id = "stamina_sage",
                title = "Stamina Sage",
                category = "Focus Meditation",
                icon = "🕯️",
                description = "Sustain focus for a total of 300 minutes.",
                current = totalFocusMin,
                target = 300,
                color = NeonRose
            ),
            RangerBadge(
                id = "guardian_shield",
                title = "Shield Guardian",
                category = "Quantum Shield",
                icon = "🌀",
                description = "Lock down systems with Focus Shield DND for 30 minutes total.",
                current = totalFocusZoneMin,
                target = 30,
                color = NeonCyan
            ),
            RangerBadge(
                id = "habit_alchemist",
                title = "Habit Alchemist",
                category = "Planning Rituals",
                icon = "🧪",
                description = "Forge 3 multi-environmental If-Then planners.",
                current = intentions.size,
                target = 3,
                color = NeonGreen
            ),
            RangerBadge(
                id = "commitment_paladin",
                title = "Commitment Paladin",
                category = "Oaths of Stakes",
                icon = "💎",
                description = "Establish a XP Staked commitment contract.",
                current = commitmentContracts.size,
                target = 1,
                color = NeonRose
            ),
            RangerBadge(
                id = "aspiration_forger",
                title = "Aspiration Forger",
                category = "Plan Horizon",
                icon = "🌟",
                description = "Establish 3 multi-sector life goals.",
                current = goals.size,
                target = 3,
                color = NeonAmber
            ),
            RangerBadge(
                id = "phoenix_ascendant",
                title = "Phoenix Ascendant",
                category = "Ranger Streaks",
                icon = "🔥",
                description = "Maintain a 3-day active streak in the matrix.",
                current = activeStreak,
                target = 3,
                color = Quadrant1Color
            ),
            RangerBadge(
                id = "grand_ranger",
                title = "Grand Ranger",
                category = "Level Elevation",
                icon = "👑",
                description = "Ascend your character to Level 3.",
                current = level,
                target = 3,
                color = NeonPurple
            )
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 90.dp, top = 8.dp)
    ) {
        // Section: Sector Goals Progress Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sector Goals",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Button(
                    onClick = { showAddGoalDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_goal_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Goal", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Goal", fontSize = 13.sp)
                }
            }
        }

        item {
            var showAIForge by remember { mutableStateOf(false) }
            var aiGoalInput by remember { mutableStateOf("") }
            val aiLoading = viewModel.aiCoPilotLoading
            var copilotMessage by remember { mutableStateOf("") }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🧙", fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Chronos AI Quest Forge",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Forge goals & related agenda tasks automatically",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        IconButton(onClick = { showAIForge = !showAIForge }) {
                            Icon(
                                imageVector = if (showAIForge) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Forge"
                            )
                        }
                    }

                    if (showAIForge) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Describe your aspiration (e.g. 'Build healthy cardio fitness' or 'Master Kotlin structures'). Chronos will automatically populate 1 main Goal and 2 Eisenhower matrix tasks instantly!",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = aiGoalInput,
                            onValueChange = { aiGoalInput = it },
                            placeholder = { Text("Enter aspiration...", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("ai_forge_input"),
                            singleLine = true,
                            enabled = !aiLoading
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (copilotMessage.isNotEmpty()) {
                                Text(
                                    text = copilotMessage,
                                    fontSize = 11.sp,
                                    color = NeonAmber,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                            }
                            Button(
                                onClick = {
                                    if (aiGoalInput.isNotBlank()) {
                                        viewModel.autoForgeWithCoPilot(aiGoalInput) { msg ->
                                            copilotMessage = msg
                                            aiGoalInput = ""
                                        }
                                    }
                                },
                                enabled = !aiLoading && aiGoalInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("ai_forge_submit_btn")
                            ) {
                                if (aiLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Forging...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.Bolt, contentDescription = "Forge", modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Forge Quests", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (goals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = "Empty Goals",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No life goals configured yet!",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Gamify different sectors of your life (Business, Health, Spiritual, Personal) and complete corresponding matrix tasks to progress them.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Group Goals by Sector
            val groupedGoals = goals.groupBy { it.sector }
            items(groupedGoals.keys.toList()) { sector ->
                val sectorGoals = groupedGoals[sector] ?: emptyList()
                val sectorIcon = getSectorIcon(sector)
                val sectorColor = getSectorColor(sector)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                sectorIcon,
                                contentDescription = sector,
                                tint = sectorColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = sector,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = sectorColor
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        sectorGoals.forEach { goal ->
                            Row(
                                modifier = Modifier.fillMaxWidth().testTag("goal_item_${goal.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = goal.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    val progress = (goal.currentValue / goal.targetValue).coerceIn(0f, 1f)
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                        color = sectorColor,
                                        trackColor = sectorColor.copy(alpha = 0.2f),
                                        strokeCap = StrokeCap.Round
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Progress: ${goal.currentValue.toInt()}/${goal.targetValue.toInt()} points",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "${(progress * 100).toInt()}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = sectorColor
                                        )
                                    }
                                }
                                Box(modifier = Modifier.padding(start = 12.dp)) {
                                    IconButton(
                                        onClick = { viewModel.deleteGoal(goal) },
                                        modifier = Modifier.testTag("delete_goal_${goal.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Goal",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        // --- GAMIFIED ACCOMPLISHMENTS: RANGER CODEX ---
        item {
            val unlockedCount = badges.count { it.isUnlocked }
            val filteredBadges = remember(badges, codexFilterIndex) {
                when (codexFilterIndex) {
                    1 -> badges.filter { it.isUnlocked }
                    2 -> badges.filter { !it.isUnlocked }
                    else -> badges
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ranger_codex_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCodexExpanded = !isCodexExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🏆",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Column {
                                Text(
                                    text = "RANGER CODEX",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Completed: $unlockedCount / ${badges.size} Milestones",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (unlockedCount == badges.size) NeonGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (unlockedCount == badges.size) NeonGreen.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.12f),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "${((unlockedCount.toFloat() / badges.size.toFloat()) * 100).toInt()}% CLR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (unlockedCount == badges.size) NeonGreen else NeonCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Icon(
                                imageVector = if (isCodexExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Codex",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    AnimatedVisibility(visible = isCodexExpanded) {
                        Column {
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.height(12.dp))

                            // Filter Chips Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = codexFilterIndex == 0,
                                    onClick = { codexFilterIndex = 0 },
                                    label = { Text("All (${badges.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.testTag("codex_filter_all")
                                )
                                FilterChip(
                                    selected = codexFilterIndex == 1,
                                    onClick = { codexFilterIndex = 1 },
                                    label = { Text("Unlocked ($unlockedCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.testTag("codex_filter_unlocked")
                                )
                                FilterChip(
                                    selected = codexFilterIndex == 2,
                                    onClick = { codexFilterIndex = 2 },
                                    label = { Text("Locked (${badges.size - unlockedCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.testTag("codex_filter_locked")
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            if (filteredBadges.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No milestones in this class yet.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    filteredBadges.forEach { badge ->
                                        val isUnlocked = badge.isUnlocked
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("badge_${badge.id}"),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isUnlocked) {
                                                    badge.color.copy(alpha = 0.05f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                }
                                            ),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (isUnlocked) badge.color.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Badge Icon with circle background
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .background(
                                                            color = if (isUnlocked) badge.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (isUnlocked) badge.icon else "🔒",
                                                        fontSize = 20.sp
                                                    )
                                                }

                                                Spacer(Modifier.width(12.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = badge.title,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isUnlocked) badge.color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                        )
                                                        Text(
                                                            text = badge.category.uppercase(),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = badge.color.copy(alpha = 0.8f),
                                                            letterSpacing = 0.5.sp
                                                        )
                                                    }
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        text = badge.description,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                    )
                                                    
                                                    Spacer(Modifier.height(6.dp))
                                                    
                                                    // Progress Indicator
                                                    LinearProgressIndicator(
                                                        progress = { badge.progress },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(5.dp),
                                                        color = badge.color,
                                                        trackColor = if (isUnlocked) badge.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                        strokeCap = StrokeCap.Round
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "Progress: ${badge.current} / ${badge.target}",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                        )
                                                        Text(
                                                            text = "${(badge.progress * 100).toInt()}%",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isUnlocked) badge.color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
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
                }
            }
        }

        // Section: AI Performance reports (Rules 6 & 7)
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = "Gemini AI",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Gemini Focus Intelligence",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Automated analysis of your Eisenhower task prioritisation, Pomodoro flow charts, and progress matrices.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.generatePerformanceReport() },
                        modifier = Modifier.fillMaxWidth().testTag("generate_report_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = "Generate")
                        Spacer(Modifier.width(8.dp))
                        Text("Analyze & Generate Weekly Chronicle", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(12.dp))

                    AnimatedVisibility(visible = aiState !is AiReportState.Idle) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            when (val state = aiState) {
                                is AiReportState.Loading -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(color = NeonCyan)
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            "Contacting Chronos Matrix Sage... Generating recommendations...",
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                is AiReportState.Success -> {
                                    MarkdownCard(state.reportMarkdown)
                                }
                                is AiReportState.Error -> {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Report Compilation Paused",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = state.message,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }

    // Goal Dialog Creation Sheet
    if (showAddGoalDialog) {
        var goalTitle by remember { mutableStateOf("") }
        var selectedSector by remember { mutableStateOf("Business") }
        var targetScore by remember { mutableStateOf("50") }
        var sizeError by remember { mutableStateOf(false) }

        val tasks by viewModel.tasks.collectAsState()
        val unassociatedTasks = remember(tasks, selectedSector) { 
            tasks.filter { !it.completed && it.associatedGoalId == null && it.sector == selectedSector } 
        }
        var selectedTaskIds by remember { mutableStateOf(setOf<Int>()) }

        Dialog(onDismissRequest = { showAddGoalDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Add Sector Quest Goal",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = goalTitle,
                        onValueChange = { goalTitle = it },
                        label = { Text("Goal / Epic Title") },
                        placeholder = { Text("e.g., Read 10 technical papers") },
                        modifier = Modifier.fillMaxWidth().testTag("goal_title_input"),
                        singleLine = true
                    )

                    Text(
                        text = "Select Life Sector Sector",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    val sectors = listOf("Business", "Health", "Spiritual", "Relationships", "Personal", "Finance")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sectors.take(3).forEach { sector ->
                            val isSel = selectedSector == sector
                            FilterChip(
                                selected = isSel,
                                onClick = { 
                                    selectedSector = sector
                                    selectedTaskIds = emptySet() // Reset selections if sector changes
                                },
                                label = { Text(sector, fontSize = 11.sp) },
                                modifier = Modifier.testTag("sector_chip_$sector")
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sectors.drop(3).forEach { sector ->
                            val isSel = selectedSector == sector
                            FilterChip(
                                selected = isSel,
                                onClick = { 
                                    selectedSector = sector
                                    selectedTaskIds = emptySet() // Reset selections if sector changes
                                },
                                label = { Text(sector, fontSize = 11.sp) },
                                modifier = Modifier.testTag("sector_chip_$sector")
                            )
                        }
                    }

                    // Task association widget inside Add Goal dialog
                    if (unassociatedTasks.isNotEmpty()) {
                        Text(
                            text = "Associate Existing Quests (${selectedSector})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            this@LazyRow.items(unassociatedTasks) { task ->
                                val isChosen = selectedTaskIds.contains(task.id)
                                FilterChip(
                                    selected = isChosen,
                                    onClick = {
                                        selectedTaskIds = if (isChosen) {
                                            selectedTaskIds - task.id
                                        } else {
                                            selectedTaskIds + task.id
                                        }
                                    },
                                    label = { Text(task.title, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = targetScore,
                        onValueChange = { targetScore = it },
                        label = { Text("Target Quest Points") },
                        placeholder = { Text("Default 50 (10 per completed quest)") },
                        modifier = Modifier.fillMaxWidth().testTag("goal_target_input"),
                        singleLine = true
                    )

                    if (sizeError) {
                        Text("Please enter a valid title and target amount.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showAddGoalDialog = false },
                            modifier = Modifier.testTag("dismiss_goal_dialog")
                        ) {
                            Text("Discard")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val targetVal = targetScore.toFloatOrNull()
                                if (goalTitle.isNotBlank() && targetVal != null && targetVal > 0) {
                                    viewModel.addGoalWithTasks(
                                        title = goalTitle.trim(), 
                                        sector = selectedSector, 
                                        targetValue = targetVal, 
                                        associatedTaskIds = selectedTaskIds.toList()
                                    )
                                    showAddGoalDialog = false
                                } else {
                                    sizeError = true
                                }
                            },
                            modifier = Modifier.testTag("submit_goal_dialog"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Enlist Goal")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownCard(markdown: String) {
    // Elegant Custom Markdown Viewer tailored to Mobile constraints
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val lines = markdown.split("\n")
        lines.forEach { line ->
            when {
                line.trim().startsWith("###") -> {
                    val cleanText = line.replace("###", "").trim()
                    Text(
                        text = cleanText,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.25.sp
                        ),
                        color = NeonCyan,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                line.trim().startsWith("##") -> {
                    val cleanText = line.replace("##", "").trim()
                    Text(
                        text = cleanText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = NeonCyan,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                }
                line.trim().startsWith("**") && line.trim().endsWith("**") -> {
                    val cleanText = line.replace("**", "").trim()
                    Text(
                        text = cleanText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeonPurple
                    )
                }
                line.trim().startsWith("-") || line.trim().startsWith("*") -> {
                    // List item
                    val cleanItem = line.substring(1).trim()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeonPurple
                        )
                        Text(
                            text = cleanItem,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                line.isNotBlank() -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

fun getSectorIcon(sector: String): ImageVector {
    return when (sector.trim().lowercase()) {
        "business" -> Icons.Default.BusinessCenter
        "health" -> Icons.Default.Favorite
        "spiritual" -> Icons.Default.SelfImprovement
        "relationships" -> Icons.Default.People
        "personal" -> Icons.Default.Person
        "finance" -> Icons.Default.AttachMoney
        else -> Icons.Default.TrendingUp
    }
}

fun getSectorColor(sector: String): Color {
    return when (sector.trim().lowercase()) {
        "business" -> NeonPurple
        "health" -> Color(0xFFFF2B6D)
        "spiritual" -> NeonCyan
        "relationships" -> Color(0xFFFF9F1C)
        "personal" -> NeonAmber
        "finance" -> Color(0xFF2EC4B6)
        else -> NeonCyan
    }
}
