package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Task
import com.example.data.model.Goal
import com.example.ui.theme.*
import com.example.ui.viewmodel.QuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanScreen(
    viewModel: QuestViewModel,
    modifier: Modifier = Modifier
) {
    val NeonCyan = getDynamicCyan()
    val NeonPurple = getDynamicPurple()
    val NeonAmber = getDynamicAmber()
    val NeonRose = getDynamicRose()
    val NeonGreen = getDynamicGreen()

    val tasks by viewModel.tasks.collectAsState()
    val goals by viewModel.goals.collectAsState()

    var selectedSectorFilter by remember { mutableStateOf<String?>(null) }
    val sectorsList = listOf("Business", "Health", "Spiritual", "Relationships", "Personal", "Finance")

    // Filter tasks based on selected sector
    val filteredTasks = remember(tasks, selectedSectorFilter) {
        if (selectedSectorFilter == null) tasks else tasks.filter { it.sector == selectedSectorFilter }
    }

    // Organize tasks into Kanban columns
    val backlogTasks = remember(filteredTasks) {
        filteredTasks.filter { !it.completed && it.plannedDay == null }
    }
    val inProgressTasks = remember(filteredTasks) {
        filteredTasks.filter { !it.completed && it.plannedDay != null }
    }
    val completedTasks = remember(filteredTasks) {
        filteredTasks.filter { it.completed }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Core Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sector Forge Workspace",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Manage your life projects across interactive board columns",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // Projects / Sector Filters
        Text(
            text = "Filter By Workspace Projects",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedSectorFilter == null,
                    onClick = { selectedSectorFilter = null },
                    label = { Text("All Workspaces") },
                    leadingIcon = { Icon(Icons.Default.GridView, contentDescription = "all", modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("kanban_sector_chip_all")
                )
            }
            items(sectorsList) { sector ->
                val isSelected = selectedSectorFilter == sector
                val color = getSectorColor(sector)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedSectorFilter = if (isSelected) null else sector },
                    label = { Text(sector) },
                    leadingIcon = {
                        Icon(
                            imageVector = getSectorIcon(sector),
                            contentDescription = sector,
                            tint = if (isSelected) Color.Black else color,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.85f),
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.testTag("kanban_sector_chip_$sector")
                )
            }
        }

        // Dashboard statistics banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(label = "Backlog", value = backlogTasks.size.toString(), color = NeonAmber)
                VerticalDivider(modifier = Modifier.height(24.dp))
                StatItem(label = "Active Agenda", value = inProgressTasks.size.toString(), color = NeonCyan)
                VerticalDivider(modifier = Modifier.height(24.dp))
                StatItem(label = "Completed", value = completedTasks.size.toString(), color = NeonGreen)
            }
        }

        // Kanban Horizontal Scrollable Board Layout
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Column 1: Unscheduled / Backlog
            KanbanColumn(
                title = "Backlog & Vault",
                description = "Icebox tasks without horizon date",
                tasks = backlogTasks,
                goals = goals,
                accentColor = NeonAmber,
                onComplete = { viewModel.completeTask(it) },
                onQuickMove = { task ->
                    // Set next status: Schedule to Monday as default
                    viewModel.updateTaskPlanning(task, "Monday", null)
                },
                moveIcon = Icons.AutoMirrored.Filled.ArrowForward,
                moveLabel = "Plan"
            )

            // Column 2: Scheduled / In Progress / Agenda
            KanbanColumn(
                title = "Planned Agenda",
                description = "Locked and scheduled targets",
                tasks = inProgressTasks,
                goals = goals,
                accentColor = NeonCyan,
                onComplete = { viewModel.completeTask(it) },
                onQuickMove = { task ->
                    // Move back to backlog
                    viewModel.updateTaskPlanning(task, null, null)
                },
                moveIcon = Icons.AutoMirrored.Filled.ArrowBack,
                moveLabel = "Unplan"
            )

            // Column 3: Completed / Chronicle
            KanbanColumn(
                title = "Completed Chronicle",
                description = "Earned XP points & achievements",
                tasks = completedTasks,
                goals = goals,
                accentColor = NeonGreen,
                onComplete = {},
                onQuickMove = null,
                moveIcon = null,
                moveLabel = ""
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
fun KanbanColumn(
    title: String,
    description: String,
    tasks: List<Task>,
    goals: List<Goal>,
    accentColor: Color,
    onComplete: (Task) -> Unit,
    onQuickMove: ((Task) -> Unit)?,
    moveIcon: ImageVector?,
    moveLabel: String
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Title & Counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accentColor)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }

                Badge(containerColor = accentColor.copy(alpha = 0.2f), contentColor = accentColor) {
                    Text(tasks.size.toString(), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            HorizontalDivider(color = accentColor.copy(alpha = 0.15f))

            Spacer(Modifier.height(8.dp))

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Column Empty",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(tasks) { task ->
                        KanbanTaskCard(
                            task = task,
                            goals = goals,
                            onComplete = { onComplete(task) },
                            onQuickMove = onQuickMove?.let { { it(task) } },
                            moveIcon = moveIcon,
                            moveLabel = moveLabel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KanbanTaskCard(
    task: Task,
    goals: List<Goal>,
    onComplete: () -> Unit,
    onQuickMove: (() -> Unit)?,
    moveIcon: ImageVector?,
    moveLabel: String
) {
    val associatedGoal = remember(goals, task.associatedGoalId) {
        goals.find { it.id == task.associatedGoalId }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("kanban_task_card_${task.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Task Sector + Points
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getSectorIcon(task.sector),
                        contentDescription = task.sector,
                        tint = getSectorColor(task.sector),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = task.sector,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = getSectorColor(task.sector)
                    )
                }
                
                Text(
                    text = if (task.completed) "Earned!" else "+${task.xpReward} XP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (task.completed) NeonGreen else NeonAmber
                )
            }

            Spacer(Modifier.height(6.dp))

            // Task Title
            Text(
                text = task.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (task.completed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Task Notes if any
            if (task.notes.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = task.notes,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Linked Goal indication if any
            if (associatedGoal != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            getSectorColor(associatedGoal.sector).copy(alpha = 0.1f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalActivity,
                        contentDescription = "Goal",
                        tint = getSectorColor(associatedGoal.sector),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = associatedGoal.title,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = getSectorColor(associatedGoal.sector),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quadrant badge
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val qColor = when (task.matrixQuadrant) {
                    1 -> Quadrant1Color
                    2 -> Quadrant2Color
                    3 -> Quadrant3Color
                    4 -> Quadrant4Color
                    else -> NeonCyan
                }
                Box(
                    modifier = Modifier
                        .background(qColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Q${task.matrixQuadrant}", color = qColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }

                // Interactive Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!task.completed) {
                        IconButton(
                            onClick = onComplete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Complete",
                                tint = NeonGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    val mIcon = moveIcon
                    if (onQuickMove != null && mIcon != null) {
                        IconButton(
                            onClick = onQuickMove,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = mIcon,
                                contentDescription = moveLabel,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
