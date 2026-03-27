package com.simats.myfitnessbuddy

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFoodScreen(
    onBack: () -> Unit,
    appPadding: PaddingValues = PaddingValues(0.dp),
    initialMealType: String = "Breakfast",
    viewModel: DiaryViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(initialMealType) }
    val filters = listOf("Breakfast", "Lunch", "Dinner", "Snacks")

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Trigger search when query OR filter changes
    LaunchedEffect(searchQuery, selectedFilter) {
        viewModel.searchFoods(searchQuery, selectedFilter)
    }

    // Find entries for the current meal to track counts and IDs
    val currentMealEntries = uiState.meals.find { it.name.equals(selectedFilter, ignoreCase = true) }?.foods ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Food", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF4A6FFF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF4F6FA)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + appPadding.calculateBottomPadding()
                )
        ) {
            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp)),
                placeholder = { Text("Search for meals or brands...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // Category Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF22C55E).copy(alpha = 0.1f),
                            selectedLabelColor = Color(0xFF22C55E),
                            labelColor = Color.Gray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.LightGray.copy(alpha = 0.5f),
                            selectedBorderColor = Color(0xFF22C55E),
                            enabled = true,
                            selected = selectedFilter == filter
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isAiSearching) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF22C55E))
                }
            }

            // Food List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.searchResult.isEmpty() && searchQuery.isNotEmpty() && !uiState.isAiSearching) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No results found for \"$searchQuery\"", color = Color.Gray)
                        }
                    }
                }

                items(uiState.searchResult) { food ->
                    val foodEntry = currentMealEntries.find { it.name == food.name }
                    val count = foodEntry?.quantity?.filter { char -> char.isDigit() }?.toIntOrNull() ?: 0
                    
                    FoodListItem(
                        food = food, 
                        count = count,
                        onAdd = {
                            viewModel.addFoodToMeal(selectedFilter, food)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Added to $selectedFilter")
                            }
                        },
                        onRemove = {
                            foodEntry?.id?.let { id ->
                                viewModel.deleteFoodEntry(id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FoodListItem(
    food: FoodEntry, 
    count: Int, 
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF22C55E).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${food.calories} kcal", fontSize = 13.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color.LightGray))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(food.macros, fontSize = 11.sp, color = Color.Gray)
                }
            }

            if (count > 0) {
                // Inline Quantity Selector: - count +
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF22C55E))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    
                    Text(
                        text = count.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            } else {
                // ADD Button
                OutlinedButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF22C55E)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text("ADD", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
