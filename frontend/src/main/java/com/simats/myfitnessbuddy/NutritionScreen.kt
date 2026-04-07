package com.simats.myfitnessbuddy

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    appPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    onNavigateToProgress: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: NutritionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("Overview", "Calories", "Nutrients", "Macros")
    
    val backgroundColor = Color(0xFFF4F6FA)

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                TopAppBar(
                    title = { Text("Nutrition", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF4A6FFF))
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToProgress) {
                            Icon(Icons.Default.ShowChart, contentDescription = "Progress", tint = Color(0xFF22C55E))
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF4A6FFF))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // --- Tabs Section ---
            Surface(color = Color.White) {
                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF4A6FFF),
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                            height = 3.dp,
                            color = Color(0xFF4A6FFF)
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = uiState.selectedTab == index,
                            onClick = { viewModel.onTabSelected(index) },
                            text = {
                                Text(
                                    text = title,
                                    color = if (uiState.selectedTab == index) Color(0xFF4A6FFF) else Color.Gray,
                                    fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = uiState.selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "TabContent"
            ) { targetTab ->
                when (targetTab) {
                    0 -> OverviewContent(uiState)
                    1 -> CaloriesContent(uiState, viewModel)
                    2 -> NutrientsContent(uiState, viewModel)
                    3 -> MacrosContent(uiState, viewModel)
                    else -> PlaceholderContent(tabs[targetTab])
                }
            }
        }
    }
}

