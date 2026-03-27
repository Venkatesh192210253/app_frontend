package com.simats.myfitnessbuddy

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale
import kotlin.math.abs

// Reusing colors from AuthScreens or defining new ones per prompt
val OnboardingPrimaryGreen = Color(0xFF22C55E)
val OnboardingLightGreenTint = Color(0xFFDCFCE7)
val OnboardingLightGrey = Color(0xFFF3F4F6)
val OnboardingDarkGrey = Color(0xFF9CA3AF)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SetupFlow(onFinish: () -> Unit) {
    val viewModel: OnboardingViewModel = viewModel()
    val uiState by viewModel.uiState
    val isNextEnabled by viewModel.isNextEnabled

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (uiState.currentStep) {
                                in 0..2 -> "Goals"
                                in 3..5 -> "Plan"
                                else -> "You"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        if (uiState.currentStep > 0) {
                            IconButton(onClick = { viewModel.prevStep() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )
                // Custom Progress Indicator
                OnboardingProgressIndicator(currentStep = uiState.currentStep)
            }
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(24.dp)
                ) {
                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Button(
                        onClick = { 
                            viewModel.nextStep(onFinish = onFinish)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = isNextEnabled && !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OnboardingPrimaryGreen,
                            disabledContainerColor = OnboardingLightGrey
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            val isLastStep = uiState.currentStep == 8
                            Text(
                                text = if (isLastStep) "Finish" else "Next",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isNextEnabled) Color.White else OnboardingDarkGrey
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "StepTransition"
            ) { step ->
                OnboardingStepContent(step, viewModel)
            }
        }
    }
}

@Composable
fun OnboardingProgressIndicator(currentStep: Int) {
    val activeColor = OnboardingPrimaryGreen
    val inactiveColor = OnboardingLightGrey
    
    // Grouping steps into 4 segments as shown in existing SetupActivity
    // Segment 1: Steps 0-2 (Goals)
    // Segment 2: Steps 3-5 (Plan)
    // Segment 3: Steps 6-8 (You)
    // Actually existing has 4 bars. Let's make it 3 major phases.
    
    val phase = when (currentStep) {
        in 0..2 -> 1
        in 3..5 -> 2
        else -> 3
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 1..3) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (phase >= i) activeColor else inactiveColor)
            )
        }
    }
}

@Composable
fun OnboardingStepContent(step: Int, viewModel: OnboardingViewModel) {
    val uiState by viewModel.uiState
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val title = getStepTitle(step)
        val subtitle = getStepSubtitle(step)
        
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        when (step) {
            0 -> SelectionStep(
                options = listOf("Lose weight", "Maintain weight", "Gain weight", "Gain muscle", "Modify my diet", "Plan meals", "Manage stress"),
                selected = uiState.selectedGoals,
                onSelected = { viewModel.updateGoals(it) },
                isMultiSelect = true
            )
            1 -> SelectionStep(
                options = listOf("Lack of time", "The regimen was hard to follow", "Healthy diets lack variety", "Stress around food choices", "Holidays/Social Events", "Food cravings", "Lack of progress"),
                selected = uiState.barriers,
                onSelected = { viewModel.updateBarriers(it) },
                isMultiSelect = true
            )
            2 -> SelectionStep(
                options = listOf("Eat more protein", "Plan more meals", "Meal prep and cook", "Eat more fiber", "Move more", "Workout more"),
                selected = uiState.habits,
                onSelected = { viewModel.updateHabits(it) },
                isMultiSelect = true
            )
            3 -> SelectionStep(
                options = listOf("Never", "Rarely", "Occasionally", "Frequently", "Always"),
                selected = if (uiState.mealPlanningFreq.isEmpty()) emptyList() else listOf(uiState.mealPlanningFreq),
                onSelected = { viewModel.updateMealFreq(it.firstOrNull() ?: "") },
                isMultiSelect = false
            )
            4 -> SelectionStep(
                options = listOf("Yes, definitely", "Open to trying", "No thanks"),
                selected = if (uiState.weeklyMealPlans.isEmpty()) emptyList() else listOf(uiState.weeklyMealPlans),
                onSelected = { viewModel.updateWeeklyMealPlans(it.firstOrNull() ?: "") },
                isMultiSelect = false
            )
            5 -> SelectionStep(
                options = listOf("Not Very Active", "Lightly Active", "Active", "Very Active"),
                selected = if (uiState.activityLevel.isEmpty()) emptyList() else listOf(uiState.activityLevel),
                onSelected = { viewModel.updateActivityLevel(it.firstOrNull() ?: "") },
                isMultiSelect = false
            )
            6 -> DemographicsScreen(viewModel)
            7 -> HeightWeightScreen(viewModel)
            8 -> SelectionStep(
                options = listOf("Lose 0.25 kg per week", "Lose 0.5 kg per week", "Lose 0.75 kg per week", "Lose 1 kg per week"),
                selected = if (uiState.weeklyGoal.isEmpty()) emptyList() else listOf(uiState.weeklyGoal),
                onSelected = { viewModel.updateWeeklyGoal(it.firstOrNull() ?: "") },
                isMultiSelect = false
            )
        }
    }
}

