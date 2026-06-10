package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.QuestDao
import com.example.data.model.Task
import com.example.data.model.Goal
import com.example.data.model.PomodoroSession
import com.example.data.model.UserStats
import com.example.data.model.ImplementationIntention
import com.example.data.model.CommitmentContract
import com.example.data.model.WeeklyReflection

@Database(
    entities = [
        Task::class, 
        Goal::class, 
        PomodoroSession::class, 
        UserStats::class,
        ImplementationIntention::class,
        CommitmentContract::class,
        WeeklyReflection::class
    ],
    version = 6,
    exportSchema = false
)
abstract class QuestDatabase : RoomDatabase() {

    abstract fun questDao(): QuestDao

    companion object {
        @Volatile
        private var INSTANCE: QuestDatabase? = null

        fun getDatabase(context: Context): QuestDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuestDatabase::class.java,
                    "quest_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
