package com.example.data.repository

import com.example.data.dao.QuestDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

class QuestRepository(private val questDao: QuestDao) {

    val allTasks: Flow<List<Task>> = questDao.getAllTasks()
    val allGoals: Flow<List<Goal>> = questDao.getAllGoals()
    val allPomodoroSessions: Flow<List<PomodoroSession>> = questDao.getAllPomodoroSessions()
    val userStatsFlow: Flow<UserStats?> = questDao.getUserStatsFlow()
    
    val allIntentions: Flow<List<ImplementationIntention>> = questDao.getAllIntentions()
    val allCommitmentContracts: Flow<List<CommitmentContract>> = questDao.getAllCommitmentContracts()
    val allWeeklyReflections: Flow<List<WeeklyReflection>> = questDao.getAllWeeklyReflections()

    // --- Tasks CRUD ---
    suspend fun insertTask(task: Task) = questDao.insertTask(task)
    suspend fun updateTask(task: Task) = questDao.updateTask(task)
    suspend fun deleteTask(task: Task) = questDao.deleteTask(task)
    suspend fun deleteTaskById(id: Int) = questDao.deleteTaskById(id)

    // --- Goals CRUD ---
    suspend fun insertGoal(goal: Goal) = questDao.insertGoal(goal)
    suspend fun updateGoal(goal: Goal) = questDao.updateGoal(goal)
    suspend fun deleteGoal(goal: Goal) = questDao.deleteGoal(goal)
    suspend fun deleteGoalById(id: Int) = questDao.deleteGoalById(id)

    // --- Intentions CRUD ---
    suspend fun insertIntention(intention: ImplementationIntention) = questDao.insertIntention(intention)
    suspend fun deleteIntentionById(id: Int) = questDao.deleteIntentionById(id)

    // --- Contracts CRUD ---
    suspend fun insertCommitmentContract(contract: CommitmentContract) = questDao.insertCommitmentContract(contract)
    suspend fun updateCommitmentContract(contract: CommitmentContract) = questDao.updateCommitmentContract(contract)
    suspend fun deleteCommitmentContractById(id: Int) = questDao.deleteCommitmentContractById(id)

    // --- Reflections CRUD ---
    suspend fun insertWeeklyReflection(reflection: WeeklyReflection) = questDao.insertWeeklyReflection(reflection)

    // --- Pomodoro Sessions ---
    suspend fun insertPomodoroSession(session: PomodoroSession) {
        questDao.insertPomodoroSession(session)
        
        // Log extra stats
        if (session.type == "work") {
            val stats = getOrCreateUserStats()
            val xpGain = if (session.isFocusZone) {
                // Earn 3 XP per focus minute in active Focus Zone shield!
                session.durationMinutes * 3
            } else {
                session.durationMinutes * 2
            }
            
            val newStats = stats.copy(
                focusMinutesTotal = stats.focusMinutesTotal + session.durationMinutes,
                totalFocusZoneMinutes = stats.totalFocusZoneMinutes + (if (session.isFocusZone) session.durationMinutes else 0),
                xp = stats.xp + xpGain
            )
            val leveledStats = handleLevelUp(newStats)
            questDao.insertUserStats(leveledStats)

            // Let's increment task associated Pomodoro spent count
            if (session.associatedTaskId != null) {
                val task = questDao.getTaskById(session.associatedTaskId)
                if (task != null) {
                    questDao.updateTask(task.copy(pomodorosSpent = task.pomodorosSpent + 1))
                }
            }
        }
    }

