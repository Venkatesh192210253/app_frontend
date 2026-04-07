package com.simats.myfitnessbuddy.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Query("SELECT * FROM daily_steps WHERE userId = :userId ORDER BY date DESC")
    fun getAllSteps(userId: String): Flow<List<StepEntry>>

    @Query("SELECT * FROM daily_steps WHERE date = :date AND userId = :userId LIMIT 1")
    suspend fun getStepsForDate(date: String, userId: String): StepEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stepEntry: StepEntry): Long

    @Query("UPDATE daily_steps SET steps = :steps, last_updated_time = :time WHERE date = :date AND userId = :userId")
    suspend fun updateSteps(date: String, userId: String, steps: Int, time: Long): Int

    @Query("SELECT SUM(steps) FROM daily_steps WHERE date >= :startDate AND userId = :userId")
    suspend fun getWeeklyTotal(startDate: String, userId: String): Int?

    @Query("SELECT AVG(steps) FROM daily_steps WHERE userId = :userId")
    suspend fun getAverageSteps(userId: String): Double?
}