@Composable
fun MacrosContent(uiState: NutritionState, viewModel: NutritionViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- Day View Selector ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.onPreviousDay() }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = Color(0xFF4A6FFF))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Day View", fontSize = 12.sp, color = Color.Gray)
                    Text(uiState.dateDisplay, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = { viewModel.onNextDay() }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = Color(0xFF4A6FFF))
                }
            }
        }

        // --- Macro Donut Chart Card ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                        DonutChart(uiState.macros)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("0 kcal", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.DarkGray)
                            Text("Today", fontSize = 14.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Macro Legend Section
                    uiState.macros.forEachIndexed { index, macro ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(14.dp).background(macro.color, RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(macro.name, fontWeight = FontWeight.Bold, color = Color.DarkGray, modifier = Modifier.weight(1f))
                            Text("(${macro.grams}g) — ${macro.percentage}% / ${macro.targetPercentage}%", color = Color.Gray, fontSize = 14.sp)
                        }
                        if (index < uiState.macros.size - 1) {
                            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // --- Foods Highest Cards ---
        item { FoodsHighestCard("Carbohydrates", Color(0xFF22D3EE), uiState.highCalorieFoods) }
        item { FoodsHighestCard("Fat", Color(0xFFA78BFA), uiState.highCalorieFoods) }
        item { FoodsHighestCard("Protein", Color(0xFFF59E0B), uiState.highCalorieFoods) }

        // --- Bottom Banner ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(24.dp)).clickable { /* Meal Scan */ },
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Brush.horizontalGradient(colors = listOf(Color(0xFF00C896), Color(0xFF4A6FFF))))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Log it faster with Meal Scan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun DonutChart(macros: List<MacroData>) {
    val totalTarget = 100f
    var startAngle = -90f
    
    val animateStroke by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1500, easing = LinearOutSlowInEasing),
        label = "donutAnimation"
    )

    Canvas(modifier = Modifier.size(180.dp)) {
        macros.forEach { macro ->
            val sweepAngle = (macro.targetPercentage.toFloat() / totalTarget) * 360f
            drawArc(
                color = macro.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle * animateStroke,
                useCenter = false,
                style = Stroke(width = 25.dp.toPx(), cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun FoodsHighestCard(title: String, stripColor: Color, foods: List<HighCalorieFood>) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(stripColor))
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Foods Highest in $title", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFACC15))
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                foods.forEach { food ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(food.name, color = Color.DarkGray)
                        Text("${food.value} ${food.unit}", color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { /* Analyze */ },
                    modifier = Modifier.fillMaxWidth().height(50.dp).shadow(6.dp, RoundedCornerShape(50.dp)),
                    shape = RoundedCornerShape(50.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF4A6FFF), Color(0xFF9C6CFF))))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("\uD83D\uDC51 Analyze My Foods", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NutrientsContent(uiState: NutritionState, viewModel: NutritionViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Day View Selector ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.onPreviousDay() }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = Color(0xFF4A6FFF))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Day View", fontSize = 12.sp, color = Color.Gray)
                        Text(uiState.dateDisplay, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = { viewModel.onNextDay() }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = Color(0xFF4A6FFF))
                    }
                }
            }

            // --- Protein Suggestion Banner ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Need more protein?", color = Color.DarkGray, fontWeight = FontWeight.Bold)
                            Text(
                                "Browse high protein recipes",
                                color = Color(0xFF4A6FFF),
                                fontSize = 14.sp,
                                modifier = Modifier.clickable { /* Browse */ }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.LightGray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }
            }

            // --- Nutrient Table Header ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("", modifier = Modifier.weight(1.5f))
                    Text("Total", modifier = Modifier.weight(1f), color = Color.Gray, textAlign = TextAlign.End, fontSize = 14.sp)
                    Text("Goal", modifier = Modifier.weight(1f), color = Color.Gray, textAlign = TextAlign.End, fontSize = 14.sp)
                    Text("Left", modifier = Modifier.weight(1f), color = Color.Gray, textAlign = TextAlign.End, fontSize = 14.sp)
                }
            }

            // --- Nutrient List ---
            items(uiState.nutrientsList) { nutrient ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(nutrient.name, color = Color.DarkGray, modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Medium)
                        Text("${nutrient.total}", color = Color.DarkGray, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text("${nutrient.goal}", color = Color.DarkGray, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text("${nutrient.left} ${nutrient.unit}", color = Color.DarkGray, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        // --- Bottom Banner ---
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
            ) {
                Box(
                    modifier = Modifier
                        .background(Brush.horizontalGradient(listOf(Color(0xFF00C896), Color(0xFF4A6FFF))))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Log it faster with Barcode Scan", color = Color.White, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewContent(uiState: NutritionState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { PoweringUpSection(uiState.overallProgress) }
        item { BoostProgressSection() }
        item { TrackFoodSection(uiState.foodsTracked, uiState.mealsTracked) }
        item { Text("Hit Your Top Nutrient Goals", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        items(uiState.nutrientGoals) { goal -> NutrientGoalCard(goal) }
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = { /* View all */ }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("View All Nutrients", color = Color(0xFF4A6FFF), fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF4A6FFF), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        item { AboutProgressCard() }
        item { BottomMealScanBanner() }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun CaloriesContent(uiState: NutritionState, viewModel: NutritionViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- Day View Selector ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.onPreviousDay() }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = Color(0xFF4A6FFF))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Day View", fontSize = 12.sp, color = Color.Gray)
                    Text(uiState.dateDisplay, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = { viewModel.onNextDay() }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = Color(0xFF4A6FFF))
                }
            }
        }

        // --- Calories Summary Card ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Pie Chart Placeholder
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        PieChartPlaceholder()
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Legend Grid
                    LegendGrid()

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(20.dp))

                    // Stats Section
                    CalorieStatRow("Total Calories", uiState.calorieStat.total.toString())
                    CalorieStatRow("Net Calories", uiState.calorieStat.net.toString())
                    CalorieStatRow("Goal", String.format("%, d", uiState.calorieStat.goal), isGoal = true)
                }
            }
        }

        // --- Foods Highest in Calories Card ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Foods Highest in Calories", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFACC15))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    uiState.highCalorieFoods.forEach { food ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(food.name, color = Color.DarkGray)
                            Text("${food.value} ${food.unit}", color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { /* Analyze */ },
                        modifier = Modifier.fillMaxWidth().height(50.dp).shadow(6.dp, RoundedCornerShape(50.dp)),
                        shape = RoundedCornerShape(50.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(Color(0xFF4A6FFF), Color(0xFF9C6CFF))))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("\uD83D\uDC51 Analyze My Foods", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- Bottom Banner ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(24.dp)).clickable { /* Barcode */ },
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Brush.horizontalGradient(colors = listOf(Color(0xFF00C896), Color(0xFF4A6FFF))))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Log it faster with Barcode Scan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun PieChartPlaceholder() {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(1500, easing = LinearOutSlowInEasing),
        label = "pieAnimation"
    )
    
    LaunchedEffect(Unit) { animationPlayed = true }

    Canvas(modifier = Modifier.size(160.dp)) {
        drawCircle(
            color = Color.LightGray.copy(alpha = 0.3f),
            style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(
            color = Color.LightGray.copy(alpha = 0.1f),
            radius = size.minDimension / 2 - 10.dp.toPx()
        )
    }
}

