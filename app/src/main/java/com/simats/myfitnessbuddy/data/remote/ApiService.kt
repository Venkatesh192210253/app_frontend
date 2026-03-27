package com.simats.myfitnessbuddy.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT

interface ApiService {
    @POST("api/auth/login/")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("api/token/refresh/")
    suspend fun refreshToken(@Body body: TokenRefreshRequest): Response<TokenRefreshResponse>

    @POST("api/auth/register/")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/otp/generate/")
    suspend fun generateOtp(@Body body: OtpGenerateRequest): Response<SimpleResponse>

    @POST("api/otp/verify/")
    suspend fun verifyOtp(@Body body: OtpVerifyRequest): Response<AuthResponse>

    @GET("api/auth/verify/")
    suspend fun verifyToken(@Header("Authorization") token: String? = null): Response<TokenVerificationResponse>

    @GET("api/profile/")
    suspend fun getProfile(): Response<UserProfileResponse>

    @PUT("api/profile/update/")
    suspend fun updateProfile(
        @Body body: ProfileResponse
    ): Response<UserProfileResponse>

    @retrofit2.http.Multipart
    @POST("api/profile/update/")
    suspend fun updateProfileMultipart(
        @retrofit2.http.Part("full_name") fullName: okhttp3.RequestBody?,
        @retrofit2.http.Part("bio") bio: okhttp3.RequestBody?,
        @retrofit2.http.Part profile_photo: okhttp3.MultipartBody.Part?
    ): Response<UserProfileResponse>

    @POST("api/onboarding/complete-goals/")
    suspend fun completeGoals(
        @Body body: Map<String, List<String>>
    ): Response<SimpleResponse>

    @POST("api/forgot-password/")
    suspend fun forgotPassword(@Body body: Map<String, String>): Response<SimpleResponse>

    @POST("api/verify-reset-otp/")
    suspend fun verifyResetOtp(@Body body: Map<String, String>): Response<SimpleResponse>

    @POST("api/reset-password/")
    suspend fun resetPassword(@Body body: Map<String, String>): Response<SimpleResponse>

    @GET("api/stats/daily/")
    suspend fun getDailyStats(): Response<DailyStatsResponse>

    @GET("api/dashboard/data/")
    suspend fun getDashboardData(): Response<DashboardDataResponse>

    @POST("api/stats/daily/")
    suspend fun updateDailyStats(
        @Body body: Map<String, Any>
    ): Response<DailyStatsResponse>

    @GET("api/stats/detailed/")
    suspend fun getDetailedStats(): Response<DetailedStatsResponse>

    @GET("api/stats/achievements/")
    suspend fun getAchievements(): Response<List<AchievementResponse>>

    // Friends and Groups
    @GET("api/friends/")
    suspend fun getFriends(): Response<FriendsListResponse>

    @GET("api/friends/search/")
    suspend fun searchUsers(
        @retrofit2.http.Query("q") query: String
    ): Response<List<UserData>>

    @GET("api/friends/requests/")
    suspend fun getFriendRequests(): Response<FriendRequestsResponse>

    @GET("api/friends/suggestions/")
    suspend fun getSuggestedFriends(): Response<List<UserData>>

    @POST("api/friends/accept/")
    suspend fun acceptFriendRequest(
        @Body body: Map<String, String>
    ): Response<SimpleResponse>

    @POST("api/friends/reject/")
    suspend fun rejectFriendRequest(
        @Body body: Map<String, String>
    ): Response<SimpleResponse>

    @POST("api/friends/remove/")
    suspend fun removeFriend(
        @Body body: Map<String, String>
    ): Response<SimpleResponse>

    @GET("api/groups/my/")
    suspend fun getGroups(): Response<List<GroupResponse>>

    @POST("api/groups/create/")
    suspend fun createGroup(
        @Body body: CreateGroupRequest
    ): Response<SimpleResponse>