@Composable
fun SelectionStep(
    options: List<String>,
    selected: List<String>,
    onSelected: (List<String>) -> Unit,
    isMultiSelect: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { option ->
            val isSelected = selected.contains(option)
            SelectionCard(
                text = option,
                isSelected = isSelected,
                onClick = {
                    if (isMultiSelect) {
                        if (isSelected) onSelected(selected - option)
                        else onSelected(selected + option)
                    } else {
                        onSelected(listOf(option))
                    }
                }
            )
        }
    }
}

@Composable
fun SelectionCard(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) OnboardingLightGreenTint else Color.White
        ),
        border = BorderStroke(
            1.dp, 
            if (isSelected) OnboardingPrimaryGreen else OnboardingLightGrey
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                color = Color.Black,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = OnboardingPrimaryGreen
                )
            }
        }
    }
}

@Composable
fun DemographicsScreen(viewModel: OnboardingViewModel) {
    val uiState by viewModel.uiState
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GenderBox(
                label = "Male",
                isSelected = uiState.gender == Gender.MALE,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.updateGender(Gender.MALE) }
            )
            GenderBox(
                label = "Female",
                isSelected = uiState.gender == Gender.FEMALE,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.updateGender(Gender.FEMALE) }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("How old are you?", fontWeight = FontWeight.SemiBold, color = Color.Black)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.age,
            onValueChange = { viewModel.updateAge(it) },
            placeholder = { Text("Enter Age", color = OnboardingDarkGrey) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(color = Color.Black),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OnboardingPrimaryGreen,
                unfocusedBorderColor = OnboardingLightGrey
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Where do you live?", fontWeight = FontWeight.SemiBold, color = Color.Black)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.country,
            onValueChange = { viewModel.updateCountry(it) },
            placeholder = { Text("Select Country", color = OnboardingDarkGrey) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            textStyle = TextStyle(color = Color.Black),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OnboardingPrimaryGreen,
                unfocusedBorderColor = OnboardingLightGrey
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "We use sex at birth and age to calculate an accurate goal for you.",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun GenderBox(label: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) OnboardingLightGreenTint else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) OnboardingPrimaryGreen else OnboardingLightGrey
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                color = Color.Black,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun HeightWeightScreen(viewModel: OnboardingViewModel) {
    val uiState by viewModel.uiState
    val diff by viewModel.weightDifference
    val status by viewModel.weightStatus
    val weeks by viewModel.weeksToGoal

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("How tall are you?", fontWeight = FontWeight.SemiBold, color = Color.Black)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HeightInputBox(
                value = uiState.heightFeet,
                onValueChange = { viewModel.updateHeightFeet(it) },
                label = "Feet (ft)",
                modifier = Modifier.weight(1f)
            )
            HeightInputBox(
                value = uiState.heightInches,
                onValueChange = { viewModel.updateHeightInches(it) },
                label = "Inches (in)",
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Weight Details", fontWeight = FontWeight.SemiBold, color = Color.Black)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.currentWeight,
            onValueChange = { viewModel.updateCurrentWeight(it) },
            placeholder = { Text("Current Weight (kg)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(color = Color.Black),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OnboardingPrimaryGreen,
                unfocusedBorderColor = OnboardingLightGrey
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = uiState.goalWeight,
            onValueChange = { viewModel.updateGoalWeight(it) },
            placeholder = { Text("Goal Weight (kg)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(color = Color.Black),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OnboardingPrimaryGreen,
                unfocusedBorderColor = OnboardingLightGrey
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AnimatedVisibility(
            visible = status.isNotEmpty(),
            enter = fadeIn() + expandVertically()
        ) {
            Column {
                val absDiff = abs(diff)
                val msg = when (status) {
                    "lose" -> "You are aiming to lose ${String.format(Locale.US, "%.1f", absDiff)} kg 💪"
                    "gain" -> "You are aiming to gain ${String.format(Locale.US, "%.1f", absDiff)} kg 💪"
                    "maintain" -> "You are maintaining your current weight."
                    else -> ""
                }
                
                Text(
                    text = msg,
                    color = OnboardingPrimaryGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                
                if (status != "maintain") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "At 0.5 kg per week, you can reach your goal in $weeks weeks.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun HeightInputBox(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 12.sp, color = Color.Black, modifier = Modifier.padding(bottom = 4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(color = Color.Black, textAlign = TextAlign.Center),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OnboardingPrimaryGreen,
                unfocusedBorderColor = OnboardingLightGrey
            ),
            singleLine = true
        )
    }
}

private fun getStepTitle(step: Int) = when (step) {
    0 -> "Hey! Let's start with your goals."
    1 -> "What have been your barriers to losing weight?"
    2 -> "Which health habits are most important to you?"
    3 -> "How often do you plan your meals in advance?"
    4 -> "Do you want us to help you build weekly meal plans?"
    5 -> "What is your baseline activity level?"
    6 -> "Tell us a little bit about yourself"
    7 -> "Just a few more questions"
    8 -> "What is your weekly goal?"
    else -> ""
}

private fun getStepSubtitle(step: Int) = when (step) {
    0 -> "Select up to three that are most important to you."
    1 -> "Select all that apply."
    2 -> "Recommended for you"
    4 -> "Your custom plan will be tailored to your lifestyle and goals."
    5 -> "Not including workouts - we count that separately."
    6 -> "Please select which sex we should use to calculate your calorie needs:"
    else -> ""
}

