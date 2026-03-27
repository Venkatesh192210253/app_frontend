package com.simats.myfitnessbuddy.data.remote

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.simats.myfitnessbuddy.data.local.SettingsManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class UserStats(
    val userId: String = "",
    val username: String = "",
    val steps: Int = 0,
    val workouts: Int = 0,
    val xp: Int = 0,
    val streak: Int = 0,
    val level: Int = 1,
    val lastUpdated: Long = System.currentTimeMillis()
)

object FirebaseStatsManager {
    private const val TAG = "FirebaseStatsManager"
    
    private val database by lazy { FirebaseDatabase.getInstance() }
    private val statsRef by lazy { database.getReference("user_stats") }

    fun updateMyStats(steps: Int, workouts: Int, xp: Int, streak: Int, level: Int = 1) {
        try {
            val userId = SettingsManager.userId
            if (userId.isEmpty()) return

            val stats = UserStats(
                userId = userId,
                username = SettingsManager.userName,
                steps = steps,
                workouts = workouts,
                xp = xp,
                streak = streak,
                level = level,
                lastUpdated = System.currentTimeMillis()
            )

            statsRef.child(userId).setValue(stats)
                .addOnSuccessListener {
                    Log.d(TAG, "Stats updated in Firebase")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update stats in Firebase: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase not initialized: ${e.message}")
        }
    }

    fun observeUserStats(userId: String): Flow<UserStats?> = callbackFlow {
        try {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val stats = snapshot.getValue(UserStats::class.java)
                    trySend(stats)
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            statsRef.child(userId).addValueEventListener(listener)
            awaitClose { statsRef.child(userId).removeEventListener(listener) }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase observe failed: ${e.message}")
            trySend(null)
            close()
        }
    }
}