    @POST("api/groups/accept/")
    suspend fun acceptGroupInvite(
        @Body body: Map<String, String>
    ): Response<SimpleResponse>

    @GET("api/groups/detail/{group_id}/")
    suspend fun getGroupDetail(
        @retrofit2.http.Path("group_id") groupId: String
    ): Response<GroupDetailResponse>

    @POST("api/groups/create-challenge/")
    suspend fun createChallenge(
        @Body body: Map<String, String>
    ): Response<SimpleResponse>

    @GET("api/groups/challenges/{group_id}/")
    suspend fun getGroupChallenges(
        @retrofit2.http.Path("group_id") groupId: String
    ): Response<List<ChallengeResponse>>

    @GET("api/battles/active/")
    suspend fun getActiveBattles(): Response<List<BattleResponse>>

    @POST("api/friends/send-request/")
    suspend fun sendFriendRequest(
        @Body body: Map<String, String>
    ): Response<SimpleResponse>

    @GET("api/friends/compare/{friend_id}/")
    suspend fun compareStats(
        @retrofit2.http.Path("friend_id") friendId: String
    ): Response<CompareStatsResponse>

    @POST("api/groups/delete/{group_id}/")
    suspend fun deleteGroup(
        @retrofit2.http.Path("group_id") groupId: String
    ): Response<SimpleResponse>

    @POST("api/groups/challenges/delete/{challenge_id}/")
    suspend fun deleteChallenge(
        @retrofit2.http.Path("challenge_id") challengeId: String
    ): Response<SimpleResponse>

    @POST("api/groups/challenges/join/{challenge_id}/")
    suspend fun joinChallenge(
        @retrofit2.http.Path("challenge_id") challengeId: String
    ): Response<SimpleResponse>

    @GET("api/groups/challenges/participants/{challenge_id}/")
    suspend fun getChallengeParticipants(
        @retrofit2.http.Path("challenge_id") challengeId: String
    ): Response<List<ChallengeParticipantResponse>>

    @POST("api/groups/invite/")
    suspend fun inviteToGroup(
        @Body request: InviteToGroupRequest
    ): Response<SimpleResponse>

    @POST("api/groups/reject/")
    suspend fun rejectGroupInvite(
        @Body request: RejectGroupInviteRequest
    ): Response<SimpleResponse>

    @GET("api/groups/{group_id}/messages/")
    suspend fun getGroupMessages(
        @retrofit2.http.Path("group_id") groupId: String
    ): Response<List<GroupMessageResponse>>

    @POST("api/groups/{group_id}/messages/send/")
    suspend fun sendGroupMessage(
        @retrofit2.http.Path("group_id") groupId: String,
        @Body request: SendGroupMessageRequest
    ): Response<SimpleResponse>

    @POST("api/ai/chat/")
    suspend fun chatWithAi(
        @Body request: AiChatRequest
    ): Response<AiChatResponse>

    @POST("api/trained-ai/ask/")
    suspend fun askTrainedAi(
        @Body request: TrainedAiRequest
    ): Response<TrainedAiResponse>

    @GET("api/privacy-settings/")
    suspend fun getPrivacySettings(): Response<PrivacySettingsResponse>

    @PUT("api/privacy-settings/")
    suspend fun updatePrivacySettings(
        @Body body: PrivacySettingsResponse
    ): Response<PrivacySettingsResponse>

    @POST("api/auth/change-password/")
    suspend fun changePassword(
        @Body body: ChangePasswordRequest
    ): Response<SimpleResponse>

    @POST("api/auth/delete/")
    suspend fun deleteAccount(
        @Body body: Map<String, String>
    ): Response<SimpleResponse>


    @GET("api/profile/download-data/")
    suspend fun downloadData(): Response<Any>

    @POST("api/users/block/")
    suspend fun blockUser(
        @Body body: Map<String, String>
    ): Response<SimpleResponse>

