package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val notes: String = "",
    val matrixQuadrant: Int, // 1 = Urgent & Important, 2 = Important & Not Urgent, 3 = Urgent & Not Important, 4 = Not Urgent & Not Important
    val sector: String, // e.g., "Business", "Health", "Spiritual", "Personal", etc.
    val xpReward: Int,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val pomodorosSpent: Int = 0,
    val targetPomodoros: Int = 1,
    
    // --- Behavioral & Planning Additions ----
    val plannedDay: String? = null, // "Monday", "Tuesday", "Wednesday", etc. (Weekly Horizon Planner)
    val plannedDate: Long? = null, // Epoch millisecond (Daily Planner)
    val temptationBundle: String = "", // Temptation Bundling ritual (e.g. "Siing matcha tea")
    val hasCommitmentContract: Boolean = false,
    val commitmentXpStake: Int = 0
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val sector: String, // "Business", "Health", "Spiritual", "Personal", "Financial"
    val targetValue: Float = 100f, // target completion or focus minutes
    val currentValue: Float = 0f,
    val dueDate: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000), // default 1 week
    val completed: Boolean = false
)

@Entity(tableName = "pomodoro_sessions")
data class PomodoroSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMinutes: Int,
    val type: String, // "work", "short_break", "long_break"
    val associatedTaskId: Int? = null,
    val isFocusZone: Boolean = false // Track when the system was running in extreme "Focus Zone" (silent alerts)
)

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1, // Only 1 row ever exists (ID=1)
    val level: Int = 1,
    val xp: Int = 0,
    val streakDays: Int = 1,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val focusMinutesTotal: Int = 0,
    val questsCompleted: Int = 0,
    val totalFocusZoneMinutes: Int = 0 // Track total Focus Zone metrics
) {
    val xpForNextLevel: Int
        get() = level * 150 + 100 // Leveling curve formula (e.g., Level 1 needs 250 XP, Level 2 needs 400 XP)
}

@Entity(tableName = "implementation_intentions")
data class ImplementationIntention(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val triggerSituation: String, // "IF I enter the study room after coffee..."
    val desiredAction: String, // "...THEN I will outline Q2 tasks..."
    val associatedTaskId: Int? = null,
    val location: String = "",
    val timeOfDay: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "commitment_contracts")
data class CommitmentContract(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskTitle: String,
    val xpStake: Int,
    val penaltyDescription: String,
    val dueDate: Long,
    val isSettled: Boolean = false,
    val isFulfilled: Boolean = false
)

@Entity(tableName = "weekly_reflections")
data class WeeklyReflection(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String, // e.g. "Week 23 Synthesis"
    val winsSummary: String,
    val lessonsLearned: String,
    val focusRating: Int, // 1 through 5 stars
    val aiInsight: String = "", // Chronos RPG review
    val createdAt: Long = System.currentTimeMillis()
)

