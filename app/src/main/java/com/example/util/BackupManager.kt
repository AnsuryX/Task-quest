package com.example.util

import android.util.Base64
import com.example.data.model.*
import com.example.data.repository.QuestRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object BackupManager {

    suspend fun exportDataToJson(repository: QuestRepository): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        // 1. Tasks
        val tasksArr = JSONArray()
        val tasks = repository.allTasks.first()
        for (t in tasks) {
            val jobj = JSONObject()
            jobj.put("id", t.id)
            jobj.put("title", t.title)
            jobj.put("notes", t.notes)
            jobj.put("matrixQuadrant", t.matrixQuadrant)
            jobj.put("sector", t.sector)
            jobj.put("xpReward", t.xpReward)
            jobj.put("completed", t.completed)
            jobj.put("createdAt", t.createdAt)
            jobj.put("completedAt", t.completedAt ?: JSONObject.NULL)
            jobj.put("pomodorosSpent", t.pomodorosSpent)
            jobj.put("targetPomodoros", t.targetPomodoros)
            jobj.put("plannedDay", t.plannedDay ?: JSONObject.NULL)
            jobj.put("plannedDate", t.plannedDate ?: JSONObject.NULL)
            jobj.put("temptationBundle", t.temptationBundle)
            jobj.put("hasCommitmentContract", t.hasCommitmentContract)
            jobj.put("commitmentXpStake", t.commitmentXpStake)
            tasksArr.put(jobj)
        }
        root.put("tasks", tasksArr)

        // 2. Goals
        val goalsArr = JSONArray()
        val goals = repository.allGoals.first()
        for (g in goals) {
            val jobj = JSONObject()
            jobj.put("id", g.id)
            jobj.put("title", g.title)
            jobj.put("sector", g.sector)
            jobj.put("targetValue", g.targetValue.toDouble())
            jobj.put("currentValue", g.currentValue.toDouble())
            jobj.put("dueDate", g.dueDate)
            jobj.put("completed", g.completed)
            goalsArr.put(jobj)
        }
        root.put("goals", goalsArr)

        // 3. Sessions
        val sessionsArr = JSONArray()
        val sessions = repository.allPomodoroSessions.first()
        for (s in sessions) {
            val jobj = JSONObject()
            jobj.put("id", s.id)
            jobj.put("timestamp", s.timestamp)
            jobj.put("durationMinutes", s.durationMinutes)
            jobj.put("type", s.type)
            jobj.put("associatedTaskId", s.associatedTaskId ?: JSONObject.NULL)
            jobj.put("isFocusZone", s.isFocusZone)
            sessionsArr.put(jobj)
        }
        root.put("sessions", sessionsArr)

        // 4. User Stats
        val stats = repository.userStatsFlow.first()
        if (stats != null) {
            val jobj = JSONObject()
            jobj.put("id", stats.id)
            jobj.put("level", stats.level)
            jobj.put("xp", stats.xp)
            jobj.put("streakDays", stats.streakDays)
            jobj.put("lastActiveTimestamp", stats.lastActiveTimestamp)
            jobj.put("focusMinutesTotal", stats.focusMinutesTotal)
            jobj.put("questsCompleted", stats.questsCompleted)
            jobj.put("totalFocusZoneMinutes", stats.totalFocusZoneMinutes)
            root.put("user_stats", jobj)
        }

        // 5. Intentions
        val intentionArr = JSONArray()
        val intentions = repository.allIntentions.first()
        for (i in intentions) {
            val jobj = JSONObject()
            jobj.put("id", i.id)
            jobj.put("triggerSituation", i.triggerSituation)
            jobj.put("desiredAction", i.desiredAction)
            jobj.put("associatedTaskId", i.associatedTaskId ?: JSONObject.NULL)
            jobj.put("location", i.location)
            jobj.put("timeOfDay", i.timeOfDay)
            jobj.put("createdAt", i.createdAt)
            intentionArr.put(jobj)
        }
        root.put("intentions", intentionArr)

        // 6. Contracts
        val contractArr = JSONArray()
        val contracts = repository.allCommitmentContracts.first()
        for (c in contracts) {
            val jobj = JSONObject()
            jobj.put("id", c.id)
            jobj.put("taskTitle", c.taskTitle)
            jobj.put("xpStake", c.xpStake)
            jobj.put("penaltyDescription", c.penaltyDescription)
            jobj.put("dueDate", c.dueDate)
            jobj.put("isSettled", c.isSettled)
            jobj.put("isFulfilled", c.isFulfilled)
            contractArr.put(jobj)
        }
        root.put("contracts", contractArr)

        // 7. Reflections
        val reflectionArr = JSONArray()
        val reflections = repository.allWeeklyReflections.first()
        for (r in reflections) {
            val jobj = JSONObject()
            jobj.put("id", r.id)
            jobj.put("title", r.title)
            jobj.put("winsSummary", r.winsSummary)
            jobj.put("lessonsLearned", r.lessonsLearned)
            jobj.put("focusRating", r.focusRating)
            jobj.put("aiInsight", r.aiInsight)
            jobj.put("createdAt", r.createdAt)
            reflectionArr.put(jobj)
        }
        root.put("reflections", reflectionArr)

        return root.toString()
    }

    suspend fun importDataFromJson(repository: QuestRepository, jsonStr: String): Boolean {
        try {
            val cleanStr = jsonStr.trim()
            val parsedStr = if (cleanStr.startsWith("ey") || cleanStr.startsWith("eyJ")) {
                val decodedBytes = Base64.decode(cleanStr, Base64.DEFAULT)
                String(decodedBytes, StandardCharsets.UTF_8)
            } else {
                cleanStr
            }

            val root = JSONObject(parsedStr)
            
            // Wipe all current tables first
            repository.clearAllDatabaseTables()

            // 1. Tasks
            if (root.has("tasks")) {
                val arr = root.getJSONArray("tasks")
                for (idx in 0 until arr.length()) {
                    val jobj = arr.getJSONObject(idx)
                    val task = Task(
                        id = 0, // auto-generate standard autoic indexes
                        title = jobj.getString("title"),
                        notes = jobj.optString("notes", ""),
                        matrixQuadrant = jobj.getInt("matrixQuadrant"),
                        sector = jobj.optString("sector", "Personal"),
                        xpReward = jobj.optInt("xpReward", 20),
                        completed = jobj.optBoolean("completed", false),
                        createdAt = jobj.optLong("createdAt", System.currentTimeMillis()),
                        completedAt = if (jobj.isNull("completedAt")) null else jobj.getLong("completedAt"),
                        pomodorosSpent = jobj.optInt("pomodorosSpent", 0),
                        targetPomodoros = jobj.optInt("targetPomodoros", 1),
                        plannedDay = if (jobj.isNull("plannedDay")) null else jobj.getString("plannedDay"),
                        plannedDate = if (jobj.isNull("plannedDate")) null else jobj.getLong("plannedDate"),
                        temptationBundle = jobj.optString("temptationBundle", ""),
                        hasCommitmentContract = jobj.optBoolean("hasCommitmentContract", false),
                        commitmentXpStake = jobj.optInt("commitmentXpStake", 0)
                    )
                    repository.insertTask(task)
                }
            }

            // 2. Goals
            if (root.has("goals")) {
                val arr = root.getJSONArray("goals")
                for (idx in 0 until arr.length()) {
                    val jobj = arr.getJSONObject(idx)
                    val goal = Goal(
                        id = 0,
                        title = jobj.getString("title"),
                        sector = jobj.getString("sector"),
                        targetValue = jobj.optDouble("targetValue", 100.0).toFloat(),
                        currentValue = jobj.optDouble("currentValue", 0.0).toFloat(),
                        dueDate = jobj.optLong("dueDate", System.currentTimeMillis()),
                        completed = jobj.optBoolean("completed", false)
                    )
                    repository.insertGoal(goal)
                }
            }

            // 3. Sessions
            if (root.has("sessions")) {
                val arr = root.getJSONArray("sessions")
                for (idx in 0 until arr.length()) {
                    val jobj = arr.getJSONObject(idx)
                    val s = PomodoroSession(
                        id = 0,
                        timestamp = jobj.optLong("timestamp", System.currentTimeMillis()),
                        durationMinutes = jobj.getInt("durationMinutes"),
                        type = jobj.getString("type"),
                        associatedTaskId = if (jobj.isNull("associatedTaskId")) null else jobj.getInt("associatedTaskId"),
                        isFocusZone = jobj.optBoolean("isFocusZone", false)
                    )
                    repository.insertPomodoroSession(s)
                }
            }

            // 5. Intentions
            if (root.has("intentions")) {
                val arr = root.getJSONArray("intentions")
                for (idx in 0 until arr.length()) {
                    val jobj = arr.getJSONObject(idx)
                    val intention = ImplementationIntention(
                        id = 0,
                        triggerSituation = jobj.getString("triggerSituation"),
                        desiredAction = jobj.getString("desiredAction"),
                        associatedTaskId = if (jobj.isNull("associatedTaskId")) null else jobj.getInt("associatedTaskId"),
                        location = jobj.optString("location", ""),
                        timeOfDay = jobj.optString("timeOfDay", ""),
                        createdAt = jobj.optLong("createdAt", System.currentTimeMillis())
                    )
                    repository.insertIntention(intention)
                }
            }

            // 6. Contracts
            if (root.has("contracts")) {
                val arr = root.getJSONArray("contracts")
                for (idx in 0 until arr.length()) {
                    val jobj = arr.getJSONObject(idx)
                    val contract = CommitmentContract(
                        id = 0,
                        taskTitle = jobj.getString("taskTitle"),
                        xpStake = jobj.getInt("xpStake"),
                        penaltyDescription = jobj.optString("penaltyDescription", ""),
                        dueDate = jobj.getLong("dueDate"),
                        isSettled = jobj.optBoolean("isSettled", false),
                        isFulfilled = jobj.optBoolean("isFulfilled", false)
                    )
                    repository.insertCommitmentContract(contract)
                }
            }

            // 7. Reflections
            if (root.has("reflections")) {
                val arr = root.getJSONArray("reflections")
                for (idx in 0 until arr.length()) {
                    val jobj = arr.getJSONObject(idx)
                    val reflection = WeeklyReflection(
                        id = 0,
                        title = jobj.getString("title"),
                        winsSummary = jobj.optString("winsSummary", ""),
                        lessonsLearned = jobj.optString("lessonsLearned", ""),
                        focusRating = jobj.optInt("focusRating", 5),
                        aiInsight = jobj.optString("aiInsight", ""),
                        createdAt = jobj.optLong("createdAt", System.currentTimeMillis())
                    )
                    repository.insertWeeklyReflection(reflection)
                }
            }

            // 4. User Stats (overriding generated defaults at end)
            if (root.has("user_stats")) {
                val jobj = root.getJSONObject("user_stats")
                val statObj = UserStats(
                    id = 1,
                    level = jobj.optInt("level", 1),
                    xp = jobj.optInt("xp", 0),
                    streakDays = jobj.optInt("streakDays", 1),
                    lastActiveTimestamp = jobj.optLong("lastActiveTimestamp", System.currentTimeMillis()),
                    focusMinutesTotal = jobj.optInt("focusMinutesTotal", 0),
                    questsCompleted = jobj.optInt("questsCompleted", 0),
                    totalFocusZoneMinutes = jobj.optInt("totalFocusZoneMinutes", 0)
                )
                repository.insertUserStats(statObj)
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