    @POST("api/users/unblock/")
    suspend fun unblockUser(
        @Body body: Map<String, String>
    ): Response<SimpleResponse>

    @GET("api/users/blocked/")
    suspend fun getBlockedUsers(): Response<List<BlockedUserResponse>>

    @GET("api/sessions/")
    suspend fun getSessions(): Response<List<UserSessionResponse>>

    @POST("api/sessions/revoke/")
    suspend fun revokeSession(
        @Body body: Map<String, Int>
    ): Response<SimpleResponse>

    @GET("api/goal-settings/")
    suspend fun getGoalSettings(): Response<GoalSettingsResponse>

    @PUT("api/goal-settings/")
    suspend fun updateGoalSettings(
        @Body body: GoalSettingsResponse
    ): Response<GoalSettingsResponse>

    @PATCH("api/goal-settings/")
    suspend fun partialGoalUpdate(
        @Body body: Map<String, Any>
    ): Response<GoalSettingsResponse>

    // Food Diary
    @GET("api/food-diary/")
    suspend fun getDiaryDaily(
        @retrofit2.http.Query("date") date: String
    ): Response<DiaryDailyResponse>

    @POST("api/food-diary/add/")
    suspend fun addFoodEntry(
        @Body body: AddFoodEntryRequest
    ): Response<FoodEntryDto>

    @retrofit2.http.DELETE("api/food-diary/delete/{id}/")
    suspend fun deleteFoodEntry(
        @retrofit2.http.Path("id") id: Int
    ): Response<Unit>

    @GET("api/food-diary/smart-swaps/")
    suspend fun getSmartSwaps(
        @retrofit2.http.Query("food") food: String? = null
    ): Response<SmartSwapsResponse>

    @POST("api/diary/workout/add/")
    suspend fun logWorkout(
        @Body body: WorkoutLogRequest
    ): Response<WorkoutLogResponse>

    @GET("api/diary/workout/history/")
    suspend fun getWorkoutHistory(): Response<WorkoutHistoryResponse>

    @GET("api/diary/weight/")
    suspend fun getWeightHistory(): Response<List<WeightLogDto>>

    @POST("api/diary/weight/")
    suspend fun logWeight(
        @Body body: WeightLogRequest
    ): Response<WeightLogDto>

    @GET("api/diary/weight/goal/")
    suspend fun getWeightGoal(): Response<WeightGoalDto>

    @POST("api/diary/weight/goal/")
    suspend fun setWeightGoal(
        @Body body: WeightGoalRequest
    ): Response<WeightGoalDto>

    @GET("api/diary/workout/schedule/")
    suspend fun getWeeklySchedule(): Response<List<WeeklyScheduleResponse>>

    @POST("api/diary/workout/schedule/")
    suspend fun updateWeeklySchedule(
        @Body body: Map<String, Any?>
    ): Response<WeeklyScheduleResponse>

    @GET("api/diary/workout/today/")
    suspend fun getTodayWorkout(): Response<WorkoutTemplateResponse>

    @GET("api/diary/workout/templates/")
    suspend fun getWorkoutTemplates(): Response<List<WorkoutTemplateResponse>>

    @GET("api/food/search/")
    suspend fun searchFoods(
        @retrofit2.http.Query("query") query: String,
        @retrofit2.http.Query("meal_type") mealType: String? = null
    ): Response<List<FoodDto>>

    @GET("api/food/ai-search/")
    suspend fun searchFoodAi(
        @retrofit2.http.Query("query") query: String
    ): Response<List<FoodDto>>

    @retrofit2.http.Multipart
    @POST("api/food/ai-scan/")
    suspend fun scanFood(
        @retrofit2.http.Part image: okhttp3.MultipartBody.Part
    ): Response<FoodScanResponse>

    @GET("api/food/barcode/{barcode}/")
    suspend fun lookupBarcode(
        @retrofit2.http.Path("barcode") barcode: String
    ): Response<FoodDto>

