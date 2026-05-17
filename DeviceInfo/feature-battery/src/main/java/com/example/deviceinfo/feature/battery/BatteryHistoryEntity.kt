package com.example.deviceinfo.feature.battery

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_history")
data class BatteryHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,        // System.currentTimeMillis()
    val percent: Int,
    val temperatureCelsius: Float,
    val voltageMv: Int,
    val isCharging: Boolean
)
