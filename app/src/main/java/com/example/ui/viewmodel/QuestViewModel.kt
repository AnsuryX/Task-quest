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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    // --- Pomodoro Ticker Logic ---
    fun toggleTimer() {
        if (isTimerRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        isTimerRunning = true
        if (isFocusZoneEnabled && pomodoroMode == "work") {
            isSuppressionActive = true
            companionNudge = "🔊 DISTRACTIONS SILENCED: Focus Shield engaged. Crush your objectives, Hero!"
        }
        timerJob = viewModelScope.launch {
            while (countDownSeconds > 0 && isTimerRunning) {
                delay(1000)
                countDownSeconds -= 1
            }
            if (countDownSeconds == 0) {
                onTimerFinished()
            }
        }
    }

    fun pauseTimer() {
        isTimerRunning = false
        isSuppressionActive = false
        timerJob?.cancel()
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
    }

    private fun onTimerFinished() {
        isTimerRunning = false
        isSuppressionActive = false
        timerJob?.cancel()
        
        viewModelScope.launch {
            val duration = when (pomodoroMode) {
                "work" -> 25
                "short_break" -> 5
                "long_break" -> 15
                else -> 25
            }
            // Save Pomodoro Session records
            repository.insertPomodoroSession(
                PomodoroSession(
                    durationMinutes = duration,
                    type = pomodoroMode,
                    associatedTaskId = if (pomodoroMode == "work") selectedTaskId else null,
                    isFocusZone = (pomodoroMode == "work" && isFocusZoneEnabled)
                )
            )

            // Switch Mode Automatically to guide the user
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
        }
    }

    fun resetTimer() {
        pauseTimer()
        setTimerMode(pomodoroMode)
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

    fun addGoal(title: String, sector: String, targetValue: Float) {
        viewModelScope.launch {
            repository.insertGoal(
                Goal(
                    title = title,
                    sector = sector,
                    targetValue = targetValue,
                    currentValue = 0f
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

            val systemPrompt = "You are Chronos, an elite productivity companion, gamification trainer, and wise RPG mentor. Keep reports stylish, engaging, highly structured, in a gorgeous cosmic style."

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
        commitmentXpStake: Int = 0
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
            repository.insertTask(
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
                    commitmentXpStake = commitmentXpStake
                )
            )
            if (hasContract) {
                repository.insertCommitmentContract(
                    CommitmentContract(
                        taskTitle = title,
                        xpStake = commitmentXpStake,
                        penaltyDescription = "Yield character stake penalty if defaulted",
                        dueDate = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
                    )
                )
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
            
            val systemPrompt = "You are Chronos, a wise RPG ranger. Keep reviews stylish, structured, encouraging, in clean markdown."
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

    fun autoForgeWithCoPilot(userFocusTopic: String, onCompleted: (String) -> Unit) {
        aiCoPilotLoading = true
        viewModelScope.launch {
            val prompt = """
                You are Chronos, the elite productivity co-pilot.
                Create high-importance goals and action-oriented tasks for this topic: "$userFocusTopic"
                
                I need exactly 1 specific Goal and exactly 2 associated Tasks.
                Generate them using this STRICT parser pattern so that the APP can read them directly:
                
                ===GOAL_BEGIN===
                [Title of Goal]|[Life Area Sector like "Health","Business","Spiritual" or "Personal"]|[Target Progress Points e.g. 50]
                ===GOAL_END===
                
                ===TASKS_BEGIN===
                [Task 1 Title]|[Eisenhower Quadrant 1,2,3 or 4]|[Sector]|[Target Pomodoros]
                [Task 2 Title]|[Eisenhower Quadrant 1,2,3 or 4]|[Sector]|[Target Pomodoros]
                ===TASKS_END===
                
                Keep the titles highly inspiring and RPG-flavored.
            """.trimIndent()
            
            try {
                val response = GeminiClient.generateAiContent(prompt, "You are an elite productivity database co-pilot. You print strictly following requested split tokens without extra text outside of tokens.")
                
                // Parse Goal
                val goalRegex = "===GOAL_BEGIN===\\s*(.*?)\\s*===GOAL_END===".toRegex(RegexOption.DOT_MATCHES_ALL)
                val goalMatch = goalRegex.find(response)
                var parsedGoalCount = 0
                if (goalMatch != null) {
                    val lines = goalMatch.groupValues[1].split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    for (line in lines) {
                        val parts = line.split("|")
                        if (parts.size >= 3) {
                            val title = parts[0]
                            val sector = parts[1]
                            val target = parts[2].toFloatOrNull() ?: 50f
                            repository.insertGoal(Goal(title = title, sector = sector, targetValue = target))
                            parsedGoalCount++
                        }
                    }
                }

                // Parse Tasks
                val tasksRegex = "===TASKS_BEGIN===\\s*(.*?)\\s*===TASKS_END===".toRegex(RegexOption.DOT_MATCHES_ALL)
                val tasksMatch = tasksRegex.find(response)
                var parsedTaskCount = 0
                if (tasksMatch != null) {
                    val lines = tasksMatch.groupValues[1].split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    for (line in lines) {
                        val parts = line.split("|")
                        if (parts.size >= 4) {
                            val title = parts[0]
                            val quad = parts[1].toIntOrNull() ?: 2
                            val sector = parts[2]
                            val targetPomos = parts[3].toIntOrNull() ?: 2
                            
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
                                    targetPomodoros = targetPomos
                                )
                            )
                            parsedTaskCount++
                        }
                    }
                }
                
                aiCoPilotLoading = false
                onCompleted("Successfully forged $parsedGoalCount Goal and $parsedTaskCount Tasks for '$userFocusTopic'!")
            } catch (e: Exception) {
                aiCoPilotLoading = false
                onCompleted("Error in forging: ${e.localizedMessage}")
            }
        }
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