    @POST("api/water/update/")
    suspend fun updateWaterIntake(
        @Body body: WaterUpdateRequest
    ): Response<SimpleResponse>

    @GET("api/support/faqs/")
    suspend fun getFaqs(): Response<List<FaqResponse>>

    @POST("api/support/tickets/")
    suspend fun createTicket(
        @Body body: TicketRequest
    ): Response<TicketResponse>

    // Notifications
    @GET("api/notifications/")
    suspend fun getNotifications(): Response<List<NotificationResponse>>

    @POST("api/notifications/{id}/mark-read/")
    suspend fun markNotificationRead(
        @retrofit2.http.Path("id") id: Int
    ): Response<Map<String, String>>

    @POST("api/notifications/mark-all-read/")
    suspend fun markAllNotificationsRead(): Response<Map<String, String>>
}

data class WaterUpdateRequest(
    val date: String,
    val glasses: Int
)

data class LoginRequest(
    val identifier: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val phone_number: String,
    val password: String
)

data class TokenRefreshRequest(
    val refresh: String
)

data class TokenRefreshResponse(
    val access: String
)

data class OtpGenerateRequest(
    val phone_number: String
)

data class OtpVerifyRequest(
    val phone_number: String,
    val otp: String
)

data class SimpleResponse(
    val message: String,
    val otp: String? = null
)

data class TokenVerificationResponse(
    val valid: Boolean,
    val goals_completed: Boolean,
    val user: UserData
)

data class AuthResponse(
    val access: String? = null,
    val refresh: String? = null,
    val goals_completed: Boolean = false,
    val user: UserData? = null
)

data class UserData(
    val id: String,
    val email: String,
    val username: String,
    val full_name: String? = null,
    val phone_number: String? = null,
    val goals_completed: Boolean = false,
    val profile: ProfileDetailsResponse? = null
)

data class ProfileResponse(
    val full_name: String? = null,
    val username: String? = null,
    val email: String? = null,
    val phone_number: String? = null,
    val bio: String? = null,
    val fitness_level: String? = null,
    val weekly_goal: String? = null,
    val meal_planning_freq: String? = null,
    val weekly_meal_plans: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val country: String? = null,
    val height_ft: Int? = null,
    val height_in: Int? = null,
    val height_cm: Float? = null,
    val current_weight: Float? = null,
    val goal_weight: Float? = null,
    val activity_level: String? = null,
    val goals_completed: Boolean = false,
    val goals: List<String>? = null,
    val barriers: List<String>? = null,
    val habits: List<String>? = null,
    val streak: Int? = null,
    val longest_streak: Int? = null,
    val level: Int? = null,
    val xp: Int? = null,
    val workouts_completed: Int? = null,
    val achievements_count: Int? = null
)

data class UserProfileResponse(
    val id: Int = 0,
    val email: String? = null,
    val username: String? = null,
    val full_name: String? = null,
    val phone_number: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val height_feet: Int? = null,
    val height_inches: Int? = null,
    val height_cm: Float? = null,
    val current_weight: Float? = null,
    val goal_weight: Float? = null,
    val activity_level: String? = null,
    val goals_completed: Boolean = false,
    val profile: ProfileDetailsResponse? = null,
    val profile_image: String? = null
)

data class ProfileDetailsResponse(
    val bio: String? = null,
    val profile_photo: String? = null,
    val fitness_level: String? = null,
    val streak: Int = 0,
    val longest_streak: Int = 0,
    val level: Int = 1,
    val xp: Int = 0,
    val workouts_completed: Int = 0,
    val achievements_count: Int = 0,
    val full_name: String? = null,
    val weekly_goal: String? = null,
    val meal_planning_freq: String? = null,
    val weekly_meal_plans: String? = null
)

