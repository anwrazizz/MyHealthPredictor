package com.example.myhealthpredictor

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PhysicalActivity::class, WeightLog::class, PredictionHistory::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun physicalActivityDao(): PhysicalActivityDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun predictionHistoryDao(): PredictionHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_health_predictor_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
