package com.simats.myfitnessbuddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.myfitnessbuddy.data.local.SettingsManager
import com.simats.myfitnessbuddy.data.remote.AddFoodEntryRequest
import com.simats.myfitnessbuddy.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import okhttp3.MediaType.Companion.toMediaTypeOrNull

data class FoodEntry(
    val name: String,
    val calories: Int,
    val macros: String,
    val quantity: String = "1 serving",
    val id: Int? = null,
    val servingSize: String = "1 serving",
    val servingsPerContainer: String = "1",
    val saturatedFat: Float = 0f,
    val transFat: Float = 0f,
    val polyunsaturatedFat: Float = 0f,
    val monounsaturatedFat: Float = 0f,
    val cholesterol: Float = 0f,
    val sodium: Float = 0f,
    val dietaryFiber: Float = 0f,
    val totalSugars: Float = 0f,
    val addedSugars: Float = 0f,
    val vitaminD: Float = 0f,
    val calcium: Float = 0f,
    val iron: Float = 0f,
    val potassium: Float = 0f,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val notFound: Boolean = false,
    val aiSuggestion: String? = null
)

data class MealLog(
    val name: String,
    val calories: Int = 0,
    val foods: List<FoodEntry> = emptyList()
)

data class FoodSwapSuggestion(
    val originalFood: String,
    val suggestedFood: String,
    val caloriesSaved: Int,
    val reason: String
)

data class DiaryState(
    val selectedDate: LocalDate = LocalDate.now(),
    val meals: List<MealLog> = listOf(
        MealLog("Breakfast"),
        MealLog("Lunch"),
        MealLog("Dinner"),
        MealLog("Snacks")
    ),
    val totalCalories: Int = 0,
    val exerciseCalories: Int = 0,
    val calorieGoal: Int = 2200,
    val swapSuggestions: List<FoodSwapSuggestion> = emptyList(),
    val searchResult: List<FoodEntry> = emptyList(),
    val scannedBarcodeResult: FoodEntry? = null,
    val waterIntake: Int = 0, // in glasses
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isAiSearching: Boolean = false,
    val error: String? = null
) {
    val dateDisplay: String
        get() = when (selectedDate) {
            LocalDate.now() -> "Today"
            LocalDate.now().minusDays(1) -> "Yesterday"
            LocalDate.now().plusDays(1) -> "Tomorrow"
            else -> selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM dd"))
        }

    val caloriesLeft: Int
        get() = (calorieGoal - totalCalories + exerciseCalories).coerceAtLeast(0)
}

class DiaryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        DiaryState(calorieGoal = SettingsManager.calorieGoal.toIntOrNull() ?: 2200)
    )
    val uiState: StateFlow<DiaryState> = _uiState.asStateFlow()

    init {
        fetchDiaryData()
        fetchSmartSwaps()
    }



    private fun fetchDiaryData(date: LocalDate = _uiState.value.selectedDate) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            try {
                val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val response = RetrofitClient.apiService.getDiaryDaily(dateStr)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val newMeals = listOf("Breakfast", "Lunch", "Dinner", "Snacks").map { mealName ->
                            val foodsDto = body.meals[mealName.lowercase()] ?: emptyList()
                            val foods = foodsDto.map {
                                FoodEntry(
                                    name = it.food_name,
                                    calories = it.calories,
                                    macros = "C: ${it.carbs}g • P: ${it.protein}g • F: ${it.fat}g",
                                    quantity = it.quantity,
                                    id = it.id
                                )
                            }
                            MealLog(
                                name = mealName,
                                calories = foods.sumOf { it.calories },
                                foods = foods
                            )
                        }

                        _uiState.update { it.copy(
                            meals = newMeals,
                            totalCalories = body.summary.food,
                            exerciseCalories = body.summary.exercise,
                            calorieGoal = body.summary.goal,
                            waterIntake = body.water_intake,
                            isLoading = false
                        )}

                        // Fetch suggestions for high calorie items (> 200 cals)
                        newMeals.flatMap { it.foods }.filter { it.calories > 200 && it.aiSuggestion == null }.forEach { food ->
                            fetchSuggestionForFood(food)
                        }
                    }
                } else {
                    _uiState.update { it.copy(error = "Failed to fetch data", isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }
    
    private fun fetchSuggestionForFood(food: FoodEntry) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getSmartSwaps(food.name)
                if (response.isSuccessful && response.body() != null) {
                    val swap = response.body()!!
                    val suggestion = "Try ${swap.better_option} (${swap.calorie_difference})"
                    
                    _uiState.update { state ->
                        val updatedMeals = state.meals.map { meal ->
                            meal.copy(foods = meal.foods.map { f ->
                                if (f.name == food.name && f.id == food.id) f.copy(aiSuggestion = suggestion) else f
                            })
                        }
                        state.copy(meals = updatedMeals)
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun fetchSmartSwaps() {
        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            try {
                val response = RetrofitClient.apiService.getSmartSwaps()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val calDiffStr = body.calorie_difference
                    val isGain = calDiffStr.contains("+")
                    val rawCals = calDiffStr.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                    val finalDiff = if (isGain) rawCals else -rawCals

                    _uiState.update { it.copy(
                        swapSuggestions = listOf(
                            FoodSwapSuggestion(
                                originalFood = body.current_food,
                                suggestedFood = body.better_option,
                                caloriesSaved = finalDiff, // Negative for savings, Positive for gains
                                reason = body.benefits
                            )
                        )
                    )}
                }
            } catch (e: Exception) {
                // Ignore smart swap fetch failures
            }
        }
    }

    fun addFoodToMeal(
        mealName: String, 
        food: FoodEntry, 
        quantity: Int = 1,
        protein: Float = 0f, 
        carbs: Float = 0f, 
        fat: Float = 0f
    ) {
        val dateStr = _uiState.value.selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                var p = protein; var c = carbs; var f = fat
                if (p == 0f && c == 0f && f == 0f) {
                    val match = Regex("C: (.*?)g • P: (.*?)g • F: (.*?)g").find(food.macros)
                    if (match != null) {
                        c = match.groupValues[1].toFloatOrNull() ?: 0f
                        p = match.groupValues[2].toFloatOrNull() ?: 0f
                        f = match.groupValues[3].toFloatOrNull() ?: 0f
                    }
                }

                val request = AddFoodEntryRequest(
                    date = dateStr,
                    meal_type = mealName.lowercase(),
                    food_name = food.name,
                    quantity = if (quantity == 1) "1 serving" else "$quantity servings",
                    calories = food.calories * quantity,
                    protein = p * quantity,
                    carbs = c * quantity,
                    fat = f * quantity
                )
                
                val token = SettingsManager.authToken ?: ""
                val response = RetrofitClient.apiService.addFoodEntry(request)
                if (response.isSuccessful) {
                    fetchDiaryData(_uiState.value.selectedDate)
                } else {
                    _uiState.update { it.copy(error = "Failed to add food: ${response.code()}", isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun deleteFoodEntry(id: Int) {
        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            try {
                val response = RetrofitClient.apiService.deleteFoodEntry(id)
                if (response.isSuccessful) {
                    fetchDiaryData(_uiState.value.selectedDate)
                }
            } catch (e: Exception) {
                // Ignore deletion errors for now
            }
        }
    }

    fun updateWaterIntake(glasses: Int) {
        val dateStr = _uiState.value.selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        // Optimistic update
        val previousWater = _uiState.value.waterIntake
        _uiState.update { it.copy(waterIntake = glasses.coerceIn(0, 12)) } // Max 3L = 12 glasses
        
        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            try {
                val response = RetrofitClient.apiService.updateWaterIntake(com.simats.myfitnessbuddy.data.remote.WaterUpdateRequest(dateStr, glasses.coerceIn(0, 12))
                )
                if (!response.isSuccessful) {
                    // Rollback on failure
                    _uiState.update { it.copy(waterIntake = previousWater, error = "Failed to update water intake") }
                }
            } catch (e: Exception) {
                // Rollback on connection error
                _uiState.update { it.copy(waterIntake = previousWater, error = e.localizedMessage) }
            }
        }
    }

    fun saveChanges() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            kotlinx.coroutines.delay(800) // Artificial delay for UX feedback
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun onPreviousDay() {
        _uiState.update { it.copy(selectedDate = it.selectedDate.minusDays(1)) }
        fetchDiaryData(_uiState.value.selectedDate)
    }

    fun onNextDay() {
        _uiState.update { it.copy(selectedDate = it.selectedDate.plusDays(1)) }
        fetchDiaryData(_uiState.value.selectedDate)
    }



    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        fetchDiaryData(date)
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    fun searchFoods(query: String, mealType: String? = null) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            // Debounce for 300ms if searching
            if (query.isNotEmpty()) {
                kotlinx.coroutines.delay(300)
            }

            try {
                val token = SettingsManager.authToken ?: ""
                val response = RetrofitClient.apiService.searchFoods(query, mealType)
                if (response.isSuccessful && response.body() != null) {
                    val localFoods = response.body()!!.map { it.toFoodEntry() }
                    _uiState.update { it.copy(searchResult = localFoods) }
                    
                    // Automatically trigger AI search if local results are few (e.g., < 3) and query is specific
                    if (localFoods.size < 3 && query.length >= 3) {
                        searchFoodAi(query, append = true)
                    }
                } else if (query.length >= 3) {
                    searchFoodAi(query)
                }
            } catch (e: Exception) {
                if (query.length >= 3) searchFoodAi(query)
            }
        }
    }

    fun searchFoodAi(query: String, append: Boolean = false, setAsScanned: Boolean = false) {
        if (query.length < 3) return
        
        _uiState.update { it.copy(isAiSearching = true, error = null) }

        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            try {
                val response = RetrofitClient.apiService.searchFoodAi(query)
                if (response.isSuccessful && response.body() != null) {
                    val aiFoods = response.body()!!.map { it.toFoodEntry() }
                    _uiState.update { state -> 
                        val newResults = if (append) {
                            (state.searchResult + aiFoods).distinctBy { it.name }
                        } else {
                            aiFoods
                        }
                        state.copy(
                            searchResult = newResults, 
                            scannedBarcodeResult = if (setAsScanned && aiFoods.isNotEmpty()) aiFoods.first() else state.scannedBarcodeResult,
                            isAiSearching = false
                        ) 
                    }
                } else {
                    _uiState.update { it.copy(isAiSearching = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isAiSearching = false) }
            }
        }
    }

    private fun com.simats.myfitnessbuddy.data.remote.FoodDto.toFoodEntry(): FoodEntry {
        return FoodEntry(
            name = this.name,
            calories = this.calories,
            macros = "C: ${this.carbs}g • P: ${this.protein}g • F: ${this.fat}g",
            quantity = "1 serving",
            id = this.id,
            servingSize = this.serving_size,
            servingsPerContainer = this.servings_per_container,
            saturatedFat = this.saturated_fat,
            transFat = this.trans_fat,
            polyunsaturatedFat = this.polyunsaturated_fat,
            monounsaturatedFat = this.monounsaturated_fat,
            cholesterol = this.cholesterol,
            sodium = this.sodium,
            dietaryFiber = this.dietary_fiber,
            totalSugars = this.total_sugars,
            addedSugars = this.added_sugars,
            vitaminD = this.vitamin_d,
            calcium = this.calcium,
            iron = this.iron,
            potassium = this.potassium,
            protein = this.protein,
            carbs = this.carbs,
            fat = this.fat,
            notFound = this.not_found ?: false
        )
    }

    fun applyFoodSwap(suggestion: FoodSwapSuggestion) {
        val newFood = FoodEntry(
            name = suggestion.suggestedFood,
            calories = 100, // Mock base cals
            macros = "C: 15.0g • P: 5.0g • F: 2.0g"
        )
        addFoodToMeal("Breakfast", newFood, protein = 5f, carbs = 15f, fat = 2f)
        
        _uiState.update { state ->
            state.copy(swapSuggestions = state.swapSuggestions.filter { it != suggestion })
        }
    }

    fun scanFoodImage(
        imageBytes: ByteArray,
        onSuccess: (com.simats.myfitnessbuddy.data.remote.FoodScanResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val mediaType = "image/jpeg".toMediaTypeOrNull()
                val requestFile = okhttp3.RequestBody.create(
                    mediaType,
                    imageBytes
                )
                val body = okhttp3.MultipartBody.Part.createFormData(
                    "image",
                    "scan_${System.currentTimeMillis()}.jpg",
                    requestFile
                )

                val token = SettingsManager.authToken ?: ""
                val response = RetrofitClient.apiService.scanFood(body)
                if (response.isSuccessful && response.body() != null) {
                    onSuccess(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val json = org.json.JSONObject(errorBody ?: "{}")
                        json.optString("error", "Unknown error")
                    } catch (e: Exception) {
                        "AI Scan failed with code ${response.code()}"
                    }
                    onError(errorMessage)
                }
            } catch (e: Exception) {
                onError("Scan error: ${e.localizedMessage}")
            }
        }
    }

    fun com.simats.myfitnessbuddy.data.remote.FoodScanResponse.toFoodEntry(): FoodEntry {
        return FoodEntry(
            name = this.name,
            calories = this.calories,
            macros = "C: ${this.carbs}g • P: ${this.protein}g • F: ${this.fat}g",
            quantity = "1 serving",
            id = this.id,
            servingSize = this.serving_size,
            servingsPerContainer = this.servings_per_container,
            saturatedFat = this.saturated_fat,
            transFat = this.trans_fat,
            polyunsaturatedFat = this.polyunsaturated_fat,
            monounsaturatedFat = this.monounsaturated_fat,
            cholesterol = this.cholesterol,
            sodium = this.sodium,
            dietaryFiber = this.dietary_fiber,
            totalSugars = this.total_sugars,
            addedSugars = this.added_sugars,
            vitaminD = this.vitamin_d,
            calcium = this.calcium,
            iron = this.iron,
            potassium = this.potassium,
            protein = this.protein,
            carbs = this.carbs,
            fat = this.fat,
            notFound = false
        )
    }

    fun lookupBarcode(barcode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            val token = SettingsManager.authToken ?: ""
            try {
                val response = RetrofitClient.apiService.lookupBarcode(barcode)
                if (response.isSuccessful && response.body() != null) {
                    val foodDto = response.body()!!
                    val foodEntry = FoodEntry(
                        name = foodDto.name,
                        calories = foodDto.calories,
                        macros = "C: ${foodDto.carbs}g • P: ${foodDto.protein}g • F: ${foodDto.fat}g",
                        id = foodDto.id,
                        servingSize = foodDto.serving_size,
                        servingsPerContainer = foodDto.servings_per_container,
                        saturatedFat = foodDto.saturated_fat,
                        transFat = foodDto.trans_fat,
                        polyunsaturatedFat = foodDto.polyunsaturated_fat,
                        monounsaturatedFat = foodDto.monounsaturated_fat,
                        cholesterol = foodDto.cholesterol,
                        sodium = foodDto.sodium,
                        dietaryFiber = foodDto.dietary_fiber,
                        totalSugars = foodDto.total_sugars,
                        addedSugars = foodDto.added_sugars,
                        vitaminD = foodDto.vitamin_d,
                        calcium = foodDto.calcium,
                        iron = foodDto.iron,
                        potassium = foodDto.potassium,
                        protein = foodDto.protein,
                        carbs = foodDto.carbs,
                        fat = foodDto.fat,
                        notFound = foodDto.not_found ?: false
                    )
                    _uiState.update { it.copy(scannedBarcodeResult = foodEntry, isLoading = false) }
                    onSuccess()
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Product not found") }
                    onError("Product not found")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
                onError(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun clearScannedBarcode() {
        _uiState.update { it.copy(scannedBarcodeResult = null) }
    }
}
