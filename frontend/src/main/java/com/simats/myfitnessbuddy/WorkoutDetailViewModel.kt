package com.simats.myfitnessbuddy

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Exercise(
    val name: String,
    val sets: String,
    val rest: String,
    val isCompleted: Boolean = false,
    val calories: Int = 50
)

data class WorkoutDetail(
    val title: String,
    val level: String = "Beginner / Intermediate",
    val duration: String = "60 min",
    val exercises: List<Exercise>,
    val tips: List<String>
)

data class WorkoutDetailUiState(
    val workout: WorkoutDetail? = null,
    val caloriesBurned: Int = 0,
    val exercisesCompleted: Int = 0
)

class WorkoutDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutDetailUiState())
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()

    fun loadWorkout(type: String?) {
        val workout = when (type?.lowercase()) {
            "chest" -> WorkoutDetail(
                "Chest & Triceps Workout",
                duration = "75 min",
                exercises = listOf(
                    Exercise("Bench Press", "4 sets × 8–10 reps", "90s"),
                    Exercise("Incline Dumbbell Press", "3 sets × 10–12 reps", "60s"),
                    Exercise("Cable Flyes", "3 sets × 12–15 reps", "45s"),
                    Exercise("Tricep Pushdowns", "3 sets × 12–15 reps", "45s"),
                    Exercise("Overhead Tricep Extension", "3 sets × 12 reps", "60s"),
                    Exercise("Dips", "3 sets to failure", "60s"),
                    Exercise("Push-ups", "3 sets to failure", "45s")
                ),
                tips = listOf(
                    "Control the weight on the way down (eccentric phase)",
                    "Maintain a slight arch in your back during bench press",
                    "Keep your shoulder blades retracted",
                    "Squeeze your triceps at the bottom of pushdowns",
                    "Hydrate well before heavy compound lifts"
                )
            )
            "back" -> WorkoutDetail(
                "Back & Biceps Workout",
                duration = "70 min",
                exercises = listOf(
                    Exercise("Deadlifts", "4 sets × 5 reps", "120s"),
                    Exercise("Pull-ups", "3 sets to failure", "90s"),
                    Exercise("Bent Over Rows", "3 sets × 8–10 reps", "60s"),
                    Exercise("Lat Pulldowns", "3 sets × 10–12 reps", "60s"),
                    Exercise("Seated Cable Rows", "3 sets × 12 reps", "60s"),
                    Exercise("Barbell Bicep Curls", "3 sets × 10–12 reps", "60s"),
                    Exercise("Hammer Curls", "3 sets × 12 reps", "60s")
                ),
                tips = listOf(
                    "Keep your spine neutral during deadlifts",
                    "Pull with your elbows, not just your hands",
                    "Focus on the squeeze in your lats",
                    "Don't swing your body during bicep curls",
                    "Retract your shoulder blades for rowing movements"
                )
            )
            "legs" -> WorkoutDetail(
                "Legs Workout", duration = "70 min",
                exercises = listOf(
                    Exercise("Squats", "4 sets × 8 reps", "120s"),
                    Exercise("Romanian Deadlifts", "3 sets × 10 reps", "90s"),
                    Exercise("Leg Press", "3 sets × 12 reps", "60s"),
                    Exercise("Leg Extensions", "3 sets × 15 reps", "45s"),
                    Exercise("Walking Lunges", "3 sets × 20 steps", "60s"),
                    Exercise("Calf Raises", "4 sets × 15–20 reps", "45s"),
                    Exercise("Bulgarian Split Squats", "3 sets × 8 reps/leg", "60s"),
                    Exercise("Glute Bridges", "3 sets × 15 reps", "60s")
                ),
                tips = listOf(
                    "Keep heels planted during squats",
                    "Full range of motion for calf raises",
                    "Drive through your heels for glute bridges",
                    "Leg day requires the most recovery time"
                )
            )
            "shoulders" -> WorkoutDetail(
                "Shoulders Workout", duration = "40 min",
                exercises = listOf(
                    Exercise("Overhead Press", "4 sets × 8 reps", "90s"),
                    Exercise("Lateral Raises", "3 sets × 12–15 reps", "45s"),
                    Exercise("Front Raises", "3 sets × 12 reps", "45s"),
                    Exercise("Rear Delt Flyes", "3 sets × 15 reps", "45s")
                ),
                tips = listOf(
                    "Keep your core tight during overhead press",
                    "Controlled movements for lateral raises",
                    "Don't shrug your shoulders up during raises",
                    "Maintain a steady tempo"
                )
            )
            "abs" -> WorkoutDetail(
                "Abs Workout", duration = "20 min",
                exercises = listOf(
                    Exercise("Plank", "3 sets × 60s", "45s"),
                    Exercise("Russian Twists", "3 sets × 30 reps", "45s"),
                    Exercise("Leg Raises", "3 sets × 15 reps", "45s"),
                    Exercise("Crunches", "3 sets × 20 reps", "45s")
                ),
                tips = listOf(
                    "Maintain a flat back during planks",
                    "Exhale at the peak of the contraction",
                    "Quality over quantity for core training"
                )
            )
            "fullbody" -> WorkoutDetail(
                "Regular Full Body Exercises", duration = "65 min",
                exercises = listOf(
                    Exercise("Jumping Jacks", "3 sets × 60s", "30s"),
                    Exercise("Bodyweight Squats", "3 sets × 20 reps", "45s"),
                    Exercise("Push-ups", "3 sets to failure", "45s"),
                    Exercise("Plank", "3 sets × 60s", "45s"),
                    Exercise("Burpees", "3 sets × 10 reps", "60s"),
                    Exercise("High Knees", "3 sets × 45s", "30s"),
                    Exercise("Mountain Climbers", "3 sets × 45s", "30s"),
                    Exercise("Glute Bridges", "3 sets × 15 reps", "45s"),
                    Exercise("Superman Hold", "3 sets × 30s", "30s")
                ),
                tips = listOf(
                    "Focus on constant movement",
                    "Quality over quantity for full body",
                    "Great for fat burning and conditioning",
                    "Keep transitions between exercises short"
                )
            )
            else -> null
        }
        _uiState.update { it.copy(workout = workout, caloriesBurned = 0, exercisesCompleted = 0) }
    }

    fun completeExercise(exerciseName: String) {
        _uiState.update { state ->
            val workout = state.workout ?: return@update state
            val newExercises = workout.exercises.map {
                if (it.name == exerciseName && !it.isCompleted) it.copy(isCompleted = true) else it
            }
            val newlyCompleted = newExercises.count { it.isCompleted }
            
            // Calculate accurate calories using Generic MET (5f) * Weight * Duration
            val durationMins = workout.duration.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 60
            val metValue = 5f
            val userWeightKg = com.simats.myfitnessbuddy.data.local.SettingsManager.currentWeight.toFloatOrNull() ?: 0f
            val totalPossibleCalories = metValue * userWeightKg * (durationMins / 60f)
            
            val totalCount = newExercises.size
            val fractionCompleted = if (totalCount > 0) newlyCompleted.toFloat() / totalCount else 0f
            val activeCaloriesBurned = (totalPossibleCalories * fractionCompleted).toInt()
            
            state.copy(
                workout = workout.copy(exercises = newExercises),
                exercisesCompleted = newlyCompleted,
                caloriesBurned = activeCaloriesBurned
            )
        }
    }
}
