package com.simats.myfitnessbuddy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_steps")
data class StepEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String, // Added this field
    val date: String, // YYYY-MM-DD
    val steps: Int,
    val last_updated_time: Long = System.currentTimeMillis()
)
