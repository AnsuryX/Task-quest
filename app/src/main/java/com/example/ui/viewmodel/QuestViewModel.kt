package com.example.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.gemini.GeminiClient
import com.example.data.model.Goal
import com.example.data.model.PomodoroSession
import com.example.data.model.Task
import com.example.data.model.UserStats
import com.example.data.model.ImplementationIntention
import com.example.data.model.CommitmentContract
import com.example.data.model.WeeklyReflection
import com.example.data.repository.QuestRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiReportState {
    object Idle : AiReportState
    object Loading : AiReportState
    data class Success(val reportMarkdown: String) : AiReportState
    data class Error(val message: String) : AiReportState
}

class QuestViewModel(private val repository: QuestRepository) : ViewModel() {

    // --- Database State Flows ---
    val tasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<Goal>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStats: StateFlow<UserStats?> = repository.userStatsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val pomodoroSessions: StateFlow<List<PomodoroSession>> = repository.allPomodoroSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val intentions: StateFlow<List<ImplementationIntention>> = repository.allIntentions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commitmentContracts: StateFlow<List<CommitmentContract>> = repository.allCommitmentContracts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyReflections: StateFlow<List<WeeklyReflection>> = repository.allWeeklyReflections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Quest UI Events ---
    sealed class QuestUiEvent {
        data class TimerStarted(val mode: String, val isShieldEnabled: Boolean) : QuestUiEvent()
        data class TimerPaused(val mode: String) : QuestUiEvent()
        data class TimerFinished(val mode: String) : QuestUiEvent()
        object LevelUp : QuestUiEvent()
    }

    private val _uiEvents = MutableSharedFlow<QuestUiEvent>()
    val uiEvents: SharedFlow<QuestUiEvent> = _uiEvents.asSharedFlow()

    // --- Customizable Sound Options & Cloud Account Sync States ---
    var workSoundThemeSetting by mutableStateOf(0) // 0 = Arcade, 1 = Classic, 2 = Zen, 3 = Cosmic
    var breakSoundThemeSetting by mutableStateOf(0) // 0 = Arcade, 1 = Classic, 2 = Zen, 3 = Cosmic
    var appThemeMode by mutableStateOf(0) // 0 = Follow System, 1 = Great White, 2 = Cosmic Obsidian

    var cloudUserEmail by mutableStateOf("")
    var cloudUserNickname by mutableStateOf("")
    var isLoggedIn by mutableStateOf(false)
    var syncLogs by mutableStateOf("No synchronization performed in this session. Cloud vault ready.")
    var isSyncInProgress by mutableStateOf(false)
    var chronosTrainingDirectives by mutableStateOf("")
    var isCalendarSyncEnabled by mutableStateOf(false)

    private var appContext: android.content.Context? = null

