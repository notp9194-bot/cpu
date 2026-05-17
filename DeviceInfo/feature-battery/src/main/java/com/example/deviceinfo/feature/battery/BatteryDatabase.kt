package com.example.deviceinfo.feature.battery

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BatteryHistoryEntity::class], version = 1, exportSchema = false)
abstract class BatteryDatabase : RoomDatabase() {
    abstract fun dao(): BatteryHistoryDao

    companion object {
        @Volatile private var INSTANCE: BatteryDatabase? = null

        fun get(context: Context): BatteryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BatteryDatabase::class.java,
                    "battery_history.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
