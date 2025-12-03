package com.example.myhealthpredictor.Prediction

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myhealthpredictor.Prediction.PredictionHistory
import com.example.myhealthpredictor.Prediction.PredictionHistoryDao

// Menghapus PhysicalActivity dan WeightLog dari entities
@Database(entities = [PredictionHistory::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Menghapus DAO yang tidak digunakan lagi
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
                )
                .fallbackToDestructiveMigration() // Menambahkan ini agar tidak crash karena perubahan struktur tabel
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}