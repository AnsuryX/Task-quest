package com.example

import android.app.Application
import com.example.data.database.QuestDatabase
import com.example.data.repository.QuestRepository

class TaskQuestApplication : Application() {

    val database by lazy { QuestDatabase.getDatabase(this) }
    val repository by lazy { QuestRepository(database.questDao()) }

    override fun onCreate() {
        super.onCreate()
    }
}