@Composable
fun LegendGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            LegendItem(Modifier.weight(1f), Color(0xFF4A6FFF), "Breakfast", 0, 0)
            LegendItem(Modifier.weight(1f), Color(0xFF9C6CFF), "Lunch", 0, 0)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            LegendItem(Modifier.weight(1f), Color(0xFF4A6FFF).copy(alpha = 0.6f), "Dinner", 0, 0)
            LegendItem(Modifier.weight(1f), Color(0xFF00C896), "Snacks", 0, 0)
        }
    }
}

@Composable
fun LegendItem(modifier: Modifier, color: Color, label: String, percentage: Int, calories: Int) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Text("$label — $percentage% ($calories cal)", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun CalorieStatRow(label: String, value: String, isGoal: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.DarkGray)
        Text(
            value,
            fontWeight = FontWeight.Bold,
            color = if (isGoal) Color(0xFF4A6FFF) else Color.DarkGray
        )
    }
}

@Composable
fun PlaceholderContent(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Content for $title Coming Soon", color = Color.Gray)
    }
}

@Composable
fun PoweringUpSection(progress: Float) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(1000))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Powering up", fontWeight = FontWeight.Medium, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF4A6FFF),
                    strokeWidth = 8.dp,
                    trackColor = Color.LightGray.copy(alpha = 0.3f),
                    strokeCap = StrokeCap.Round
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF4A6FFF)
                )
            }
        }
    }
}

@Composable
fun BoostProgressSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("To boost today's progress:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        
        BoostStepItem(1, "Track your food")
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
        BoostStepItem(2, "Hit your top nutrient goals")
    }
}

@Composable
fun BoostStepItem(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Color.LightGray.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number.toString(), fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
        }
        Text(text, fontWeight = FontWeight.Medium, color = Color.DarkGray)
    }
}

@Composable
fun TrackFoodSection(foods: Int, meals: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text("Track your food", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Logging consistent meals helps AI predict your progress", fontSize = 14.sp, color = Color.Gray)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FoodStatCard(Icons.Default.Restaurant, "$foods foods", Color(0xFF4A6FFF), Modifier.weight(1f))
            FoodStatCard(Icons.Default.SoupKitchen, "$meals meals", Color(0xFF9C6CFF), Modifier.weight(1f))
        }
    }
}

@Composable
fun FoodStatCard(icon: ImageVector, text: String, iconColor: Color, modifier: Modifier) {
    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
            Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun NutrientGoalCard(goal: NutrientGoal) {
    val animatedProgress by animateFloatAsState(
        targetValue = goal.current / goal.goal,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "nutrientProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(goal.iconColor.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when(goal.name) {
                            "Protein" -> Icons.Default.KebabDining
                            "Fiber" -> Icons.Default.Grass
                            else -> Icons.Default.BakeryDining
                        }
                        Icon(icon, contentDescription = null, tint = goal.iconColor, modifier = Modifier.size(24.dp))
                    }
                    Text(goal.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text("${goal.current.toInt()}${goal.unit} of ${goal.goal.toInt()}${goal.unit}", color = Color.Gray, fontSize = 14.sp)
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = goal.color,
                trackColor = Color.LightGray.copy(alpha = 0.3f),
                strokeCap = StrokeCap.Round
            )

            Text(goal.description, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun AboutProgressCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Based on your activity and goals, we recommend focusing on high-protein meals today to support muscle recovery from your morning workout.",
                fontSize = 14.sp,
                color = Color.DarkGray,
                lineHeight = 20.sp
            )
            Text(
                "How we make recommendations",
                color = Color(0xFF4A6FFF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { /* Explainer */ }
            )
        }
    }
}

@Composable
fun BottomMealScanBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clickable { /* Meal Scan */ },
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(colors = listOf(Color(0xFF00C896), Color(0xFF4A6FFF))))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Log it faster with Meal Scan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Snap a photo of your plate", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
            }
        }
    }
}
