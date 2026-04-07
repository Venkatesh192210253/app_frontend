package com.simats.myfitnessbuddy.data.local

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {
    private const val PREF_NAME = "MyFitnessBuddyPrefs"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun getPrefs(): SharedPreferences {
        return prefs ?: throw IllegalStateException("SettingsManager must be initialized before use")
    }

    // Goal Keys
    private const val KEY_CALORIE_GOAL = "calorie_goal"
    private const val KEY_CURRENT_WEIGHT = "current_weight"
    private const val KEY_START_WEIGHT = "start_weight"
    private const val KEY_TARGET_WEIGHT = "target_weight"
    private const val KEY_WEEKLY_GOAL = "weekly_goal"
    private const val KEY_BODY_FAT = "body_fat"
    private const val KEY_PROTEIN = "protein"
    private const val KEY_CARBS = "carbs"
    private const val KEY_FATS = "fats"

    // Profile Keys
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name" // This will store username
    private const val KEY_FULL_NAME = "full_name" // This will store full name
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_PROFILE_PICTURE_URI = "profile_picture_uri"

    // Notification Keys
    private const val KEY_MASTER_PUSH = "master_push"
    private const val KEY_WORKOUT_REMINDERS = "workout_reminders"
    private const val KEY_QUIET_HOURS_FROM = "quiet_hours_from"
    private const val KEY_QUIET_HOURS_TO = "quiet_hours_to"

    // Privacy Keys
    private const val KEY_PRIVATE_ACCOUNT = "private_account"
    private const val KEY_SHOW_IN_SEARCH = "show_in_search"
    private const val KEY_SHOW_ACTIVITY = "show_activity"
    private const val KEY_DIARY_DATA = "diary_data"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_GOALS_COMPLETED = "goals_completed"
    private const val KEY_ONBOARDING_SEEN = "onboarding_seen"
    private const val KEY_ADAPTIVE_MODE = "adaptive_mode"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    private fun userKey(baseKey: String): String {
        val uid = getPrefs().getString(KEY_USER_ID, "") ?: ""
        return if (uid.isEmpty()) baseKey else "${baseKey}_$uid"
    }

    var calorieGoal: String
        get() = getPrefs().getString(userKey(KEY_CALORIE_GOAL), "2500") ?: "2500"
        set(value) = getPrefs().edit().putString(userKey(KEY_CALORIE_GOAL), value).apply()

    var currentWeight: String
        get() = getPrefs().getString(userKey(KEY_CURRENT_WEIGHT), "0") ?: "0"
        set(value) = getPrefs().edit().putString(userKey(KEY_CURRENT_WEIGHT), value).apply()

    var startWeight: String
        get() = getPrefs().getString(userKey(KEY_START_WEIGHT), "0") ?: "0"
        set(value) = getPrefs().edit().putString(userKey(KEY_START_WEIGHT), value).apply()

    var targetWeight: String
        get() = getPrefs().getString(userKey(KEY_TARGET_WEIGHT), "0") ?: "0"
        set(value) = getPrefs().edit().putString(userKey(KEY_TARGET_WEIGHT), value).apply()

    var weeklyGoal: String
        get() = getPrefs().getString(userKey(KEY_WEEKLY_GOAL), "0.5") ?: "0.5"
        set(value) = getPrefs().edit().putString(userKey(KEY_WEEKLY_GOAL), value).apply()

    var protein: String
        get() = getPrefs().getString(userKey(KEY_PROTEIN), "160") ?: "160"
        set(value) = getPrefs().edit().putString(userKey(KEY_PROTEIN), value).apply()

    var carbs: String
        get() = getPrefs().getString(userKey(KEY_CARBS), "250") ?: "250"
        set(value) = getPrefs().edit().putString(userKey(KEY_CARBS), value).apply()

    var fats: String
        get() = getPrefs().getString(userKey(KEY_FATS), "70") ?: "70"
        set(value) = getPrefs().edit().putString(userKey(KEY_FATS), value).apply()

    var userId: String
        get() = getPrefs().getString(KEY_USER_ID, "") ?: ""
        set(value) = getPrefs().edit().putString(KEY_USER_ID, value).apply()

    var userName: String
        get() = getPrefs().getString(userKey(KEY_USER_NAME), "") ?: ""
        set(value) = getPrefs().edit().putString(userKey(KEY_USER_NAME), value).apply()

    var fullName: String
        get() = getPrefs().getString(userKey(KEY_FULL_NAME), "") ?: ""
        set(value) = getPrefs().edit().putString(userKey(KEY_FULL_NAME), value).apply()

    var userEmail: String
        get() = getPrefs().getString(userKey(KEY_USER_EMAIL), "") ?: ""
        set(value) = getPrefs().edit().putString(userKey(KEY_USER_EMAIL), value).apply()

    var profilePictureUri: String?
        get() = getPrefs().getString(userKey(KEY_PROFILE_PICTURE_URI), null)
        set(value) = getPrefs().edit().putString(userKey(KEY_PROFILE_PICTURE_URI), value).apply()

    var masterPush: Boolean
        get() = getPrefs().getBoolean(userKey(KEY_MASTER_PUSH), true)
        set(value) = getPrefs().edit().putBoolean(userKey(KEY_MASTER_PUSH), value).apply()

    var workoutReminders: Boolean
        get() = getPrefs().getBoolean(userKey(KEY_WORKOUT_REMINDERS), true)
        set(value) = getPrefs().edit().putBoolean(userKey(KEY_WORKOUT_REMINDERS), value).apply()

    var quietHoursFrom: String?
        get() = getPrefs().getString(userKey(KEY_QUIET_HOURS_FROM), "22:00")
        set(value) = getPrefs().edit().putString(userKey(KEY_QUIET_HOURS_FROM), value).apply()

    var quietHoursTo: String?
        get() = getPrefs().getString(userKey(KEY_QUIET_HOURS_TO), "07:00")
        set(value) = getPrefs().edit().putString(userKey(KEY_QUIET_HOURS_TO), value).apply()

    var privateAccount: Boolean
        get() = getPrefs().getBoolean(userKey(KEY_PRIVATE_ACCOUNT), false)
        set(value) = getPrefs().edit().putBoolean(userKey(KEY_PRIVATE_ACCOUNT), value).apply()

    var showInSearch: Boolean
        get() = getPrefs().getBoolean(userKey(KEY_SHOW_IN_SEARCH), true)
        set(value) = getPrefs().edit().putBoolean(userKey(KEY_SHOW_IN_SEARCH), value).apply()

    var showActivity: Boolean
        get() = getPrefs().getBoolean(userKey(KEY_SHOW_ACTIVITY), true)
        set(value) = getPrefs().edit().putBoolean(userKey(KEY_SHOW_ACTIVITY), value).apply()

    var diaryData: String
        get() = getPrefs().getString(userKey(KEY_DIARY_DATA), "") ?: ""
        set(value) = getPrefs().edit().putString(userKey(KEY_DIARY_DATA), value).apply()

    var workoutsCompleted: Int
        get() = getPrefs().getInt(userKey(KEY_WORKOUTS), 0)
        set(value) = getPrefs().edit().putInt(userKey(KEY_WORKOUTS), value).apply()

    var userXp: Int
        get() = getPrefs().getInt(userKey(KEY_XP), 0)
        set(value) = getPrefs().edit().putInt(userKey(KEY_XP), value).apply()

    var userStreak: Int
        get() = getPrefs().getInt(userKey(KEY_STREAK), 0)
        set(value) = getPrefs().edit().putInt(userKey(KEY_STREAK), value).apply()

    var userLevel: Int
        get() = getPrefs().getInt(userKey(KEY_LEVEL), 1)
        set(value) = getPrefs().edit().putInt(userKey(KEY_LEVEL), value).apply()

    var authToken: String?
        get() = getPrefs().getString(KEY_AUTH_TOKEN, null)
        set(value) = getPrefs().edit().putString(KEY_AUTH_TOKEN, value).apply()

    var refreshToken: String?
        get() = getPrefs().getString(KEY_REFRESH_TOKEN, null)
        set(value) = getPrefs().edit().putString(KEY_REFRESH_TOKEN, value).apply()

    var goalsCompleted: Boolean
        get() = getPrefs().getBoolean(userKey(KEY_GOALS_COMPLETED), false)
        set(value) = getPrefs().edit().putBoolean(userKey(KEY_GOALS_COMPLETED), value).apply()

    var onboardingSeen: Boolean
        get() = getPrefs().getBoolean(userKey(KEY_ONBOARDING_SEEN), false)
        set(value) = getPrefs().edit().putBoolean(userKey(KEY_ONBOARDING_SEEN), value).apply()

    var isAdaptiveModeEnabled: Boolean
        get() = getPrefs().getBoolean(userKey(KEY_ADAPTIVE_MODE), false)
        set(value) = getPrefs().edit().putBoolean(userKey(KEY_ADAPTIVE_MODE), value).apply()

    // AI Persistence
    private const val KEY_LAST_AI_DATE = "last_ai_date"
    private const val KEY_CACHED_AI_IMPROVEMENTS = "cached_ai_improvements"
    private const val KEY_CACHED_AI_WORKOUTS = "cached_ai_workouts"
    private const val KEY_APPLIED_AI_SUGGESTIONS = "applied_ai_suggestions"
    private const val KEY_ADDED_AI_WORKOUTS = "added_ai_workouts"
    private const val KEY_CACHED_AI_INSIGHT = "cached_ai_insight"
    private const val KEY_CACHED_AI_PROGRESS = "cached_ai_progress"
    
    // Step Tracking
    private const val KEY_STEPS_OFFSET = "steps_offset"
    private const val KEY_TOTAL_STEPS_TODAY = "total_steps_today"
    private const val KEY_LAST_STEP_COUNT = "last_step_count"
    private const val KEY_LAST_STEP_DATE = "last_step_date"
    private const val KEY_STEP_GOAL = "step_goal"
    private const val KEY_WORKOUTS = "workouts_completed"
    private const val KEY_XP = "user_xp"
    private const val KEY_STREAK = "user_streak"
    private const val KEY_LEVEL = "user_level"

    var lastAiDate: String
        get() = getPrefs().getString(userKey(KEY_LAST_AI_DATE), "") ?: ""
        set(value) = getPrefs().edit().putString(userKey(KEY_LAST_AI_DATE), value).apply()

    var cachedAiImprovements: String
        get() = getPrefs().getString(userKey(KEY_CACHED_AI_IMPROVEMENTS), "[]") ?: "[]"
        set(value) = getPrefs().edit().putString(userKey(KEY_CACHED_AI_IMPROVEMENTS), value).apply()

    var cachedAiWorkouts: String
        get() = getPrefs().getString(userKey(KEY_CACHED_AI_WORKOUTS), "[]") ?: "[]"
        set(value) = getPrefs().edit().putString(userKey(KEY_CACHED_AI_WORKOUTS), value).apply()

    var appliedAiSuggestions: Set<String>
        get() = getPrefs().getStringSet(userKey(KEY_APPLIED_AI_SUGGESTIONS), emptySet()) ?: emptySet()
        set(value) = getPrefs().edit().putStringSet(userKey(KEY_APPLIED_AI_SUGGESTIONS), value).apply()

    var addedAiWorkouts: Set<String>
        get() = getPrefs().getStringSet(userKey(KEY_ADDED_AI_WORKOUTS), emptySet()) ?: emptySet()
        set(value) = getPrefs().edit().putStringSet(userKey(KEY_ADDED_AI_WORKOUTS), value).apply()

    var cachedAiInsight: String
        get() = getPrefs().getString(userKey(KEY_CACHED_AI_INSIGHT), "") ?: ""
        set(value) = getPrefs().edit().putString(userKey(KEY_CACHED_AI_INSIGHT), value).apply()

    var cachedAiProgress: String
        get() = getPrefs().getString(userKey(KEY_CACHED_AI_PROGRESS), "[]") ?: "[]"
        set(value) = getPrefs().edit().putString(userKey(KEY_CACHED_AI_PROGRESS), value).apply()

    // Step Tracking Properties
    var stepsOffset: Int
        get() = getPrefs().getInt(userKey(KEY_STEPS_OFFSET), 0)
        set(value) = getPrefs().edit().putInt(userKey(KEY_STEPS_OFFSET), value).apply()

    var totalStepsToday: Int
        get() = getPrefs().getInt(userKey(KEY_TOTAL_STEPS_TODAY), 0)
        set(value) = getPrefs().edit().putInt(userKey(KEY_TOTAL_STEPS_TODAY), value).apply()

    var lastStepCount: Int
        get() = getPrefs().getInt(userKey(KEY_LAST_STEP_COUNT), 0)
        set(value) = getPrefs().edit().putInt(userKey(KEY_LAST_STEP_COUNT), value).apply()

    var lastStepDate: String
        get() = getPrefs().getString(userKey(KEY_LAST_STEP_DATE), "") ?: ""
        set(value) = getPrefs().edit().putString(userKey(KEY_LAST_STEP_DATE), value).apply()
        
    var stepGoal: Int
        get() = getPrefs().getInt(userKey(KEY_STEP_GOAL), 10000)
        set(value) = getPrefs().edit().putInt(userKey(KEY_STEP_GOAL), value).apply()
}