data class DailyStatsResponse(
    val id: Int,
    val date: String,
    val steps: Int,
    val calories_consumed: Int,
    val calories_burned: Int,
    val water_ml: Int,
    val weight_kg: Float?,
    val workouts_completed: Int = 0
)

data class GoalSettingsResponse(
    val id: Int? = null,
    val primary_goal: String = "Maintain",
    val daily_calorie_target: Int,
    val daily_step_goal: Int,
    val protein_g: Int,
    val carbs_g: Int,
    val fats_g: Int,
    val current_weight: Double,
    val target_weight: Double,
    val weekly_goal_weight: Double = 0.5,
    val current_body_fat: Double = 18.0,
    val target_body_fat: Double = 15.0,
    val muscle_mass_goal: Double = 35.0,
    val workouts_per_week: Int = 4,
    val weekly_calorie_burn_goal: Int = 2000,
    val is_adaptive_mode_enabled: Boolean = false,
    val quiet_hours_from: String = "22:00",
    val quiet_hours_to: String = "07:00"
)

data class AiSuggestionResponse(
    val id: String,
    val message: String,
    val type: String
)

data class AiMetricsResponse(
    val recovery_score: Int,
    val energy_balance_score: Int,
    val injury_risk_score: Int,
    val current_suggestion: AiSuggestionResponse?
)

data class MealMacroResponse(
    val protein: Float,
    val carbs: Float,
    val fats: Float
)
data class MealBreakdownResponse(
    val breakfast: MealMacroResponse,
    val lunch: MealMacroResponse,
    val dinner: MealMacroResponse,
    val snacks: MealMacroResponse
)
data class WeeklyMacroResponse(
    val date: String,
    val day_name: String,
    val protein: Float,
    val carbs: Float,
    val fats: Float,
    val meals: MealBreakdownResponse
)
data class DashboardDataResponse(
    val daily_stats: DailyStatsResponse,
    val goal_settings: GoalSettingsResponse,
    val weekly_history: List<DailyStatsResponse>,
    val weekly_macros: List<WeeklyMacroResponse>? = null,
    val ai_metrics: AiMetricsResponse,
    val weight_goal: WeightGoalDto? = null
)

data class FriendsListResponse(
    val friends: List<FriendResponse>
)

data class FriendResponse(
    val id: String,
    val username: String,
    val profile: ProfileDetailsResponse?
)

data class GroupResponse(
    val id: String,
    val name: String,
    val description: String?,
    val goal: String?,
    val is_public: Boolean,
    val member_count: Int,
    val unread_count: Int = 0,
    val active_challenge: String?
)

data class CreateGroupRequest(
    val name: String,
    val description: String,
    val goal: String,
    val is_public: Boolean,
    val invited_users: List<String>
)

data class BattleResponse(
    val id: String,
    val user1_username: String,
    val user2_username: String,
    val score1: Int,
    val score2: Int,
    val status: String,
    val end_time: String
)

data class FriendRequestsResponse(
    val received_requests: List<FriendRequestItem>,
    val sent_requests: List<FriendRequestItem>
)

data class FriendRequestItem(
    val id: String,
    val sender: UserData,
    val receiver: UserData,
    val status: String,
    val created_at: String
)

data class GroupDetailResponse(
    val id: String,
    val name: String,
    val description: String?,
    val goal: String?,
    val is_public: Boolean,
    val member_count: Int,
    val active_challenge: String?,
    val challenge_end_days: Int,
    val progress: Float,
    val total_workouts: Int,
    val total_calories: String,
    val avg_streak: Int,
    val members: List<GroupMemberItem>
)

data class GroupMemberItem(
    val id: String,
    val name: String,
    val username: String,
    val initials: String,
    val points: Int,
    val rank: Int,
    val is_you: Boolean
)

data class ChallengeResponse(
    val id: String,
    val name: String,
    val description: String?,
    val type: String,
    val duration_days: Int,
    val target_value: String?,
    val points_reward: Int,
    val created_at: String
)

