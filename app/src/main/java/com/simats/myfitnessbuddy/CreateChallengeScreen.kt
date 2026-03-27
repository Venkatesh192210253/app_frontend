package com.simats.myfitnessbuddy

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.simats.myfitnessbuddy.data.local.SettingsManager
import com.simats.myfitnessbuddy.data.remote.RetrofitClient

// --- Models ---

data class ChallengeTemplate(
    val id: String,
    val name: String,
    val duration: Int,
    val type: String,
    val target: String,
    val icon: ImageVector
)

// --- ViewModel ---

class CreateChallengeViewModel : ViewModel() {
    val templates = listOf(
        ChallengeTemplate("1", "10K Steps Daily", 7, "Steps", "10000 steps", Icons.Default.DirectionsRun),
        ChallengeTemplate("2", "Workout Streak", 14, "Workouts", "14 days streak", Icons.Default.FitnessCenter),
        ChallengeTemplate("3", "Calorie Burn", 7, "Calories", "500 kcal daily", Icons.Default.Whatshot),
        ChallengeTemplate("4", "Protein Goal", 7, "Protein", "120g protein", Icons.Default.Restaurant)
    )

    private val _challengeName = MutableStateFlow("")
    val challengeName: StateFlow<String> = _challengeName.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _type = MutableStateFlow("Steps")
    val type: StateFlow<String> = _type.asStateFlow()

    private val _duration = MutableStateFlow("7")
    val duration: StateFlow<String> = _duration.asStateFlow()

    private val _targetValue = MutableStateFlow("")
    val targetValue: StateFlow<String> = _targetValue.asStateFlow()

    private val _pointsReward = MutableStateFlow("500")
    val pointsReward: StateFlow<String> = _pointsReward.asStateFlow()

    private val _allMembersRequired = MutableStateFlow(true)
    val allMembersRequired: StateFlow<Boolean> = _allMembersRequired.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun onNameChange(value: String) { _challengeName.value = value }
    fun onDescriptionChange(value: String) { _description.value = value }
    fun onTypeChange(value: String) { _type.value = value }
    fun onDurationChange(value: String) { _duration.value = value }
    fun onTargetChange(value: String) { _targetValue.value = value }
    fun onPointsChange(value: String) { _pointsReward.value = value }
    fun onParticipationChange(required: Boolean) { _allMembersRequired.value = required }

    fun applyTemplate(template: ChallengeTemplate) {
        _challengeName.value = template.name
        _type.value = template.type
        _duration.value = template.duration.toString()
        _targetValue.value = template.target
    }

    fun createChallenge(groupId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SettingsManager.authToken ?: ""
                val response = RetrofitClient.apiService.createChallenge(mapOf(
                        "group_id" to groupId,
                        "name" to _challengeName.value,
                        "description" to _description.value,
                        "type" to _type.value,
                        "duration" to _duration.value,
                        "target" to _targetValue.value,
                        "points" to _pointsReward.value
                    )
                )
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to create challenge"
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                onError(e.message ?: "An unexpected error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// --- UI Components ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChallengeScreen(
    appPadding: PaddingValues = PaddingValues(0.dp),
    groupId: String,
    onBack: () -> Unit,
    viewModel: CreateChallengeViewModel = viewModel()
) {
    val challengeName by viewModel.challengeName.collectAsState()
    val description by viewModel.description.collectAsState()
    val type by viewModel.type.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val targetValue by viewModel.targetValue.collectAsState()
    val pointsReward by viewModel.pointsReward.collectAsState()
    val allMembersRequired by viewModel.allMembersRequired.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(shadowElevation = 2.dp) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Create Challenge", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Motivate your group", fontSize = 12.sp, color = Color.Gray)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        },
        containerColor = Color(0xFFF4F6FA),
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { 
                            if (challengeName.isNotBlank()) {
                                viewModel.createChallenge(groupId, 
                                    onSuccess = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Challenge created successfully!")
                                            onBack()
                                        }
                                    },
                                    onError = { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Error: $error")
                                        }
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C896)),
                        shape = RoundedCornerShape(50.dp),
                        enabled = !isLoading && challengeName.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.TrackChanges, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Challenge", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + appPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + appPadding.calculateBottomPadding() + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 3. Quick Templates Section
            item {
                Text("Quick Templates", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            }
            
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TemplateGridItem(viewModel.templates[0], onClick = { viewModel.applyTemplate(viewModel.templates[0]) }, Modifier.weight(1f))
                    TemplateGridItem(viewModel.templates[1], onClick = { viewModel.applyTemplate(viewModel.templates[1]) }, Modifier.weight(1f))
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TemplateGridItem(viewModel.templates[2], onClick = { viewModel.applyTemplate(viewModel.templates[2]) }, Modifier.weight(1f))
                    TemplateGridItem(viewModel.templates[3], onClick = { viewModel.applyTemplate(viewModel.templates[3]) }, Modifier.weight(1f))
                }
            }

            // 4. Custom Challenge Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = challengeName,
                            onValueChange = { viewModel.onNameChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g., Summer Shred Challenge") },
                            label = { Text("Challenge Name") },
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { viewModel.onDescriptionChange(it) },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            placeholder = { Text("Describe the challenge…") },
                            label = { Text("Description") },
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Type Dropdown (simplified as a text field for mock)
                        OutlinedTextField(
                            value = type,
                            onValueChange = { viewModel.onTypeChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Type") },
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = duration,
                                onValueChange = { viewModel.onDurationChange(it) },
                                modifier = Modifier.weight(1f),
                                label = { Text("Duration (days)") },
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = targetValue,
                                onValueChange = { viewModel.onTargetChange(it) },
                                modifier = Modifier.weight(1.5f),
                                label = { Text("Target Value") },
                                placeholder = { Text("e.g., 10000 steps") },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        OutlinedTextField(
                            value = pointsReward,
                            onValueChange = { viewModel.onPointsChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Points Reward") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // 5. Participation Section
            item {
                Text("Participation", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ParticipationOption(
                        label = "All Members",
                        subtitle = "Required",
                        isSelected = allMembersRequired,
                        onClick = { viewModel.onParticipationChange(true) },
                        modifier = Modifier.weight(1f)
                    )
                    ParticipationOption(
                        label = "Optional",
                        subtitle = "Choose to join",
                        isSelected = !allMembersRequired,
                        onClick = { viewModel.onParticipationChange(false) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun TemplateGridItem(template: ChallengeTemplate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable { onClick() }.shadow(1.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(template.icon, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(template.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("${template.duration} days", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ParticipationOption(
    label: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) Color(0xFFDCFCE7) else Color(0xFFF3F4F6)
    val borderColor = if (isSelected) Color(0xFF22C55E) else Color.Transparent

    Surface(
        modifier = modifier.clickable { onClick() },
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFF166534) else Color.DarkGray)
            Text(subtitle, fontSize = 10.sp, color = if (isSelected) Color(0xFF166534).copy(alpha = 0.7f) else Color.Gray)
        }
    }
}
