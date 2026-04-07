package com.simats.myfitnessbuddy

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.simats.myfitnessbuddy.data.local.SettingsManager
import com.simats.myfitnessbuddy.data.local.AppDatabase
import com.simats.myfitnessbuddy.data.local.StepEntry
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.simats.myfitnessbuddy.data.remote.FirebaseStatsManager

class StepCounterService : Service(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var database: AppDatabase

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "step_counter_channel"
        private const val TAG = "StepCounterService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created")
        SettingsManager.init(applicationContext)
        database = AppDatabase.getDatabase(applicationContext)
        
        createNotificationChannel()
        val notification = createNotification("Starting step tracking...")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        // Activity Recognition permission check (Android 10+)
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true

        if (!hasPermission) {
            Log.e(TAG, "Activity Recognition permission not granted")
            updateNotification("Step tracking paused: Permission needed")
            return
        }

        // Prefer STEP_COUNTER for precision and battery efficiency
        stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        // Using SENSOR_DELAY_UI for more responsive updates when the app is open
        // The hardware still manages the cumulative count efficiently
        if (stepCounterSensor != null) {
            val registered = sensorManager?.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
            Log.d(TAG, "Step Counter Sensor Registered: $registered")
        } else if (stepDetectorSensor != null) {
            val registered = sensorManager?.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI)
            Log.d(TAG, "Step Detector Sensor Registered: $registered (Fallback)")
        } else {
            Log.e(TAG, "No hardware sensors for step tracking found!")
            updateNotification("Hardware step tracking unavailable")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val sensorValue = event.values[0].toInt()
                updateStepsCumulative(sensorValue)
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                // Only use detector if counter is not available
                if (stepCounterSensor == null && event.values[0] == 1.0f) {
                    incrementStepsByOne()
                }
            }
        }
    }

    private fun incrementStepsByOne() {
        val today = LocalDate.now().toString()
        val lastDate = SettingsManager.lastStepDate
        
        var totalStepsToday = SettingsManager.totalStepsToday
        
        if (today != lastDate) {
            Log.d(TAG, "New day detected via detector: $today")
            totalStepsToday = 1
            SettingsManager.lastStepDate = today
        } else {
            totalStepsToday += 1
        }
        
        SettingsManager.totalStepsToday = totalStepsToday
        updateNotification("Today: $totalStepsToday steps")
        Log.d(TAG, "Detector increment: $totalStepsToday")
    }

    private fun updateStepsCumulative(sensorValue: Int) {
        val today = LocalDate.now().toString()
        val lastDate = SettingsManager.lastStepDate
        
        var currentTotalSteps = SettingsManager.totalStepsToday
        val lastKnownSensorValue = SettingsManager.lastStepCount

        // 1. Day change detection
        if (today != lastDate) {
            Log.d(TAG, "New day detected: $today. Resetting steps.")
            currentTotalSteps = 0
            SettingsManager.lastStepDate = today
            // Reset the baseline for the new day
            SettingsManager.lastStepCount = sensorValue
            SettingsManager.totalStepsToday = 0
            updateNotification("Today: 0 steps")
            return
        }

        // 2. Baseline initialization (first run after install or data clear)
        if (lastKnownSensorValue <= 0) {
            Log.d(TAG, "Initializing first sensor baseline: $sensorValue")
            SettingsManager.lastStepCount = sensorValue
            return
        }

        // 3. Delta calculation
        if (sensorValue >= lastKnownSensorValue) {
            val delta = sensorValue - lastKnownSensorValue
            
            // Jitter filter: large jumps are possible if the app was suspended
            // hardware STEP_COUNTER is reliable, so we add the delta
            if (delta > 0) {
                currentTotalSteps += delta
                Log.d(TAG, "Steps delta: +$delta. Total today: $currentTotalSteps")
            }
        } else {
            // 4. Reboot detection: sensor value reset to 0 or smaller than previous
            Log.d(TAG, "Sensor reset detected (Reboot?). Sensor: $sensorValue, Prev: $lastKnownSensorValue")
            // The current sensor value is the steps taken since reboot
            currentTotalSteps += sensorValue
        }
        
        // 5. Update persistence
        SettingsManager.lastStepCount = sensorValue
        SettingsManager.totalStepsToday = currentTotalSteps
        
        saveStepsToDatabase(today, currentTotalSteps)
        
        // 6. Update notification
        updateNotification("Today: $currentTotalSteps steps")
    }

    private var lastDbUpdateSteps = -1
    private var lastDbUpdateTime = 0L

    private fun saveStepsToDatabase(date: String, steps: Int) {
        val currentTime = System.currentTimeMillis()
        val userId = SettingsManager.userId // Get current user
        if (userId.isEmpty()) return // Don't save if no user is logged in
        
        // Throttle DB updates: every 10 steps or every 30 seconds
        if (Math.abs(steps - lastDbUpdateSteps) < 10 && currentTime - lastDbUpdateTime < 30000) return
        
        lastDbUpdateSteps = steps
        lastDbUpdateTime = currentTime
        
        scope.launch {
            try {
                val dao = database.stepDao()
                val existing = dao.getStepsForDate(date, userId)
                if (existing != null) {
                    dao.updateSteps(date, userId, steps, currentTime)
                } else {
                    dao.insertOrUpdate(StepEntry(userId = userId, date = date, steps = steps, last_updated_time = currentTime))
                }
                Log.d(TAG, "Saved $steps steps to local DB for $date (User: $userId)")
                
                // --- REMOTE SYNC ---
                // Sync to Django (Permanent Database)
                try {
                    val response = RetrofitClient.apiService.updateDailyStats(mapOf("steps" to steps))
                    if (response.isSuccessful) {
                        Log.d(TAG, "Synced $steps steps to Django backend")
                    } else {
                        Log.e(TAG, "Django sync failed: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Django sync exception: ${e.message}")
                }

                // Sync to Firebase (Real-time comparison)
                try {
                    // We need workouts/xp/level too for full sync, but steps is the most frequent
                    // Fetch existing profile data if possible, or just send steps
                    FirebaseStatsManager.updateMyStats(
                        steps = steps,
                        workouts = SettingsManager.workoutsCompleted,
                        xp = SettingsManager.userXp,
                        streak = SettingsManager.userStreak,
                        level = SettingsManager.userLevel
                    )
                    Log.d(TAG, "Synced $steps steps to Firebase")
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase sync exception: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save steps to DB: ${e.message}")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Step Counter Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(content: String): android.app.Notification {
        val notificationIntent = Intent(this, DashboardActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Step Counter")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher) // Use launcher icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification: android.app.Notification = createNotification(content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(this)
        Log.d(TAG, "Service Destroyed")
    }
}