data class ChallengeParticipantResponse(
    val id: String,
    val name: String,
    val username: String,
    val initials: String,
    val progress_value: String,
    val is_completed: Boolean,
    val joined_at: String
)

data class GroupMessageResponse(
    val id: String,
    val sender_id: String,
    val sender_name: String,
    val message: String,
    val created_at: String
)

data class SendGroupMessageRequest(
    val message: String
)

data class InviteToGroupRequest(
    val group_id: String,
    val user_id: String
)

data class RejectGroupInviteRequest(
    val group_id: String
)

data class AiChatMessageData(
    val role: String,
    val content: String
)

data class AiChatRequest(
    val message: String,
    val image_base64: String? = null,
    val history: List<AiChatMessageData> = emptyList()
)

data class AiChatResponse(
    val reply: String
)

data class TrainedAiRequest(
    val question: String
)

data class TrainedAiResponse(
    val answer: String,
    val category: String
)

data class PrivacySettingsResponse(
    val private_account: Boolean = false,
    val show_profile_in_search: Boolean = true,
    val show_activity_status: Boolean = true,
    val share_workout_data: Boolean = true,
    val share_diet_data: Boolean = true,
    val share_progress_photos: Boolean = false,
    val appear_on_leaderboards: Boolean = true
)

data class ChangePasswordRequest(
    val old_password: String,
    val new_password: String
)


data class BlockedUserResponse(
    val id: Int,
    val blocked: Int,
    val blocked_username: String?,
    val blocked_email: String?,
    val blocked_full_name: String?
)

data class UserSessionResponse(
    val id: Int,
    val device_name: String,
    val ip_address: String?,
    val created_at: String,
    val last_active: String,
    val is_active: Boolean
)


data class DiaryDailyResponse(
    val date: String,
    val summary: DiarySummaryDto,
    val meals: Map<String, List<FoodEntryDto>>,
    val water_intake: Int
)

data class DiarySummaryDto(
    val goal: Int,
    val food: Int,
    val exercise: Int,
    val remaining: Int
)

data class FoodEntryDto(
    val id: Int? = null,
    val meal_type: String,
    val food_name: String,
    val quantity: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val created_at: String? = null
)

data class AddFoodEntryRequest(
    val date: String,
    val meal_type: String,
    val food_name: String,
    val quantity: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float
)

data class SmartSwapsResponse(
    val current_food: String,
    val better_option: String,
    val calorie_difference: String,
    val benefits: String
)

data class FoodDto(
    val id: Int? = null,
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val category: String? = null,
    val serving_size: String = "1 serving",
    val servings_per_container: String = "1",
    val saturated_fat: Float = 0f,
    val trans_fat: Float = 0f,
    val polyunsaturated_fat: Float = 0f,
    val monounsaturated_fat: Float = 0f,
    val cholesterol: Float = 0f,
    val sodium: Float = 0f,
    val dietary_fiber: Float = 0f,
    val total_sugars: Float = 0f,
    val added_sugars: Float = 0f,
    val vitamin_d: Float = 0f,
    val calcium: Float = 0f,
    val iron: Float = 0f,
    val potassium: Float = 0f,
    val not_found: Boolean? = false
)

data class FoodScanResponse(
    val id: Int,
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val category: String,
    val meal_type: String,
    val serving_size: String = "1 serving",
    val servings_per_container: String = "1",
    val saturated_fat: Float = 0f,
    val trans_fat: Float = 0f,
    val polyunsaturated_fat: Float = 0f,
    val monounsaturated_fat: Float = 0f,
    val cholesterol: Float = 0f,
    val sodium: Float = 0f,
    val dietary_fiber: Float = 0f,
    val total_sugars: Float = 0f,
    val added_sugars: Float = 0f,
    val vitamin_d: Float = 0f,
    val calcium: Float = 0f,
    val iron: Float = 0f,
    val potassium: Float = 0f
)

