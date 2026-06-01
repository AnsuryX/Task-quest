package com.example

import android.app.Application
import com.example.data.database.QuestDatabase
import com.example.data.repository.QuestRepository
import com.example.util.NotificationAndSoundHelper

class TaskQuestApplication : Application() {

    val database by lazy { QuestDatabase.getDatabase(this) }
    val repository by lazy { QuestRepository(database.questDao()) }

    override fun onCreate() {
        super.onCreate()
        // Initialize notification channels for the entire app life cycle
        NotificationAndSoundHelper.createNotificationChannels(this)
    }
}