    fun loadPreferences(context: android.content.Context) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        val prefs = applicationContext.getSharedPreferences("quest_settings", android.content.Context.MODE_PRIVATE)
        workSoundThemeSetting = prefs.getInt("work_sound_setting", 0)
        breakSoundThemeSetting = prefs.getInt("break_sound_setting", 0)
        appThemeMode = prefs.getInt("app_theme_mode", 0)
        cloudUserEmail = prefs.getString("cloud_user_email", "") ?: ""
        cloudUserNickname = prefs.getString("cloud_user_nickname", "") ?: ""
        isLoggedIn = prefs.getBoolean("is_logged_in", false)
        chronosTrainingDirectives = prefs.getString("chronos_training_directives", "") ?: ""
        isCalendarSyncEnabled = prefs.getBoolean("is_calendar_sync_enabled", false)
        reconcileTimerState(applicationContext)
    }

    fun saveCalendarSyncSetting(context: android.content.Context, enabled: Boolean) {
        isCalendarSyncEnabled = enabled
        val prefs = context.getSharedPreferences("quest_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_calendar_sync_enabled", enabled)
            .apply()
    }

    fun saveChronosTrainingDirectives(context: android.content.Context, directives: String) {
        chronosTrainingDirectives = directives
        val prefs = context.getSharedPreferences("quest_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("chronos_training_directives", directives)
            .apply()
    }

    fun saveThemeSetting(context: android.content.Context, modeIndex: Int) {
        appThemeMode = modeIndex
        val prefs = context.getSharedPreferences("quest_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("app_theme_mode", modeIndex)
            .apply()
    }

    fun saveSoundSettings(context: android.content.Context, workIndex: Int, breakIndex: Int) {
        workSoundThemeSetting = workIndex
        breakSoundThemeSetting = breakIndex
        val prefs = context.getSharedPreferences("quest_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("work_sound_setting", workIndex)
            .putInt("break_sound_setting", breakIndex)
            .apply()
    }

    fun registerOrLoginCloud(context: android.content.Context, email: String, nickname: String) {
        cloudUserEmail = email
        cloudUserNickname = nickname
        isLoggedIn = true
        val prefs = context.getSharedPreferences("quest_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("cloud_user_email", email)
            .putString("cloud_user_nickname", nickname)
            .putBoolean("is_logged_in", true)
            .apply()
        syncLogs = "Successfully linked cloud profile: $nickname ($email). Auto-save active!"
    }

    fun logoutCloud(context: android.content.Context) {
        cloudUserEmail = ""
        cloudUserNickname = ""
        isLoggedIn = false
        val prefs = context.getSharedPreferences("quest_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .remove("cloud_user_email")
            .remove("cloud_user_nickname")
            .putBoolean("is_logged_in", false)
            .apply()
        syncLogs = "Logged out from Cloud database. Offline work is buffered locally."
    }

    fun backupToCloudAndLocal(context: android.content.Context, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            isSyncInProgress = true
            try {
                val backupStr = com.example.util.BackupManager.exportDataToJson(repository)
                val prefs = context.getSharedPreferences("quest_settings", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("auto_vault_backup", backupStr).apply()
                delay(1200)
                syncLogs = "SUCCESS: Synchronized with Cloud Database. Auto-saved tasks, streaks, level (${lastKnownLevel ?: 1}) and completed quests safely."
                onResult(true)
            } catch (e: Exception) {
                syncLogs = "Synch Failed: ${e.localizedMessage ?: "Unknown connection error"}"
                onResult(false)
            } finally {
                isSyncInProgress = false
            }
        }
    }

    fun restoreFromCloudOrLocal(context: android.content.Context, customJsonScroll: String? = null, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            isSyncInProgress = true
            try {
                val dataToRestore = if (!customJsonScroll.isNullOrBlank()) {
                    customJsonScroll
                } else {
                    val prefs = context.getSharedPreferences("quest_settings", android.content.Context.MODE_PRIVATE)
                    prefs.getString("auto_vault_backup", null)
                }

                if (dataToRestore.isNullOrBlank()) {
                    syncLogs = "Vault is empty! Please create a local backup or connect to cloud."
                    onResult(false)
                } else {
                    val success = com.example.util.BackupManager.importDataFromJson(repository, dataToRestore)
                    if (success) {
                        syncLogs = "CLOUD RESTORE GRANTED: Character levels, achievements, and intentions completely synced!"
                        onResult(true)
                    } else {
                        syncLogs = "Error: Backup Scroll signature mismatch or corrupted data."
                        onResult(false)
                    }
                }
            } catch (e: Exception) {
                syncLogs = "Synchronization Error: ${e.localizedMessage ?: "Decoding error"}"
                onResult(false)
            } finally {
                isSyncInProgress = false
            }
        }
    }

    fun getBackupRepository(): QuestRepository {
        return repository
    }

    suspend fun getBackupJsonScroll(): String {
        return com.example.util.BackupManager.exportDataToJson(repository)
    }

    suspend fun restoreBackupJsonScroll(jsonStr: String): Boolean {
        val success = com.example.util.BackupManager.importDataFromJson(repository, jsonStr)
        if (success) {
            companionNudge = "🔮 VAULT RESTORE GRANTED: Character levels, achievements, and intentions completely synced!"
        }
        return success
    }

    private var lastKnownLevel: Int? = null

    // --- Pomodoro and Focus Shield State Variables ---
    var countDownSeconds by mutableStateOf(1500) // default 25 minutes
        private set

    var isTimerRunning by mutableStateOf(false)
        private set

    var pomodoroMode by mutableStateOf("work") // "work", "short_break", "long_break"
        private set

    var selectedTaskId by mutableStateOf<Int?>(null)

    private var timerJob: Job? = null

    // Focus Zone Shield toggles & states
    var isFocusZoneEnabled by mutableStateOf(false)
    var isSuppressionActive by mutableStateOf(false) // Simulated "DND / Alert suppression active" state label

    // --- AI Suggestions & Companion State ---
    private val _aiReportState = MutableStateFlow<AiReportState>(AiReportState.Idle)
    val aiReportState: StateFlow<AiReportState> = _aiReportState.asStateFlow()

    // For companion nudges
    var companionNudge by mutableStateOf("Welcome back, Hero! Let's conquer our goals today. Use the Focus Shield to silence notifications and unlock 3x XP!")
        private set

    init {
        // Monitor level progression for sound and animation triggers
        viewModelScope.launch {
            userStats.collect { stats ->
                if (stats != null) {
                    if (lastKnownLevel != null && stats.level > lastKnownLevel!!) {
                        _uiEvents.emit(QuestUiEvent.LevelUp)
                    }
                    lastKnownLevel = stats.level
                }
            }
        }

        // Run first trigger of stats setup and quick companion reminder
        viewModelScope.launch {
            // Seed a default task and goal if empty
            delay(100) // allow db setup
            if (tasks.value.isEmpty()) {
                repository.insertTask(Task(title = "Study Jetpack Compose Patterns", notes = "Leveling up on Q2 (Important, Not Urgent) skills for modern app development", matrixQuadrant = 2, sector = "Business", xpReward = 30))
                repository.insertGoal(Goal(title = "Level Up Android Mastery", sector = "Business", targetValue = 100f, currentValue = 10f))
                repository.insertGoal(Goal(title = "Complete 3 Daily Zen Meditations", sector = "Spiritual", targetValue = 30f, currentValue = 0f))
                
                // Seed an implementation intention template
                repository.insertIntention(
                    ImplementationIntention(
                        triggerSituation = "IF it is 9:00 AM after grinding some coffee",
                        desiredAction = "THEN I will sit in the Focus Chamber and study Compose Patterns",
                        location = "Study Desk",
                        timeOfDay = "09:00 AM"
                    )
                )
            }
            triggerCompanionNudge()
        }
    }

    // --- State persistence helpers ---
    private fun saveTimerPreferences() {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences("quest_settings", android.content.Context.MODE_PRIVATE)
        val targetEndTime = System.currentTimeMillis() + (countDownSeconds * 1000L)
        prefs.edit().apply {
            putBoolean("timer_is_running", isTimerRunning)
            putString("timer_mode", pomodoroMode)
            putLong("timer_target_end_time", targetEndTime)
            putInt("timer_remaining_seconds", countDownSeconds)
            putInt("timer_selected_task_id", selectedTaskId ?: -1)
            putBoolean("timer_shield_enabled", isFocusZoneEnabled)
            apply()
        }
    }

    private fun setSystemAlarm(targetEndTimeMs: Long, mode: String) {
        val context = appContext ?: return
        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
        val intent = android.content.Intent(context, com.example.util.TimerAlarmReceiver::class.java).apply {
            putExtra("mode", mode)
            putExtra("work_sound_setting", workSoundThemeSetting)
            putExtra("break_sound_setting", breakSoundThemeSetting)
        }
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    targetEndTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    android.app.AlarmManager.RTC_WAKEUP,
                    targetEndTimeMs,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelSystemAlarm() {
        val context = appContext ?: return
        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
        val intent = android.content.Intent(context, com.example.util.TimerAlarmReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onTimerFinishedAway(context: android.content.Context, savedMode: String) {
        viewModelScope.launch {
            val duration = when (savedMode) {
                "work" -> 25
                "short_break" -> 5
                "long_break" -> 15
                else -> 25
            }
            
            repository.insertPomodoroSession(
                PomodoroSession(
                    durationMinutes = duration,
                    type = savedMode,
                    associatedTaskId = if (savedMode == "work") selectedTaskId else null,
                    isFocusZone = (savedMode == "work" && isFocusZoneEnabled)
                )
            )

            val nextMode = if (savedMode == "work") "short_break" else "work"
            pomodoroMode = nextMode
            countDownSeconds = when (nextMode) {
                "work" -> 25 * 60
                "short_break" -> 5 * 60
                "long_break" -> 15 * 60
                else -> 25 * 60
            }
            saveTimerPreferences()
            triggerCompanionNudge()
        }
    }

    private fun reconcileTimerState(context: android.content.Context) {
        val prefs = context.getSharedPreferences("quest_settings", android.content.Context.MODE_PRIVATE)
        val savedIsRunning = prefs.getBoolean("timer_is_running", false)
        val savedMode = prefs.getString("timer_mode", "work") ?: "work"
        val targetEndTime = prefs.getLong("timer_target_end_time", 0L)
        val remainingAtPause = prefs.getInt("timer_remaining_seconds", 1500)
        val selectedTask = prefs.getInt("timer_selected_task_id", -1)
        val shieldEnabled = prefs.getBoolean("timer_shield_enabled", false)

        pomodoroMode = savedMode
        selectedTaskId = if (selectedTask != -1) selectedTask else null
        isFocusZoneEnabled = shieldEnabled

        if (savedIsRunning) {
            val now = System.currentTimeMillis()
            val remaining = ((targetEndTime - now) / 1000).toInt()
            if (remaining > 0) {
                isTimerRunning = true
                countDownSeconds = remaining
                isSuppressionActive = shieldEnabled && savedMode == "work"
                startBackgroundTimerJob()
            } else {
                isTimerRunning = false
                isSuppressionActive = false
                countDownSeconds = when (savedMode) {
                    "work" -> 25 * 60
                    "short_break" -> 5 * 60
                    "long_break" -> 15 * 60
                    else -> 25 * 60
                }
                onTimerFinishedAway(context, savedMode)
            }
        } else {
            isTimerRunning = false
            isSuppressionActive = false
            countDownSeconds = remainingAtPause
        }
    }

    private fun startBackgroundTimerJob() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (countDownSeconds > 0 && isTimerRunning) {
                delay(1000)
                countDownSeconds -= 1
                appContext?.let { ctx ->
                    val prefs = ctx.getSharedPreferences("quest_settings", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putInt("timer_remaining_seconds", countDownSeconds).apply()
                }
            }
            if (countDownSeconds == 0) {
                onTimerFinished()
            }
        }
    }

    // --- Pomodoro Ticker Logic ---
    fun toggleTimer() {
        if (isTimerRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
        saveTimerPreferences()
    }

    private fun startTimer() {
        isTimerRunning = true
        if (isFocusZoneEnabled && pomodoroMode == "work") {
            isSuppressionActive = true
            companionNudge = "🔊 DISTRACTIONS SILENCED: Focus Shield engaged. Crush your objectives, Hero!"
        }
        viewModelScope.launch {
            _uiEvents.emit(QuestUiEvent.TimerStarted(pomodoroMode, isFocusZoneEnabled))
        }
        
        val targetEndTime = System.currentTimeMillis() + (countDownSeconds * 1000L)
        setSystemAlarm(targetEndTime, pomodoroMode)
        
        startBackgroundTimerJob()
    }

    fun pauseTimer() {
        isTimerRunning = false
        isSuppressionActive = false
        timerJob?.cancel()
        
        cancelSystemAlarm()
        
        viewModelScope.launch {
            _uiEvents.emit(QuestUiEvent.TimerPaused(pomodoroMode))
        }
        saveTimerPreferences()
    }

    fun setTimerMode(mode: String) {
        pauseTimer()
        pomodoroMode = mode
        countDownSeconds = when (mode) {
            "work" -> 25 * 60
            "short_break" -> 5 * 60
            "long_break" -> 15 * 60
            else -> 25 * 60
        }
        saveTimerPreferences()
    }

    private fun onTimerFinished() {
        isTimerRunning = false
        isSuppressionActive = false
        timerJob?.cancel()
        
        cancelSystemAlarm()
        
        val finishedMode = pomodoroMode
        viewModelScope.launch {
            _uiEvents.emit(QuestUiEvent.TimerFinished(finishedMode))
        }
        
        viewModelScope.launch {
            val duration = when (pomodoroMode) {
                "work" -> 25
                "short_break" -> 5
                "long_break" -> 15
                else -> 25
            }
            repository.insertPomodoroSession(
                PomodoroSession(
                    durationMinutes = duration,
                    type = pomodoroMode,
                    associatedTaskId = if (pomodoroMode == "work") selectedTaskId else null,
                    isFocusZone = (pomodoroMode == "work" && isFocusZoneEnabled)
                )
            )

            if (pomodoroMode == "work") {
                setTimerMode("short_break")
                companionNudge = if (isFocusZoneEnabled) {
                    "🛡️ Shield Session Cleared! +3x XP Gained. Take a break, ranger!"
                } else {
                    "Enthralling focus, Champion! Grab some fresh water. Your break timer is ready."
                }
            } else {
                setTimerMode("work")
                companionNudge = "Rest period complete. Ready to start your next focus block? Let's build momentum."
            }
            saveTimerPreferences()
        }
    }

    fun resetTimer() {
        pauseTimer()
        setTimerMode(pomodoroMode)
        saveTimerPreferences()
    }

    // --- Tasks and Goals Operations ---
    fun addTask(title: String, notes: String, quadrant: Int, sector: String) {
        viewModelScope.launch {
            val xp = when (quadrant) {
                1 -> 40 // Q1: Urgent & Important
                2 -> 30 // Q2: Important & Not Urgent
                3 -> 20 // Q3: Urgent & Not Important
                4 -> 10 // Q4: Not Urgent & Not Important
                else -> 20
            }
            repository.insertTask(
                Task(
                    title = title,
                    notes = notes,
                    matrixQuadrant = quadrant,
                    sector = sector,
                    xpReward = xp
                )
            )
            triggerCompanionNudge()
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            repository.completeTask(task)
            triggerCompanionNudge()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun addGoal(
        title: String, 
        sector: String, 
        targetValue: Float, 
        dueDate: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L),
        accountabilityPartner: String = "",
        consequenceDesc: String = ""
    ) {
        viewModelScope.launch {
            repository.insertGoal(
                Goal(
                    title = title,
                    sector = sector,
                    targetValue = targetValue,
                    currentValue = 0f,
                    dueDate = dueDate,
                    accountabilityPartner = accountabilityPartner,
                    consequenceDesc = consequenceDesc
                )
            )
            triggerCompanionNudge()
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    // --- Dynamic Companion Nudges (Rule 8) ---
    fun triggerCompanionNudge() {
        viewModelScope.launch {
            val pending = tasks.value.count { !it.completed }
            val overdue = tasks.value.count { !it.completed && it.matrixQuadrant == 1 }
            val stats = userStats.value ?: UserStats()
            
            companionNudge = when {
                overdue > 0 -> "Heads up! You have $overdue urgent high-priority quests pending. Conquer them to safeguard your streak!"
                pending > 5 -> "Our task book has $pending pending quests. Break them down, tackle one Q2 item, and level up!"
                stats.streakDays > 2 -> "A formidable streak of ${stats.streakDays} days! Keep this flame burning, Hero!"
                else -> "A fresh canvas! Let's set some ambitious goals across Health, Spiritual, and Business sectors."
            }
        }
    }

    // --- AI Reports Generator (Rule 6 & 7) ---
    fun generatePerformanceReport() {
        _aiReportState.value = AiReportState.Loading
        viewModelScope.launch {
            val stats = userStats.value ?: UserStats()
            val allTaskList = tasks.value
            val allGoalList = goals.value
            val completedCount = allTaskList.count { it.completed }
            val totalMinutes = stats.focusMinutesTotal

            val goalsSummary = allGoalList.joinToString("\n") { 
                "- Sector: [${it.sector}] Goal: '${it.title}' - Progress: ${it.currentValue}/${it.targetValue} (${if (it.completed) "Completed!" else "In Progress"})"
            }

            val tasksSummary = allTaskList.filter { !it.completed }.joinToString("\n") {
                "- [Quadrant Q${it.matrixQuadrant}] [${it.sector}] [XP: ${it.xpReward}] Task: '${it.title}'"
            }

            val prompt = """
                Analyze my gamified performance metrics below:
                - RPG Fighter Level: ${stats.level} (Total Quests Logged: ${stats.questsCompleted})
                - Streak: ${stats.streakDays} Days Active
                - Total Pomodoro Focus: $totalMinutes focus minutes
                - Completed Tasks Count: $completedCount

                Goals Setup across Life Areas:
                $goalsSummary

                Pending Tasks inside the Eisenhower Matrix Quadrants:
                $tasksSummary

                Present a fully-styled Markdown summary. Include sections for:
                1. 🏆 **Hero State & Achievements**: Rate my work gamified and output 2 funny achievements unlocked or pending.
                2. 🔬 **Eisenhower Audit**: Note if I have been focusing on the right areas. Critically examine if I am avoiding Q2 (Strategic, Important but not Urgent) and how to coordinate my sectors like Health or Spiritual.
                3. 🚀 **Personalized Tactical Training**: Concrete suggestions to improve efficiency based on my performance (focus hours vs task ratios).
                4. 🦉 **Companion's Whisper**: A cute, gentle, motivational letter writing directly to me about my progression.
            """.trimIndent()

            val customDirectives = if (chronosTrainingDirectives.isNotBlank()) "\n[USER CHRONOS TRAINING DIRECTIVES IN EFFECT - STRONGLY INCORPORATE THESE RULES]:\n$chronosTrainingDirectives" else ""
            val systemPrompt = "You are Chronos, an elite productivity companion, gamification trainer, and wise RPG mentor. Keep reports stylish, engaging, highly structured, in a gorgeous cosmic style.$customDirectives"

            val report = GeminiClient.generateAiContent(prompt, systemPrompt)
            if (report.contains("Connection Error") || report.contains("Please configure")) {
                _aiReportState.value = AiReportState.Error(report)
            } else {
                _aiReportState.value = AiReportState.Success(report)
            }
        }
    }

    // --- Behavioral & Planning Operations ---

    fun addTaskWithBehavioralDetails(
        title: String,
        notes: String,
        quadrant: Int,
        sector: String,
        plannedDay: String? = null,
        plannedDate: Long? = null,
        temptationBundle: String = "",
        commitmentXpStake: Int = 0,
        associatedGoalId: Int? = null,
        dueDate: Long? = null,
        accountabilityPartner: String = "",
        consequenceDesc: String = "",
        isRepeating: Boolean = false,
        repeatInterval: String = "None"
    ) {
        viewModelScope.launch {
            val basexp = when (quadrant) {
                1 -> 40
                2 -> 30
                3 -> 20
                4 -> 10
                else -> 20
            }
            val hasContract = commitmentXpStake > 0
            val newId = repository.insertTask(
                Task(
                    title = title,
                    notes = notes,
                    matrixQuadrant = quadrant,
                    sector = sector,
                    xpReward = basexp,
                    plannedDay = plannedDay,
                    plannedDate = plannedDate,
                    temptationBundle = temptationBundle,
                    hasCommitmentContract = hasContract,
                    commitmentXpStake = commitmentXpStake,
                    associatedGoalId = associatedGoalId,
                    dueDate = dueDate,
                    accountabilityPartner = accountabilityPartner,
                    consequenceDesc = consequenceDesc,
                    isRepeating = isRepeating,
                    repeatInterval = repeatInterval
                )
            )
            if (isCalendarSyncEnabled) {
                appContext?.let { context ->
                    val freshTask = Task(
                        id = newId.toInt(),
                        title = title,
                        notes = notes,
                        matrixQuadrant = quadrant,
                        sector = sector,
                        xpReward = basexp,
                        plannedDay = plannedDay,
                        plannedDate = plannedDate,
                        temptationBundle = temptationBundle,
                        hasCommitmentContract = hasContract,
                        commitmentXpStake = commitmentXpStake,
                        associatedGoalId = associatedGoalId,
                        dueDate = dueDate,
                        accountabilityPartner = accountabilityPartner,
                        consequenceDesc = consequenceDesc,
                        isRepeating = isRepeating,
                        repeatInterval = repeatInterval
                    )
                    com.example.util.CalendarSyncHelper.syncTaskToCalendarSilently(context, freshTask)
                }
            }
            if (hasContract) {
                repository.insertCommitmentContract(
                    CommitmentContract(
                        taskTitle = title,
                        xpStake = commitmentXpStake,
                        penaltyDescription = if (consequenceDesc.isNotBlank()) consequenceDesc else "Yield character stake penalty if defaulted",
                        dueDate = dueDate ?: (System.currentTimeMillis() + (24 * 60 * 60 * 1000L))
                    )
                )
            }
            triggerCompanionNudge()
        }
    }

    fun addGoalWithTasks(
        title: String, 
        sector: String, 
        targetValue: Float, 
        associatedTaskIds: List<Int>,
        dueDate: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L),
        accountabilityPartner: String = "",
        consequenceDesc: String = ""
    ) {
        viewModelScope.launch {
            val newGoalId = repository.insertGoal(
                Goal(
                    title = title,
                    sector = sector,
                    targetValue = targetValue,
                    currentValue = 0f,
                    dueDate = dueDate,
                    accountabilityPartner = accountabilityPartner,
                    consequenceDesc = consequenceDesc
                )
            ).toInt()

            // Update selected tasks
            val currentTasks = tasks.value
            for (taskId in associatedTaskIds) {
                val taskToUpdate = currentTasks.find { it.id == taskId }
                if (taskToUpdate != null) {
                    repository.updateTask(taskToUpdate.copy(associatedGoalId = newGoalId))
                }
            }
            triggerCompanionNudge()
        }
    }

    fun updateTaskPlanning(task: Task, plannedDay: String?, plannedDate: Long?) {
        viewModelScope.launch {
            repository.updateTask(task.copy(plannedDay = plannedDay, plannedDate = plannedDate))
        }
    }

    fun addImplementationIntention(trigger: String, action: String, timeOfDay: String, location: String, matchedTaskId: Int? = null) {
        viewModelScope.launch {
            repository.insertIntention(
                ImplementationIntention(
                    triggerSituation = trigger,
                    desiredAction = action,
                    timeOfDay = timeOfDay,
                    location = location,
                    associatedTaskId = matchedTaskId
                )
            )
        }
    }

    fun deleteIntention(id: Int) {
        viewModelScope.launch {
            repository.deleteIntentionById(id)
        }
    }

    fun deleteCommitment(id: Int) {
        viewModelScope.launch {
            repository.deleteCommitmentContractById(id)
        }
    }

    fun createWeeklyReflection(title: String, wins: String, lessons: String, rating: Int) {
        _aiReportState.value = AiReportState.Loading
        viewModelScope.launch {
            val stats = userStats.value
            val totalMinutes = stats?.focusMinutesTotal ?: 0
            val totalZoneMinutes = stats?.totalFocusZoneMinutes ?: 0
            val doneCount = stats?.questsCompleted ?: 0
            
            val prompt = """
                Compile my weekly progression review:
                - Stars Rating: $rating/5
                - Focus Minutes Logged: $totalMinutes min
                - Focus Shield Zone Time: $totalZoneMinutes min
                - Quests Slain: $doneCount
                
                My Wins listed:
                $wins
                
                Lessons & Hardships:
                $lessons
                
                Please evaluate:
                1. 📖 **Coaching Review**: How effectively did I manage my mental stamina? Do I bundle temptations correctly?
                2. 🛡️ **Shield Zone Mastery**: Analyze my time in silenced Focus Zones versus standard sessions.
                3. 🦉 **Ranger Wisdom**: A short, high-energy gaming style motivation letter recommending next week's focus areas (specifically targeting Q2 strategic tasks). Keep output markdown clean.
            """.trimIndent()
            
            val customDirectives = if (chronosTrainingDirectives.isNotBlank()) "\n[USER CHRONOS TRAINING DIRECTIVES IN EFFECT - STRONGLY INCORPORATE THESE RULES]:\n$chronosTrainingDirectives" else ""
            val systemPrompt = "You are Chronos, a wise RPG ranger. Keep reviews stylish, structured, encouraging, in clean markdown.$customDirectives"
            val outcome = GeminiClient.generateAiContent(prompt, systemPrompt)
            
            val reflection = WeeklyReflection(
                title = title,
                winsSummary = wins,
                lessonsLearned = lessons,
                focusRating = rating,
                aiInsight = outcome
            )
            repository.insertWeeklyReflection(reflection)
            _aiReportState.value = AiReportState.Success(outcome)
        }
    }

    var aiCoPilotLoading by mutableStateOf(false)
        private set

    private suspend fun forgeLocalFallbackQuests(userFocusTopic: String): Pair<Int, Int> {
        val topic = userFocusTopic.lowercase().trim()
        val sector = when {
            topic.contains("fitness") || topic.contains("cardio") || topic.contains("health") || topic.contains("gym") || topic.contains("run") || topic.contains("workout") || topic.contains("diet") -> "Health"
            topic.contains("learn") || topic.contains("study") || topic.contains("read") || topic.contains("language") || topic.contains("book") || topic.contains("course") -> "Spiritual"
            topic.contains("work") || topic.contains("code") || topic.contains("kotlin") || topic.contains("app") || topic.contains("business") || topic.contains("project") || topic.contains("compile") -> "Business"
            topic.contains("money") || topic.contains("finance") || topic.contains("budget") || topic.contains("save") || topic.contains("invest") -> "Finance"
            topic.contains("friend") || topic.contains("relationship") || topic.contains("family") || topic.contains("love") || topic.contains("meet") || topic.contains("social") -> "Relationships"
            else -> "Personal"
        }

        // Generate personalized title for Goal
        val goalTitle = when (sector) {
            "Health" -> "Quest of the Emerald Athlete: Transform physical vigor and stamina regarding '$userFocusTopic'"
            "Business" -> "Master the Iron Scribe: Dominate business milestones for '$userFocusTopic'"
            "Spiritual" -> "Path of the Luminary Mind: Attain deep focus and master knowledge on '$userFocusTopic'"
            "Finance" -> "Hoard of the Golden Dragon: Secure wealth foundation for '$userFocusTopic'"
            "Relationships" -> "Covenant of the Allied Guild: Elevate personal bonds regarding '$userFocusTopic'"
            else -> "Ascent of the Chronos Ranger: Complete personal epic trek for '$userFocusTopic'"
        }

        // Generate associated tasks based on topic / sector
        val task1Title = when (sector) {
            "Health" -> "Shield Core Training: Execute active physical physical routine (30 min)"
            "Business" -> "Architect Blueprint Stage: Outline critical specifications for '$userFocusTopic'"
            "Spiritual" -> "Sacred Chronos Focus: Commit to deep-read research notes"
            "Finance" -> "Treasury Audit: Detail resource allocations & accounts"
            "Relationships" -> "Guild Assembly: Conduct meaningful engagement with key alliances"
            else -> "Establish Foundation Stone: Clear starting hurdles for '$userFocusTopic'"
        }

        val task1Quad = 1 // Quadrant 1 (Urgent & Important)
        val task2Title = when (sector) {
            "Health" -> "Dietary Alchemist: Prep healthy fuel reserves and hydrate"
            "Business" -> "Refine Artifact Repository: Refactor and sweep loose ends"
            "Spiritual" -> "Consolidate Memory Vault: Write custom summary/flashcards"
            "Finance" -> "Coin Hoarder Ward: Automate reserve savings & mitigate leaks"
            "Relationships" -> "Empathy Beacon: Dispatch appreciative message / digital gesture"
            else -> "Scout the Horizon: Map out tomorrow's sequential action items"
        }
        val task2Quad = 2 // Quadrant 2 (Important but not Urgent)

        val newGoalId = repository.insertGoal(Goal(title = goalTitle, sector = sector, targetValue = 50f)).toInt()
        val currentDayOfWeek = java.text.SimpleDateFormat("EEEE", java.util.Locale.US).format(java.util.Date())
        
        repository.insertTask(Task(title = task1Title, matrixQuadrant = task1Quad, sector = sector, xpReward = 40, targetPomodoros = 2, associatedGoalId = newGoalId, plannedDay = currentDayOfWeek))
        repository.insertTask(Task(title = task2Title, matrixQuadrant = task2Quad, sector = sector, xpReward = 30, targetPomodoros = 2, associatedGoalId = newGoalId, plannedDay = currentDayOfWeek))

        return Pair(1, 2)
    }

    fun autoForgeWithCoPilot(userFocusTopic: String, onCompleted: (String) -> Unit) {
        aiCoPilotLoading = true
        viewModelScope.launch {
            val currentDayOfWeek = java.text.SimpleDateFormat("EEEE", java.util.Locale.US).format(java.util.Date())
            val customDirectives = if (chronosTrainingDirectives.isNotBlank()) {
                "\n\n[USER CHRONOS TRAINING DIRECTIVES IN EFFECT - MAKE SURE YOUR OUTPUT ADHERES STRONGLY TO THIS PERSONALITY/STYLE]:\n$chronosTrainingDirectives"
            } else ""

            val prompt = """
                You are Chronos, the elite productivity co-pilot.
                Create high-importance goals and action-oriented tasks for this topic: "$userFocusTopic"
                $customDirectives

                I need exactly 1 specific Goal and exactly 2 associated Tasks.
                Generate them using this STRICT parser pattern so that the APP can read them directly. You must output EXACTLY what lies between ===GOAL_BEGIN=== and ===GOAL_END===, and what lies between ===TASKS_BEGIN=== and ===TASKS_END===:
                
                ===GOAL_BEGIN===
                [Title of Goal]|[Life Area Sector like "Health","Business","Spiritual","Relationships","Personal" or "Finance"]|[Target Progress Points e.g. 50]
                ===GOAL_END===
                
                ===TASKS_BEGIN===
                [Task 1 Title]|[Eisenhower Quadrant 1,2,3 or 4]|[Sector]|[Target Pomodoros]
                [Task 2 Title]|[Eisenhower Quadrant 1,2,3 or 4]|[Sector]|[Target Pomodoros]
                ===TASKS_END===
                
                Keep the titles highly inspiring and RPG-flavored. Ensure the Sector matches one of: Business, Health, Spiritual, Relationships, Personal, Finance.
            """.trimIndent()
            
            try {
                val response = GeminiClient.generateAiContent(prompt, "You are an elite productivity database co-pilot. You print strictly following requested split tokens without extra text outside of tokens.")
                
                var parsedGoalCount = 0
                var parsedTaskCount = 0

                val isResponseValid = response.isNotBlank() && 
                        !response.contains("Connection Error") && 
                        !response.contains("Please configure your GEMINI_API_KEY")

                if (isResponseValid) {
                    // Parse Goal (Standard Regex)
                    val goalRegex = "===GOAL_BEGIN===\\s*(.*?)\\s*===GOAL_END===".toRegex(RegexOption.DOT_MATCHES_ALL)
                    val goalMatch = goalRegex.find(response)
                    var createdGoalId: Int? = null
                    if (goalMatch != null) {
                        val lines = goalMatch.groupValues[1].split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                        for (line in lines) {
                            val cleanedLine = line.removeSurrounding("*").removeSurrounding("[").removeSurrounding("]").trim()
                            val parts = cleanedLine.split("|")
                            if (parts.size >= 3) {
                                val title = parts[0].trim()
                                val sector = parts[1].trim()
                                val target = parts[2].trim().toFloatOrNull() ?: 50f
                                createdGoalId = repository.insertGoal(Goal(title = title, sector = sector, targetValue = target)).toInt()
                                parsedGoalCount++
                            }
                        }
                    }

                    // Parse Tasks (Standard Regex)
                    val tasksRegex = "===TASKS_BEGIN===\\s*(.*?)\\s*===TASKS_END===".toRegex(RegexOption.DOT_MATCHES_ALL)
                    val tasksMatch = tasksRegex.find(response)
                    if (tasksMatch != null) {
                        val lines = tasksMatch.groupValues[1].split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                        for (line in lines) {
                            val cleanedLine = line.removeSurrounding("*").removeSurrounding("[").removeSurrounding("]").trim()
                            val parts = cleanedLine.split("|")
                            if (parts.size >= 4) {
                                val title = parts[0].trim()
                                val quad = parts[1].trim().toIntOrNull() ?: 2
                                val sector = parts[2].trim()
                                val targetPomos = parts[3].trim().toIntOrNull() ?: 2
                                
                                val xp = when (quad) {
                                    1 -> 40
                                    2 -> 30
                                    3 -> 20
                                    4 -> 10
                                    else -> 20
                                }
                                repository.insertTask(
                                    Task(
                                        title = title,
                                        matrixQuadrant = quad,
                                        sector = sector,
                                        xpReward = xp,
                                        targetPomodoros = targetPomos,
                                        associatedGoalId = createdGoalId,
                                        plannedDay = currentDayOfWeek
                                    )
                                )
                                parsedTaskCount++
                            }
                        }
                    }

                    // Fallback line-by-line scanning parser in case structure is slightly off
                    if (parsedGoalCount == 0 || parsedTaskCount == 0) {
                        val lines = response.split("\n").map { it.trim() }.filter { it.isNotEmpty() && it.contains("|") }
                        for (line in lines) {
                            val cleanedLine = line.removePrefix("-").removePrefix("*").trim()
                                .removeSurrounding("[", "]").removeSurrounding("*").trim()
                            val parts = cleanedLine.split("|")
                            if (parts.size >= 3) {
                                val firstPart = parts[0].trim()
                                val secondPartInt = parts[1].trim().toIntOrNull()
                                if (secondPartInt != null && parts.size >= 4 && parsedTaskCount < 2) {
                                    val title = firstPart
                                    val quad = secondPartInt
                                    val sector = parts[2].trim()
                                    val targetPomos = parts[3].trim().toIntOrNull() ?: 2
                                    val xp = when (quad) {
                                        1 -> 40
                                        2 -> 30
                                        3 -> 20
                                        4 -> 10
                                        else -> 20
                                    }
                                    repository.insertTask(
                                        Task(
                                            title = title,
                                            matrixQuadrant = quad,
                                            sector = sector,
                                            xpReward = xp,
                                            targetPomodoros = targetPomos,
                                            associatedGoalId = createdGoalId,
                                            plannedDay = currentDayOfWeek
                                        )
                                    )
                                    parsedTaskCount++
                                } else if (parsedGoalCount < 1) {
                                    val title = firstPart
                                    val sector = parts[1].trim()
                                    val target = parts[2].trim().toFloatOrNull() ?: 50f
                                    createdGoalId = repository.insertGoal(Goal(title = title, sector = sector, targetValue = target)).toInt()
                                    parsedGoalCount++
                                }
                            }
                        }
                    }
                }

                // If parsing yielded nothing or input response was invalid (due to missing key or network)
                if (parsedGoalCount == 0 || parsedTaskCount == 0) {
                    forgeLocalFallbackQuests(userFocusTopic)
                    aiCoPilotLoading = false
                    onCompleted("Successfully forged offline quests! (Note: Chronos crafted these locally using stored roleplay presets because the Gemini API is offline/unconfigured).")
                } else {
                    aiCoPilotLoading = false
                    onCompleted("Successfully forged $parsedGoalCount Goal and $parsedTaskCount Tasks for '$userFocusTopic'!")
                }
            } catch (e: Exception) {
                // If any error occurs, guarantee task creation through local fallback!
                try {
                    forgeLocalFallbackQuests(userFocusTopic)
                    aiCoPilotLoading = false
                    onCompleted("Successfully forged offline quests! (Chronos activated emergency backup templates).")
                } catch (ex: Exception) {
                    aiCoPilotLoading = false
                    onCompleted("Error in forging: ${e.localizedMessage}")
                }
            }
        }
    }

    val companionChatHistory = androidx.compose.runtime.mutableStateListOf<Pair<String, String>>()
    var isCompanionChatLoading by mutableStateOf(false)

    fun queryCompanion(message: String) {
        if (message.isBlank()) return
        companionChatHistory.add("User" to message)
        isCompanionChatLoading = true
        viewModelScope.launch {
            try {
                val statsValue = userStats.value
                val activeTasksStr = tasks.value.filter { !it.completed }.joinToString("\n") {
                    "- [Q${it.matrixQuadrant}] ${it.title} in sector ${it.sector}"
                }
                val contextPrompt = """
                    You are Chronos, a legendary RPG Productivity Mentor and AI Companion.
                    User level: ${statsValue?.level ?: 1}
                    XP: ${statsValue?.xp ?: 0}/${statsValue?.xpForNextLevel ?: 250}
                    Streak: ${statsValue?.streakDays ?: 1} Days
                    Active Quests:
                    $activeTasksStr
                    
                    User message: "$message"
                    
                    Respond to the user's message as Chronos. Keep your tone immersive, inspiring, RPG-flavored, yet practical. Keep it relatively concise (under 130 words), direct, and incredibly motivating.
                """.trimIndent()
                val systemPrompt = "You are Chronos, an elite productivity companion, gamification trainer, and wise RPG mentor."
                val reply = GeminiClient.generateAiContent(contextPrompt, systemPrompt)
                companionChatHistory.add("Chronos" to reply)
            } catch (e: Exception) {
                companionChatHistory.add("Chronos" to "The cosmic gateway is slightly unstable, Hero. Let's try syncing again. (Error: ${e.localizedMessage})")
            } finally {
                isCompanionChatLoading = false
            }
        }
    }

    fun clearCompanionChat() {
        companionChatHistory.clear()
    }
}

class QuestViewModelFactory(private val repository: QuestRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuestViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
