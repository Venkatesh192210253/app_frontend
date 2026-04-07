package com.simats.myfitnessbuddy

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScanScreen(
    onBack: () -> Unit,
    viewModel: DiaryViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val scannedResult = uiState.scannedBarcodeResult
    var manualQuery by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition Lookup", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { 
                        viewModel.clearScannedBarcode()
                        onBack() 
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (scannedResult == null) {
                // Initial Search UI
                Spacer(modifier = Modifier.height(40.dp))
                
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color(0xFF00C896).copy(alpha = 0.5f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "Find Nutritional Data",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    "Enter the product name to see the full nutrition facts table from the back side.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = manualQuery,
                    onValueChange = { manualQuery = it },
                    placeholder = { Text("e.g., Oreo Cookies, Kellogg's, etc.", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00C896),
                        unfocusedBorderColor = Color.DarkGray,
                        cursorColor = Color(0xFF00C896),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    singleLine = true,
                    trailingIcon = {
                        if (uiState.isAiSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF00C896),
                                strokeWidth = 2.dp
                            )
                        } else if (manualQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                viewModel.searchFoodAi(manualQuery, setAsScanned = true)
                            }) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Search", tint = Color(0xFF00C896))
                            }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { viewModel.searchFoodAi(manualQuery, setAsScanned = true) },
                    enabled = manualQuery.isNotEmpty() && !uiState.isAiSearching,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00C896),
                        disabledContainerColor = Color.DarkGray
                    )
                ) {
                    Text("Lookup Nutrition", fontWeight = FontWeight.Bold)
                }
            } else {
                // Result Display
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFF00C896).copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF00C896))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Product Found", fontSize = 12.sp, color = Color.Gray)
                                Text(scannedResult.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.DarkGray)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        var selectedMealType by remember { mutableStateOf("Breakfast") }
                        Text("Log to:", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { type ->
                                val isSelected = selectedMealType == type
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedMealType = type },
                                    label = { Text(type, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00C896).copy(alpha = 0.1f),
                                        selectedLabelColor = Color(0xFF00C896)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Detailed Nutrition Facts Table
                        NutritionFactsTable(scannedResult)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.clearScannedBarcode() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Search Again")
                            }
                            Button(
                                onClick = { 
                                    viewModel.addFoodToMeal(selectedMealType, scannedResult)
                                    viewModel.clearScannedBarcode()
                                    onBack() 
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C896))
                            ) {
                                Text("Add to Diary")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NutritionFactsTable(food: FoodEntry) {
    val dvMap = mapOf(
        "fat" to 78.0,
        "satFat" to 20.0,
        "cholesterol" to 300.0,
        "sodium" to 2300.0,
        "carbs" to 275.0,
        "fiber" to 28.0,
        "protein" to 50.0,
        "potassium" to 4700.0,
        "calcium" to 1300.0,
        "iron" to 18.0,
        "vitaminD" to 20.0,
        "addedSugars" to 50.0
    )

    fun calcDV(value: Float, key: String): String {
        val dv = dvMap[key] ?: return ""
        if (dv == 0.0) return ""
        val percentage = (value / dv * 100).toInt()
        return "$percentage%"
    }
    
    fun formatWeight(value: Float): String {
        return if (value > 0 && value < 1) "%.1f".format(value) else "${value.toInt()}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black)
            .padding(1.dp)
            .background(Color.White)
            .padding(8.dp)
    ) {
        Text("Nutrition Facts", fontWeight = FontWeight.Black, fontSize = 32.sp, color = Color.Black)
        Divider(thickness = 8.dp, color = Color.Black)
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("${food.servingsPerContainer} servings per container", fontSize = 14.sp, color = Color.Black)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Serving size", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(food.servingSize, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                }
            }
        }
        
        Divider(thickness = 4.dp, color = Color.Black, modifier = Modifier.padding(vertical = 4.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text("Amount per serving", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                Text("Calories", fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color.Black)
            }
            Text("${food.calories}", fontWeight = FontWeight.Black, fontSize = 36.sp, color = Color.Black)
        }
        
        Divider(thickness = 2.dp, color = Color.Black)
        
        Text("% Daily Value*", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
        
        NutritionRow("Total Fat", "${formatWeight(food.fat)}g", calcDV(food.fat, "fat"), isBold = true)
        NutritionRow("Saturated Fat", "${formatWeight(food.saturatedFat)}g", calcDV(food.saturatedFat, "satFat"), indent = 16)
        NutritionRow("Trans Fat", "${formatWeight(food.transFat)}g", "", indent = 16)
        NutritionRow("Polyunsaturated Fat", "${formatWeight(food.polyunsaturatedFat)}g", "", indent = 16)
        NutritionRow("Monounsaturated Fat", "${formatWeight(food.monounsaturatedFat)}g", "", indent = 16)
        
        NutritionRow("Cholesterol", "${food.cholesterol.toInt()}mg", calcDV(food.cholesterol, "cholesterol"), isBold = true)
        NutritionRow("Sodium", "${food.sodium.toInt()}mg", calcDV(food.sodium, "sodium"), isBold = true)
        
        NutritionRow("Total Carbohydrate", "${formatWeight(food.carbs)}g", calcDV(food.carbs, "carbs"), isBold = true)
        NutritionRow("Dietary Fiber", "${formatWeight(food.dietaryFiber)}g", calcDV(food.dietaryFiber, "fiber"), indent = 16)
        NutritionRow("Total Sugars", "${formatWeight(food.totalSugars)}g", "", indent = 16)
        NutritionRow("Includes ${formatWeight(food.addedSugars)}g Added Sugars", "", calcDV(food.addedSugars, "addedSugars"), indent = 32)
        
        NutritionRow("Protein", "${formatWeight(food.protein)}g", calcDV(food.protein, "protein"), isBold = true)
        
        Divider(thickness = 8.dp, color = Color.Black, modifier = Modifier.padding(vertical = 4.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Vitamin D ${food.vitaminD}mcg ${calcDV(food.vitaminD, "vitaminD")}", fontSize = 11.sp, color = Color.Black)
            Text("Calcium ${food.calcium.toInt()}mg ${calcDV(food.calcium, "calcium")}", fontSize = 11.sp, color = Color.Black)
        }
        Divider(thickness = 1.dp, color = Color.Gray, modifier = Modifier.padding(vertical = 2.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Iron ${food.iron}mg ${calcDV(food.iron, "iron")}", fontSize = 11.sp, color = Color.Black)
            Text("Potassium ${food.potassium.toInt()}mg ${calcDV(food.potassium, "potassium")}", fontSize = 11.sp, color = Color.Black)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text("* The % Daily Value (DV) tells you how much a nutrient in a serving of food contributes to a daily diet. 2,000 calories a day is used for general nutrition advice.", fontSize = 10.sp, lineHeight = 12.sp, color = Color.Black)
    }
}

@Composable
fun NutritionRow(label: String, value: String, percent: String, isBold: Boolean = false, indent: Int = 0) {
    HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = indent.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row {
            Text(label, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp, color = if (isBold) Color.Black else Color.DarkGray)
            if (value.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(value, fontSize = 14.sp, color = Color.DarkGray)
            }
        }
        if (percent.isNotEmpty()) {
            Text(percent, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
        }
    }
}
