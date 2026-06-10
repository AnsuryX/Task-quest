package com.example.ui.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Task
import com.example.data.model.ImplementationIntention
import com.example.data.model.CommitmentContract
import com.example.data.model.WeeklyReflection
import com.example.ui.theme.*
import com.example.ui.viewmodel.QuestViewModel
import com.example.ui.viewmodel.AiReportState
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestLogScreen(
    viewModel: QuestViewModel,
    modifier: Modifier = Modifier
) {
    val NeonCyan = getDynamicCyan()
    val NeonPurple = getDynamicPurple()
    val NeonAmber = getDynamicAmber()
    val NeonRose = getDynamicRose()
    val NeonGreen = getDynamicGreen()

    val tasks by viewModel.tasks.collectAsState()
    val intentions by viewModel.intentions.collectAsState()
    val contracts by viewModel.commitmentContracts.collectAsState()
    val reflections by viewModel.weeklyReflections.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0=Matrix, 1=Quest List, 2=Planner, 3=Behavioral, 4=Review Journal
    var showAddTaskDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Trigger local push notification helper
    fun triggerPushNudgeAlert() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "chronos_alerts", 
                "Chronos Motivation Pings", 
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        val pings = listOf(
            "Wield your sword, Ranger! Daily strategic targets need slaying.",
            "FALL BEHIND ALERT: The Eisenhower Matrix detects unresolved high-priority matrix items. Engage double focus now!",
            "Did you know? Completing Q2 strategic tasks boosts your XP payouts by 1.5x!",
            "Temptation Bundle Active: Pair your tasks with high energy beats and level up."
        )
        val alertInfo = pings.random()
        val builder = NotificationCompat.Builder(context, "chronos_alerts")
            .setContentTitle("🦉 Chronos Ranger Alarm")
            .setContentText(alertInfo)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(102, builder.build())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Siren test sender and quick title
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quest Logbook",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = { triggerPushNudgeAlert() },
                    modifier = Modifier.testTag("siren_test_button").size(36.dp)
                ) {
                    Icon(
                        Icons.Default.NotificationsActive, 
                        contentDescription = "Test Nudge Alarm",
                        tint = NeonAmber
                    )
                }
                
                Button(
                    onClick = { showAddTaskDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_quest_button_main").height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Quest", modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Quest", fontSize = 12.sp)
                }
            }
        }

        // Modular Navigation Row using ScrollableTabRow to prevent text clipping
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            indicator = { Box {} },
            divider = {}
        ) {
            val tabsLabel = listOf("Matrix", "Quest List", "Horizon Planner", "Behavioral Tools", "Weekly Review")
            tabsLabel.forEachIndexed { i, label ->
                val isSel = selectedTab == i
                Tab(
                    selected = isSel,
                    onClick = { selectedTab = i },
                    text = { 
                        Text(
                            text = label, 
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, 
                            fontSize = 11.sp,
                            color = if (isSel) NeonCyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ) 
                    },
                    modifier = Modifier.testTag("tab_mode_$i")
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // Tab views Router
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> {
                    MatrixGridView(
                        tasks = tasks, 
                        isCompleted = false, 
                        onComplete = { viewModel.completeTask(it) }, 
                        onDelete = { viewModel.deleteTask(it) }
                    )
                }
                1 -> {
                    TaskListView(
                        tasks = tasks, 
                        onComplete = { viewModel.completeTask(it) }, 
                        onDelete = { viewModel.deleteTask(it) }
                    )
                }
                2 -> {
                    HorizonPlannerView(
                        tasks = tasks,
                        onUpdatePlan = { t, day -> viewModel.updateTaskPlanning(t, day, null) },
                        onComplete = { viewModel.completeTask(it) }
                    )
                }
                3 -> {
                    BehavioralToolsTab(
                        intentions = intentions,
                        contracts = contracts,
                        onAddIntention = { trigger, action, location, tod -> 
                            viewModel.addImplementationIntention(trigger, action, tod, location)
                        },
                        onDeleteIntention = { viewModel.deleteIntention(it) },
                        onDeleteContract = { viewModel.deleteCommitment(it) }
                    )
                }
                4 -> {
                    WeeklyReviewTab(
                        reflections = reflections,
                        aiState = viewModel.aiReportState,
                        onSubmitReview = { title, wins, lessons, rating ->
                            viewModel.createWeeklyReflection(title, wins, lessons, rating)
                        }
                    )
                }
            }
        }
    }

    // Add Task / Quest dialogue containing behavioral enhancers
    if (showAddTaskDialog) {
        val calendar = java.util.Calendar.getInstance()
        val dayOfWeekFormat = java.text.SimpleDateFormat("EEEE", java.util.Locale.US)
        val initialDay = dayOfWeekFormat.format(calendar.time)

        val goals by viewModel.goals.collectAsState()
        var questTitle by remember { mutableStateOf("") }
        var questNotes by remember { mutableStateOf("") }
        var selectedQuadrant by remember { mutableStateOf(2) } // default Q2 strategic growth
        var selectedSector by remember { mutableStateOf("Business") }
        var plannedDay by remember { mutableStateOf<String?>(initialDay) }
        var isRepeating by remember { mutableStateOf(false) }
        var repeatInterval by remember { mutableStateOf("None") }
        var temptationBundle by remember { mutableStateOf("") }
        var commitmentXpStake by remember { mutableStateOf("0") }
        var selectedGoalId by remember { mutableStateOf<Int?>(null) }
        var errorState by remember { mutableStateOf(false) }
        var accountabilityPartner by remember { mutableStateOf("") }
        var consequenceDesc by remember { mutableStateOf("") }
        var selectedDuePreset by remember { mutableStateOf("None") }

        Dialog(onDismissRequest = { showAddTaskDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Forge Quest Specs",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = questTitle,
                        onValueChange = { questTitle = it },
                        label = { Text("Quest Title") },
                        placeholder = { Text("e.g. Master Room integrations") },
                        modifier = Modifier.fillMaxWidth().testTag("quest_input_title"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = questNotes,
                        onValueChange = { questNotes = it },
                        label = { Text("Objectives & Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth().testTag("quest_input_notes"),
                        maxLines = 2
                    )

                    Text("Eisenhower Matrix Category", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        QuadrantSelectionRow(1, "Q1: Urgent & Important", selectedQuadrant == 1, Quadrant1Color) { selectedQuadrant = 1 }
                        QuadrantSelectionRow(2, "Q2: Important / Growth Hub", selectedQuadrant == 2, Quadrant2Color) { selectedQuadrant = 2 }
                        QuadrantSelectionRow(3, "Q3: Urgent but Not Important", selectedQuadrant == 3, Quadrant3Color) { selectedQuadrant = 3 }
                        QuadrantSelectionRow(4, "Q4: Not Urgent & Leisure", selectedQuadrant == 4, Quadrant4Color) { selectedQuadrant = 4 }
                    }

                    Text("Associated Life Sector Area", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    val sectorsList = listOf("Business", "Health", "Spiritual", "Relationships", "Personal", "Finance")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        sectorsList.take(3).forEach { sec ->
                            FilterChip(
                                selected = selectedSector == sec,
                                onClick = { selectedSector = sec },
                                label = { Text(sec, fontSize = 11.sp) },
                                modifier = Modifier.testTag("task_sector_chip_$sec")
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        sectorsList.drop(3).forEach { sec ->
                            FilterChip(
                                selected = selectedSector == sec,
                                onClick = { selectedSector = sec },
                                label = { Text(sec, fontSize = 11.sp) },
                                modifier = Modifier.testTag("task_sector_chip_$sec")
                            )
                        }
                    }

                    // Choose connected Life Goal
                    Text("Connect with an Active Goal", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    val activeGoals = remember(goals) { goals.filter { !it.completed } }
                    if (activeGoals.isEmpty()) {
                        Text(
                            "No active goals found. (Create one on Dashboard)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            this@LazyRow.item {
                                FilterChip(
                                    selected = selectedGoalId == null,
                                    onClick = { selectedGoalId = null },
                                    label = { Text("None") }
                                )
                            }
                            this@LazyRow.items(activeGoals) { goal ->
                                val isSel = selectedGoalId == goal.id
                                FilterChip(
                                    selected = isSel,
                                    onClick = { 
                                        selectedGoalId = if (isSel) null else goal.id
                                        if (!isSel) {
                                            selectedSector = goal.sector
                                        }
                                    },
                                    label = { Text("${goal.title} (${goal.sector})") }
                                )
                            }
                        }
                    }

                    // Behavioral Addition A: Weekly Planner Horizon Assignment
                    Text("Assign Days (Weekly Horizon Planner)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        days.take(4).forEach { d ->
                            FilterChip(
                                selected = plannedDay == d,
                                onClick = { plannedDay = if (plannedDay == d) null else d },
                                label = { Text(d.take(3), fontSize = 10.sp) },
                                modifier = Modifier.testTag("day_chip_$d")
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        days.drop(4).forEach { d ->
                            FilterChip(
                                selected = plannedDay == d,
                                onClick = { plannedDay = if (plannedDay == d) null else d },
                                label = { Text(d.take(3), fontSize = 10.sp) },
                                modifier = Modifier.testTag("day_chip_$d")
                            )
                        }
                    }

                    // Behavioral Addition B: Temptation Bundling settings
                    OutlinedTextField(
                        value = temptationBundle,
                        onValueChange = { temptationBundle = it },
                        label = { Text("Temptation Bundle Reward Ritual") },
                        placeholder = { Text("e.g. Savor green tea / Listen to cinematic beats") },
                        modifier = Modifier.fillMaxWidth().testTag("quest_input_temptation"),
                        singleLine = true
                    )

                    // Behavioral Addition C: Commitment Stakes XP Contracting
                    OutlinedTextField(
                        value = commitmentXpStake,
                        onValueChange = { commitmentXpStake = it },
                        label = { Text("Staked Character XP (Commitment Contract)") },
                        placeholder = { Text("Enter XP points e.g. 50") },
                        modifier = Modifier.fillMaxWidth().testTag("quest_input_xpstake"),
                        singleLine = true
                    )

                    // Accountability settings
                    OutlinedTextField(
                        value = accountabilityPartner,
                        onValueChange = { accountabilityPartner = it },
                        label = { Text("Accountability Guardian Name / RPG Mentor") },
                        placeholder = { Text("e.g. Guild Commander, AI Chronos, Jane") },
                        modifier = Modifier.fillMaxWidth().testTag("quest_input_accountability"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = consequenceDesc,
                        onValueChange = { consequenceDesc = it },
                        label = { Text("Consequence of Default / Failure") },
                        placeholder = { Text("e.g. Deduct 40 XP / Donate $5 to Charity") },
                        modifier = Modifier.fillMaxWidth().testTag("quest_input_consequence"),
                        singleLine = true
                    )

                    Text("Due Date (Target Limit) 📅", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val duePresets = listOf("None", "1 Day", "3 Days", "1 Week", "2 Weeks")
                        duePresets.forEach { preset ->
                            val isSel = selectedDuePreset == preset
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedDuePreset = preset },
                                label = { Text(preset, fontSize = 11.sp) },
                                modifier = Modifier.testTag("due_preset_$preset")
                            )
                        }
                    }

                    // Behavioral Addition D: Repeating Quest Configuration
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Repeat, contentDescription = "Repeat", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Repeating Quest", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Automatically re-schedules on complete", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                        Switch(
                            checked = isRepeating,
                            onCheckedChange = { 
                                isRepeating = it
                                if (it && repeatInterval == "None") {
                                    repeatInterval = "Daily"
                                } else if (!it) {
                                    repeatInterval = "None"
                                }
                            },
                            modifier = Modifier.testTag("quest_repeat_switch")
                        )
                    }

                    if (isRepeating) {
                        Text("Repetition Interval", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val intervals = listOf("Daily", "Weekly", "Monthly")
                            intervals.forEach { interval ->
                                val isSel = repeatInterval == interval
                                FilterChip(
                                    selected = isSel,
                                    onClick = { repeatInterval = interval },
                                    label = { Text(interval, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("repeat_interval_$interval")
                                )
                            }
                        }
                    }

                    if (errorState) {
                        Text("Please enter a valid quest title.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showAddTaskDialog = false },
                            modifier = Modifier.testTag("dismiss_quest_dialog")
                        ) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (questTitle.trim().isNotBlank()) {
                                    val xpToStake = commitmentXpStake.toIntOrNull() ?: 0
                                    val targetDueDate = when (selectedDuePreset) {
                                        "1 Day" -> System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
                                        "3 Days" -> System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000L)
                                        "1 Week" -> System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)
                                        "2 Weeks" -> System.currentTimeMillis() + (14 * 24 * 60 * 60 * 1000L)
                                        else -> null
                                    }
                                    viewModel.addTaskWithBehavioralDetails(
                                        title = questTitle.trim(),
                                        notes = questNotes.trim(),
                                        quadrant = selectedQuadrant,
                                        sector = selectedSector,
                                        plannedDay = plannedDay,
                                        plannedDate = System.currentTimeMillis(),
                                        temptationBundle = temptationBundle.trim(),
                                        commitmentXpStake = xpToStake,
                                        associatedGoalId = selectedGoalId,
                                        dueDate = targetDueDate,
                                        accountabilityPartner = accountabilityPartner.trim(),
                                        consequenceDesc = consequenceDesc.trim(),
                                        isRepeating = isRepeating,
                                        repeatInterval = repeatInterval
                                    )
                                    showAddTaskDialog = false
                                } else {
                                    errorState = true
                                }
                            },
                            modifier = Modifier.testTag("submit_quest_dialog"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Enlist Quest")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatrixGridView(
    tasks: List<Task>,
    isCompleted: Boolean,
    onComplete: (Task) -> Unit,
    onDelete: (Task) -> Unit
) {
    val activeTasks = remember(tasks, isCompleted) { tasks.filter { it.completed == isCompleted } }
    val q1 = remember(activeTasks) { activeTasks.filter { it.matrixQuadrant == 1 } }
    val q2 = remember(activeTasks) { activeTasks.filter { it.matrixQuadrant == 2 } }
    val q3 = remember(activeTasks) { activeTasks.filter { it.matrixQuadrant == 3 } }
    val q4 = remember(activeTasks) { activeTasks.filter { it.matrixQuadrant == 4 } }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuadrantBox(
                title = "Do First (Q1)",
                description = "Urgent & Important",
                tasks = q1,
                color = Quadrant1Color,
                modifier = Modifier.weight(1f),
                onComplete = onComplete,
                onDelete = onDelete
            )
            QuadrantBox(
                title = "Schedule (Q2)",
                description = "Strategic Growth Hub",
                tasks = q2,
                color = Quadrant2Color,
                modifier = Modifier.weight(1f),
                onComplete = onComplete,
                onDelete = onDelete
            )
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuadrantBox(
                title = "Delegate (Q3)",
                description = "Urgent but Low Impact",
                tasks = q3,
                color = Quadrant3Color,
                modifier = Modifier.weight(1f),
                onComplete = onComplete,
                onDelete = onDelete
            )
            QuadrantBox(
                title = "Limit (Q4)",
                description = "Leisure & Errand",
                tasks = q4,
                color = Quadrant4Color,
                modifier = Modifier.weight(1f),
                onComplete = onComplete,
                onDelete = onDelete
            )
        }
    }
}

@Composable
fun QuadrantBox(
    title: String,
    description: String,
    tasks: List<Task>,
    color: Color,
    modifier: Modifier = Modifier,
    onComplete: (Task) -> Unit,
    onDelete: (Task) -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
                Badge(containerColor = color.copy(alpha = 0.2f), contentColor = color) {
                    Text("${tasks.size}", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
            Text(description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            
            Spacer(Modifier.height(8.dp))
            Divider(color = color.copy(alpha = 0.15f))
            Spacer(Modifier.height(8.dp))

            if (tasks.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Sector Clear", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(tasks) { task ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onComplete(task) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    task.title, 
                                    fontSize = 12.sp, 
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                                if (task.temptationBundle.isNotEmpty()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "🍬 Bundled: ${task.temptationBundle}", 
                                        fontSize = 9.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = NeonAmber
                                    )
                                }
                                if (task.hasCommitmentContract) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "🔒 XP Contract: ${task.commitmentXpStake} XP", 
                                        fontSize = 9.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = NeonPurple
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

@Composable
fun TaskListView(
    tasks: List<Task>,
    onComplete: (Task) -> Unit,
    onDelete: (Task) -> Unit
) {
    val activeList = remember(tasks) { tasks.filter { !it.completed } }
    val completedList = remember(tasks) { tasks.filter { it.completed } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        if (activeList.isNotEmpty()) {
            item {
                Text("Quest Board - Active Quests", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = NeonCyan)
            }
            items(activeList) { task ->
                ExpandedTaskRow(task = task, onComplete = onComplete, onDelete = onDelete)
            }
        }

        if (completedList.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                Text("Completed Chronicle", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = NeonGreen)
            }
            items(completedList) { task ->
                ExpandedTaskRow(task = task, onComplete = onComplete, onDelete = onDelete)
            }
        }
    }
}

@Composable
fun ExpandedTaskRow(
    task: Task,
    onComplete: (Task) -> Unit,
    onDelete: (Task) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val quadrantColor = when (task.matrixQuadrant) {
        1 -> Quadrant1Color
        2 -> Quadrant2Color
        3 -> Quadrant3Color
        4 -> Quadrant4Color
        else -> NeonCyan
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expanded_task_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onComplete(task) },
                colors = CheckboxDefaults.colors(checkedColor = quadrantColor),
                modifier = Modifier.testTag("complete_checkbox_${task.id}")
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    color = if (task.completed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                )
                if (task.notes.isNotEmpty()) {
                    Text(
                        text = task.notes,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Quadrant Q${task.matrixQuadrant}", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(labelColor = quadrantColor)
                    )
                    SuggestionChip(
                        onClick = {},
                        label = { Text(task.sector, fontSize = 10.sp) },
                        icon = {
                            Icon(
                                getSectorIcon(task.sector),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = getSectorColor(task.sector)
                            )
                        }
                    )
                    if (task.plannedDay != null) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("📅 Planner: ${task.plannedDay}", fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = NeonCyan)
                        )
                    }

                    if (task.dueDate != null) {
                        val diffMs = task.dueDate - System.currentTimeMillis()
                        val daysLeft = (diffMs / (1000.0 * 60.0 * 60.0 * 24.0)).toInt()
                        val chipText = when {
                            task.completed -> "Due: Completed"
                            daysLeft < 0 -> "OVERDUE ⚠️"
                            daysLeft == 0 -> "Due Today ⏳"
                            else -> "Due: In $daysLeft d 🕒"
                        }
                        val chipColor = when {
                            task.completed -> NeonGreen
                            daysLeft < 0 -> MaterialTheme.colorScheme.error
                            daysLeft == 0 -> NeonAmber
                            else -> NeonCyan
                        }
                        SuggestionChip(
                            onClick = {},
                            label = { Text(chipText, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = chipColor)
                        )
                    }

                    if (task.accountabilityPartner.isNotEmpty()) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("🛡️ Guardian: ${task.accountabilityPartner}", fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = NeonAmber)
                        )
                    }

                    if (task.isRepeating) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("🔄 Cycle: ${task.repeatInterval}", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    if (task.consequenceDesc.isNotEmpty() && !task.completed) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("⚠️ Penalty: ${task.consequenceDesc}", fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = MaterialTheme.colorScheme.error)
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = if (task.completed) "Earned!" else "+${task.xpReward} XP",
                    fontWeight = FontWeight.Bold,
                    color = if (task.completed) NeonGreen else NeonAmber,
                    fontSize = 11.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { 
                            com.example.util.CalendarSyncHelper.insertTaskToCalendarIntent(context, task)
                        },
                        modifier = Modifier.testTag("calendar_sync_task_${task.id}")
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Sync to Calendar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { onDelete(task) },
                        modifier = Modifier.testTag("delete_task_row_${task.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// Behavioral Enhancements UI 1: Daily and Weekly horizon planner
@Composable
fun HorizonPlannerView(
    tasks: List<Task>,
    onUpdatePlan: (Task, String?) -> Unit,
    onComplete: (Task) -> Unit
) {
    val daysList = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val currentDay = remember { java.text.SimpleDateFormat("EEEE", java.util.Locale.US).format(java.util.Date()) }
    var selectedDayFilter by remember { mutableStateOf(if (daysList.contains(currentDay)) currentDay else "Monday") }
    
    val tasksForDay = remember(tasks, selectedDayFilter) {
        tasks.filter { !it.completed && it.plannedDay == selectedDayFilter }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Horizon Planner", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                Text("Visualize week planning of high importance quests", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }

        // Horizontal scrolling days selection row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysList.forEach { d ->
                val isSel = d == selectedDayFilter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { selectedDayFilter = d }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = d.take(3),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isSel) NeonCyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            if (tasksForDay.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🛡️", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("No quests scheduled for $selectedDayFilter!", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Use the Quest dialogue to schedule objectives.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(tasksForDay) { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.completed,
                                onCheckedChange = { onComplete(task) },
                                colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (task.temptationBundle.isNotEmpty()) {
                                    Text("🍬 Bundled Reward: ${task.temptationBundle}", fontSize = 10.sp, color = NeonAmber, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            TextButton(onClick = { onUpdatePlan(task, null) }) {
                                Text("Remove", fontSize = 10.sp, color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Behavioral Enhancements UI 2: If-Then Planners (Implementation Intentions)
@Composable
fun BehavioralToolsTab(
    intentions: List<ImplementationIntention>,
    contracts: List<CommitmentContract>,
    onAddIntention: (String, String, String, String) -> Unit,
    onDeleteIntention: (Int) -> Unit,
    onDeleteContract: (Int) -> Unit
) {
    var triggerStr by remember { mutableStateOf("") }
    var actionStr by remember { mutableStateOf("") }
    var locationStr by remember { mutableStateOf("") }
    var todStr by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Form Section Info
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Implementation Intentions (If-Then Planner)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Forge custom environment associations (e.g. 'IF event occurs, THEN make task execution reflex'). Scientifically proven to decrease task procrastination by 200%.", 
                    fontSize = 11.sp, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // Implementation Intention Input Form
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Forge If-Then Association Trigger", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                
                OutlinedTextField(
                    value = triggerStr,
                    onValueChange = { triggerStr = it },
                    placeholder = { Text("IF... (e.g., I step into study room with tea)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("intention_if_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = actionStr,
                    onValueChange = { actionStr = it },
                    placeholder = { Text("THEN... (e.g., I will engage Focus Shield zone on Compose)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("intention_then_input"),
                    singleLine = true
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = locationStr,
                        onValueChange = { locationStr = it },
                        placeholder = { Text("Location e.g. Desk", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = todStr,
                        onValueChange = { todStr = it },
                        placeholder = { Text("Time e.g. 9:00 AM", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Button(
                    onClick = {
                        if (triggerStr.isNotBlank() && actionStr.isNotBlank()) {
                            onAddIntention(triggerStr, actionStr, locationStr, todStr)
                            triggerStr = ""
                            actionStr = ""
                            locationStr = ""
                            todStr = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    modifier = Modifier.align(Alignment.End).testTag("save_intention_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add Intention Association", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // List Active If-Thens Associations
        if (intentions.isNotEmpty()) {
            Text("Active Habits & Intentions associations", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            intentions.forEach { intention ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("HABIT MATRIX", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = NeonCyan)
                            IconButton(
                                onClick = { onDeleteIntention(intention.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = intention.triggerSituation,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = NeonAmber
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = intention.desiredAction,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NeonGreen
                        )
                        if (intention.location.isNotEmpty()) {
                            Text("📍 Location: ${intention.location} | ⏰ Time: ${intention.timeOfDay}", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // Commitment Contracts active list
        if (contracts.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("Active Character Commitment Contracts", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            contracts.forEach { contract ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Task: ${contract.taskTitle}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Character XP Staked Reward: ${contract.xpStake} XP", fontSize = 11.sp, color = NeonAmber, fontWeight = FontWeight.Bold)
                            Text("Default penalty: ${contract.penaltyDescription}", fontSize = 10.sp, color = Color.Gray)
                        }
                        IconButton(onClick = { onDeleteContract(contract.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Nullify Contract", tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// Behavioral Enhancements UI 3: End of Week Reviews with Chronos
@Composable
fun WeeklyReviewTab(
    reflections: List<WeeklyReflection>,
    aiState: StateFlow<AiReportState>,
    onSubmitReview: (String, String, String, Int) -> Unit
) {
    var reviewTitle by remember { mutableStateOf("Weekly Synthesis Review") }
    var winsSummary by remember { mutableStateOf("") }
    var lessonsLearned by remember { mutableStateOf("") }
    var focusStars by remember { mutableStateOf(4) }
    
    val currentAiState by aiState.collectAsState()

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Submit Weekly Progress Chronicle", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                
                OutlinedTextField(
                    value = reviewTitle,
                    onValueChange = { reviewTitle = it },
                    label = { Text("Week Label / Title") },
                    modifier = Modifier.fillMaxWidth().testTag("review_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = winsSummary,
                    onValueChange = { winsSummary = it },
                    label = { Text("What did you slay this week? (Wins)") },
                    placeholder = { Text("e.g. Cleared all strategic coding matrices") },
                    modifier = Modifier.fillMaxWidth().testTag("wins_summary_input"),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = lessonsLearned,
                    onValueChange = { lessonsLearned = it },
                    label = { Text("Hardships & Lessons (Mistakes)") },
                    placeholder = { Text("e.g. Procrastinated on morning focus zone segments") },
                    modifier = Modifier.fillMaxWidth().testTag("lessons_input"),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Stamina Stars Rating", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        (1..5).forEach { star ->
                            val active = star <= focusStars
                            Text(
                                text = if (active) "⭐" else "☆",
                                fontSize = 18.sp,
                                modifier = Modifier.clickable { focusStars = star }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (winsSummary.isNotBlank() && reviewTitle.isNotBlank()) {
                            onSubmitReview(reviewTitle, winsSummary, lessonsLearned, focusStars)
                            winsSummary = ""
                            lessonsLearned = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().testTag("submit_weekly_review_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Consult Chronos for End-of-Week reflection", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Live Consulting review loading states
        AnimatedVisibility(visible = currentAiState !is AiReportState.Idle) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                when (val state = currentAiState) {
                    is AiReportState.Loading -> {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = NeonCyan)
                            Spacer(Modifier.height(10.dp))
                            Text("🦉 Chronos reviewing week... formulating tactical feedback...")
                        }
                    }
                    is AiReportState.Success -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🦉 ", fontSize = 24.sp)
                                Text("CHRONOS RANGER ADVISORY VERDICT:", fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            MarkdownCard(state.reportMarkdown)
                        }
                    }
                    is AiReportState.Error -> {
                        Text("Connection issue consulting Chronos AI: ${state.message}", color = Color.Red, fontSize = 12.sp)
                    }
                    else -> {}
                }
            }
        }

        // History reflections
        if (reflections.isNotEmpty()) {
            Text("Previous Weekly Chronicle Reflections", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeonCyan)
            reflections.forEach { reflection ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(reflection.title, fontWeight = FontWeight.Bold, color = NeonPurple, fontSize = 13.sp)
                            Text("Stars: ${"⭐".repeat(reflection.focusRating)}", fontSize = 11.sp)
                        }
                        Text("Wins: ${reflection.winsSummary}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        if (reflection.lessonsLearned.isNotEmpty()) {
                            Text("Reflections & Hardships: ${reflection.lessonsLearned}", fontSize = 11.sp, color = Color.Gray)
                        }
                        if (reflection.aiInsight.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Historical Ranger Advisory Verdict:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NeonCyan)
                            Text(reflection.aiInsight, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuadrantSelectionRow(
    quadrant: Int,
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .border(
                1.5.dp,
                if (isSelected) color else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(8.dp)
            )
            .testTag("quadrant_selection_$quadrant"),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = color)
            )
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (isSelected) color else MaterialTheme.colorScheme.onSurface)
        }
    }
}
