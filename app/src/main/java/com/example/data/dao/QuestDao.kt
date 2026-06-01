package com.example.data.dao

import androidx.room.*
import com.example.data.model.Task
import com.example.data.model.Goal
import com.example.data.model.PomodoroSession
import com.example.data.model.UserStats
import com.example.data.model.ImplementationIntention
import com.example.data.model.CommitmentContract
import com.example.data.model.WeeklyReflection
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {

    // --- Tasks ---
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)

    // --- Goals ---
    @Query("SELECT * FROM goals ORDER BY dueDate ASC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE sector = :sector")
    fun getGoalsBySector(sector: String): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal): Long

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Int)

    // --- Pomodoro Sessions ---
    @Query("SELECT * FROM pomodoro_sessions ORDER BY timestamp DESC")
    fun getAllPomodoroSessions(): Flow<List<PomodoroSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPomodoroSession(session: PomodoroSession): Long

    // --- User Stats ---
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStatsFlow(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun getUserStats(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(stats: UserStats)

    // --- Implementation Intentions (If-Then Planner) ---
    @Query("SELECT * FROM implementation_intentions ORDER BY createdAt DESC")
    fun getAllIntentions(): Flow<List<ImplementationIntention>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntention(intention: ImplementationIntention): Long

    @Query("DELETE FROM implementation_intentions WHERE id = :id")
    suspend fun deleteIntentionById(id: Int)

    // --- Commitment Contracts ---
    @Query("SELECT * FROM commitment_contracts ORDER BY dueDate ASC")
    fun getAllCommitmentContracts(): Flow<List<CommitmentContract>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitmentContract(contract: CommitmentContract): Long

    @Update
    suspend fun updateCommitmentContract(contract: CommitmentContract)

    @Query("DELETE FROM commitment_contracts WHERE id = :id")
    suspend fun deleteCommitmentContractById(id: Int)

    // --- Weekly Reflections ---
    @Query("SELECT * FROM weekly_reflections ORDER BY createdAt DESC")
    fun getAllWeeklyReflections(): Flow<List<WeeklyReflection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyReflection(reflection: WeeklyReflection): Long

    // --- Bulk Clearing Queries for Backups ---
    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM goals")
    suspend fun clearGoals()

    @Query("DELETE FROM pomodoro_sessions")
    suspend fun clearPomodoroSessions()

    @Query("DELETE FROM user_stats")
    suspend fun clearUserStats()

    @Query("DELETE FROM implementation_intentions")
    suspend fun clearIntentions()

    @Query("DELETE FROM commitment_contracts")
    suspend fun clearCommitmentContracts()

    @Query("DELETE FROM weekly_reflections")
    suspend fun clearWeeklyReflections()
}