    // --- Core Gamification Mechanics ---
    suspend fun completeTask(task: Task) {
        val completedTask = task.copy(
            completed = true,
            completedAt = System.currentTimeMillis()
        )
        questDao.updateTask(completedTask)

        // If repeating, schedule next instance
        if (task.isRepeating && task.repeatInterval != "None") {
            val incrementMs = when (task.repeatInterval) {
                "Daily" -> 24 * 60 * 60 * 1000L
                "Weekly" -> 7 * 24 * 60 * 60 * 1000L
                "Monthly" -> 30L * 24 * 60 * 60 * 1000L
                else -> 0L
            }
            if (incrementMs > 0L) {
                val nextPlannedDate = (task.plannedDate ?: System.currentTimeMillis()) + incrementMs
                val sdf = java.text.SimpleDateFormat("EEEE", java.util.Locale.US)
                val nextPlannedDay = sdf.format(java.util.Date(nextPlannedDate))
                
                val nextTask = task.copy(
                    id = 0, // Auto-generate new primary key
                    completed = false,
                    completedAt = null,
                    pomodorosSpent = 0,
                    createdAt = System.currentTimeMillis(),
                    plannedDay = nextPlannedDay,
                    plannedDate = nextPlannedDate,
                    dueDate = task.dueDate?.let { it + incrementMs }
                )
                questDao.insertTask(nextTask)
            }
        }

        // Give User XP
        val currentStats = getOrCreateUserStats()
        
        // Check Streaks
        val now = System.currentTimeMillis()
        val currentEpochDay = TimeUnit.MILLISECONDS.toDays(now)
        val lastActiveEpochDay = TimeUnit.MILLISECONDS.toDays(currentStats.lastActiveTimestamp)
        
        var newStreak = currentStats.streakDays
        if (currentEpochDay == lastActiveEpochDay + 1) {
            newStreak += 1 // increment streak
        } else if (currentEpochDay > lastActiveEpochDay + 1) {
            newStreak = 1 // reset streak
        }

        val baseReward = completedTask.xpReward
        // 10% bonus XP per day of active streak (up to 50% max)
        val streakMultiplier = 1.0 + (java.lang.Math.min(5, newStreak - 1) * 0.1)
        val finalXpReward = (baseReward * streakMultiplier).toInt()

        val updatedStats = currentStats.copy(
            xp = currentStats.xp + finalXpReward,
            questsCompleted = currentStats.questsCompleted + 1,
            streakDays = newStreak,
            lastActiveTimestamp = now
        )

        val leveledStats = handleLevelUp(updatedStats)
        questDao.insertUserStats(leveledStats)

        // Increment related Goal: prioritises specific associatedGoalId, falls back to sector
        if (task.associatedGoalId != null) {
            val allGoals = questDao.getAllGoals().firstOrNull() ?: emptyList()
            val linkedGoal = allGoals.find { it.id == task.associatedGoalId }
            if (linkedGoal != null && !linkedGoal.completed) {
                val newValue = linkedGoal.currentValue + 15f // specific goal gets +15 points!
                val isCompleted = newValue >= linkedGoal.targetValue
                questDao.updateGoal(linkedGoal.copy(
                    currentValue = if (isCompleted) linkedGoal.targetValue else newValue,
                    completed = isCompleted
                ))
            }
        } else {
            // fallback to any active goals in task sector
            val activeGoals = questDao.getAllGoals().firstOrNull()?.filter { 
                it.sector.equals(task.sector, ignoreCase = true) && !it.completed 
            } ?: emptyList()

            for (goal in activeGoals) {
                val newValue = goal.currentValue + 10f // complete task gives +10 completion progress points to sector goal
                val isCompleted = newValue >= goal.targetValue
                questDao.updateGoal(goal.copy(
                    currentValue = if (isCompleted) goal.targetValue else newValue,
                    completed = isCompleted
                ))
            }
        }
    }

    private suspend fun getOrCreateUserStats(): UserStats {
        return questDao.getUserStats() ?: UserStats().also {
            questDao.insertUserStats(it)
        }
    }

    private fun handleLevelUp(stats: UserStats): UserStats {
        var level = stats.level
        var xp = stats.xp
        var loop = true
        while (loop) {
            val neededXp = level * 150 + 100 // Formula (Level 1: 250XP, Level 2: 400XP, etc.)
            if (xp >= neededXp) {
                xp -= neededXp
                level += 1
            } else {
                loop = false
            }
        }
        return stats.copy(level = level, xp = xp)
    }

    suspend fun resetUserStats() {
        questDao.insertUserStats(UserStats(id = 1, level = 1, xp = 0, streakDays = 1, focusMinutesTotal = 0, questsCompleted = 0))
    }

    suspend fun insertUserStats(stats: UserStats) {
        questDao.insertUserStats(stats)
    }

    suspend fun clearAllDatabaseTables() {
        questDao.clearTasks()
        questDao.clearGoals()
        questDao.clearPomodoroSessions()
        questDao.clearUserStats()
        questDao.clearIntentions()
        questDao.clearCommitmentContracts()
        questDao.clearWeeklyReflections()
    }
}
