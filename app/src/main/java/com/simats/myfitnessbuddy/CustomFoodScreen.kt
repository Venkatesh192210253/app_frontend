package com.simats.myfitnessbuddy

import androidx.compose.foundation.layout.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFoodScreen(
    onBack: () -> Unit,
    onFoodAdded: (String, String, Int, String) -> Unit
) {
    var foodName by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    
    var selectedMeal by remember { mutableStateOf("Breakfast") }
    val meals = listOf("Breakfast", "Lunch", "Dinner", "Snacks")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Custom Food", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF4F6FA)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Select Meal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                meals.forEach { meal ->
                    FilterChip(
                        selected = selectedMeal == meal,
                        onClick = { selectedMeal = meal },
                        label = { Text(meal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF22C55E).copy(alpha = 0.1f),
                            selectedLabelColor = Color(0xFF22C55E),
                            labelColor = Color.Gray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.LightGray.copy(alpha = 0.5f),
                            selectedBorderColor = Color(0xFF22C55E),
                            enabled = true,
                            selected = selectedMeal == meal
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = foodName,
                onValueChange = { foodName = it },
                label = { Text("Food Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = calories,
                onValueChange = { calories = it },
                label = { Text("Calories (kcal)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text("Carbs (g)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text("Protein (g)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text("Fat (g)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            val scope = rememberCoroutineScope()
            var isSaving by remember { mutableStateOf(false) }

            LoadingButton(
                text = "Save and Add",
                isLoading = isSaving,
                onClick = {
                    if (foodName.isNotBlank() && calories.isNotBlank()) {
                        isSaving = true
                        scope.launch {
                            kotlinx.coroutines.delay(600)
                            val macros = "C: ${carbs.ifBlank { "0" }}g • P: ${protein.ifBlank { "0" }}g • F: ${fat.ifBlank { "0" }}g"
                            onFoodAdded(selectedMeal, foodName, calories.toIntOrNull() ?: 0, macros)
                            onBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color(0xFF4A6FFF),
                icon = Icons.Default.Save,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}
