package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Lead

@Database(entities = [Lead::class], version = 2, exportSchema = false)
abstract class LeadDatabase : RoomDatabase() {
    abstract fun leadDao(): LeadDao

    companion object {
        @Volatile
        private var INSTANCE: LeadDatabase? = null

        fun getDatabase(context: Context): LeadDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LeadDatabase::class.java,
                    "lead_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