data class ExerciseLogRequest(
    val name: String,
    val sets_reps: String,
    val weight: String,
    val is_completed: Boolean
)

data class WorkoutLogRequest(
    val workout_type: String,
    val calories_burned: Int,
    val duration_minutes: Int,
    val date: String, // YYYY-MM-DD
    val exercises: List<ExerciseLogRequest>
)

data class ExerciseLogEntryResponse(
    val id: Int,
    val name: String,
    val sets_reps: String,
    val weight: String,
    val is_completed: Boolean
)

data class WorkoutLogResponse(
    val id: Int,
    val date: String,
    val workout_type: String,
    val calories_burned: Int,
    val duration_minutes: Int,
    val exercises: List<ExerciseLogEntryResponse>,
    val created_at: String
)

data class WorkoutTemplateExerciseResponse(
    val id: Int,
    val name: String,
    val sets_reps: String,
    val weight: String,
    val order: Int
)

data class WorkoutTemplateResponse(
    val id: Int? = null,
    val name: String? = null,
    val description: String? = null,
    val exercises: List<WorkoutTemplateExerciseResponse>? = null,
    val is_rest_day: Boolean = false,
    val message: String? = null
)

data class WeeklyScheduleResponse(
    val id: Int,
    val day_of_week: Int,
    val day_name: String,
    val template: WorkoutTemplateResponse?,
    val is_rest_day: Boolean
)

data class WorkoutHistorySummary(
    val monthWorkouts: Int,
    val monthMinutes: Int,
    val monthCalories: Int
)

data class WorkoutHistoryResponse(
    val summary: WorkoutHistorySummary,
    val recentWorkouts: List<WorkoutLogResponse>
)

// Weight Log DTOs
data class WeightLogDto(
    val id: Int,
    val date: String,
    val weight: Float,
    val created_at: String
)

data class WeightGoalDto(
    val id: Int? = null,
    val start_weight: Float?,
    val target_weight: Float?,
    val weekly_goal_weight: Float = 0.5f,
    val weeks_remaining: Float = 0f
)

data class WeightLogRequest(
    val date: String,
    val weight: Float
)

data class WeightGoalRequest(
    val start_weight: Float,
    val target_weight: Float,
    val weekly_goal_weight: Float = 0.5f
)


data class DetailedStatsResponse(
    val currentStreak: Int,
    val longestStreak: Int,
    val weightLost: Float,
    val totalDaysTracked: Int,
    val totalWorkouts: Int,
    val totalCaloriesBurned: Int,
    val avgDailyCalories: Int,
    val monthlyBreakdown: List<MonthlyBreakdown>,
    val personalRecords: List<PersonalRecord>
)

data class MonthlyBreakdown(
    val month: String,
    val workouts: Int,
    val calories: Int
)

data class PersonalRecord(
    val title: String,
    val value: String,
    val date: String,
    val type: String
)

data class AchievementResponse(
    val id: Int,
    val title: String,
    val description: String,
    val iconName: String,
    val colorHex: String,
    val isUnlocked: Boolean,
    val unlockedAt: String?
)

data class FaqResponse(
    val id: Int,
    val question: String,
    val answer: String
)

data class TicketRequest(
    val subject: String,
    val message: String,
    val category: String = "general"
)

data class TicketResponse(
    val id: Int,
    val subject: String,
    val message: String,
    val status: String,
    val created_at: String
)

data class NotificationResponse(
    val id: Int,
    val title: String,
    val message: String,
    val type: String,
    val is_read: Boolean,
    val created_at: String
)

data class CompareStatsResponse(
    val me: UserCompareData,
    val friend: UserCompareData
)

data class UserCompareData(
    val steps: Int,
    val workouts: Int,
    val streak: Int,
    val xp: Int,
    val level: Int,
    val calories_burned: Int
)
