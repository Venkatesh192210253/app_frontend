package com.simats.myfitnessbuddy

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.ComponentActivity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

// --- Models ---

data class AiChatMessage(
    val id: String,
    val text: String,
    val isAi: Boolean,
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class AiFeature(
    val title: String,
    val icon: ImageVector,
    val color: Color
)

// --- ViewModel ---

class AICoachViewModel : ViewModel() {
    private val _messages = MutableStateFlow(
        listOf(
            AiChatMessage("1", "Hi! I’m your AI Fitness Coach. I’m here to help you achieve your goals. How can I assist you today?", true),
            AiChatMessage("2", "Based on your recent activity, I noticed you’ve been very consistent with your workouts. Great job! 💪", true)
        )
    )
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    fun sendMessage(text: String, imageBase64: String? = null) {
        if (text.isBlank() && imageBase64 == null) return
        
        val userMsg = AiChatMessage(System.currentTimeMillis().toString(), text, false, imageBase64)
        _messages.value += userMsg
        
        // Simulate AI thinking
        simulateAiResponse(text, imageBase64)
    }

    private fun simulateAiResponse(userText: String, imageBase64: String? = null) {
        viewModelScope.launch {
            _isTyping.value = true
            
            try {
                // Using the local trained AI endpoint instead of the old ChatGPT endpoint
                val request = com.simats.myfitnessbuddy.data.remote.TrainedAiRequest(
                    question = userText
                )

                val response = RetrofitClient.apiService.askTrainedAi(request)
                
                if (response.isSuccessful) {
                    val reply = response.body()?.answer ?: "Sorry, I couldn't process that response."
                    val aiMsg = AiChatMessage((System.currentTimeMillis() + 1).toString(), reply, true)
                    _messages.value += aiMsg
                } else {
                    val errorMsg = AiChatMessage((System.currentTimeMillis() + 1).toString(), "Server Error: ${response.code()}", true)
                    _messages.value += errorMsg
                }
            } catch (e: Exception) {
                val errorMsg = AiChatMessage((System.currentTimeMillis() + 1).toString(), "Network connecting failed. Please try again later.", true)
                _messages.value += errorMsg
            } finally {
                _isTyping.value = false
            }
        }
    }

    val quickActions = listOf(
        "Analyze my progress",
        "Suggest today’s workout",
        "Review my diet",
        "Predict my results"
    )

    val features = listOf(
        AiFeature("Progress Prediction", Icons.Outlined.AutoGraph, Color(0xFFE0F2F1)),
        AiFeature("Workout Tips", Icons.Outlined.Lightbulb, Color(0xFFFFF3E0)),
        AiFeature("Goal Adjustment", Icons.Outlined.Adjust, Color(0xFFE3F2FD)),
        AiFeature("Motivation", Icons.Outlined.LocalFireDepartment, Color(0xFFFCE4EC))
    )
}

// --- Helper Functions ---
fun encodeBitmapToBase64(bitmap: Bitmap): String {
    // Calculate scaling to max 800px on the longest side to keep base64 payload reasonable
    val maxDimension = 800f
    val ratio = kotlin.math.min(maxDimension / bitmap.width, maxDimension / bitmap.height)
    val width = (bitmap.width * ratio).toInt()
    val height = (bitmap.height * ratio).toInt()
    
    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
    
    val outputStream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
}

// --- UI Components ---

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AICoachScreen(
    appPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit = {},
    viewModel: AICoachViewModel = viewModel(viewModelStoreOwner = androidx.compose.ui.platform.LocalContext.current as ComponentActivity)
) {
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    
    AICoachScreenContent(
        messages = messages,
        isTyping = isTyping,
        quickActions = viewModel.quickActions,
        features = viewModel.features,
        appPadding = appPadding,
        onBack = onBack,
        onSendMessage = { text, image -> viewModel.sendMessage(text, image) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AICoachScreenContent(
    messages: List<AiChatMessage>,
    isTyping: Boolean,
    quickActions: List<String>,
    features: List<AiFeature>,
    appPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit = {},
    onSendMessage: (String, String?) -> Unit = { _, _ -> }
) {
    var inputText by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    // UI State for attachment menu
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }

    // --- Media Access Launchers ---
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Gallery Launcher
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val base64Image = encodeBitmapToBase64(bitmap)
                onSendMessage("Please analyze this image. Identify the food or fruit shown, and then provide a thorough comparative nutritional analysis comparing it to other similar common foods or healthy alternatives.", base64Image)
                showAttachmentMenu = false
            } catch (e: Exception) {
                e.printStackTrace()
                onSendMessage("Error loading image from gallery.", null)
            }
        }
    }

    // Camera Launcher
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        if (bitmap != null) {
            val base64Image = encodeBitmapToBase64(bitmap)
            onSendMessage("Please analyze this image. Identify the food or fruit shown, and then provide a thorough comparative nutritional analysis comparing it to other similar common foods or healthy alternatives.", base64Image)
            showAttachmentMenu = false
        }
    }

    // Permissions Launchers
    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch(null)
        }
    }

    val recordAudioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            isRecording = true
        }
    }

    // Auto-scroll when new messages arrive or typing starts
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size + if (isTyping) 1 else 0)
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(top = appPadding.calculateTopPadding()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Gray)
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI Coach", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        },
        containerColor = Color(0xFFF4F6FA),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                // Attachment Menu Overlay
                AnimatedVisibility(
                    visible = showAttachmentMenu,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    AttachmentMenu(
                        onClose = { showAttachmentMenu = false },
                        onCameraClick = {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        },
                        onPhotosClick = {
                            galleryLauncher.launch("image/*")
                        }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Styled Input Bar (Light Theme)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color(0xFFF3F4F6))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color.Black, 
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF22C55E)),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (inputText.isEmpty()) {
                                            Text("Ask AI Coach...", color = Color.Gray, fontSize = 15.sp)
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Voice Wave / Send Button
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable(enabled = inputText.isNotBlank()) {
                                    if (inputText.isNotBlank()) {
                                        onSendMessage(inputText, null)
                                        inputText = ""
                                    }
                                },
                            shape = CircleShape,
                            color = if (inputText.isNotBlank()) Color(0xFF22C55E) else Color.LightGray,
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // 4. Quick Actions Section (Now at the top)
            item {
                Text("Quick actions:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickActions.forEach { action ->
                        QuickActionPill(action, onClick = { onSendMessage(action, null) })
                    }
                }
            }
            // 5. Feature Cards Grid (Now at the top)
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AiFeatureCard(
                        feature = features[0], 
                        onClick = { onSendMessage(features[0].title, null) },
                        modifier = Modifier.weight(1f)
                    )
                    AiFeatureCard(
                        feature = features[1], 
                        onClick = { onSendMessage(features[1].title, null) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AiFeatureCard(
                        feature = features[2], 
                        onClick = { onSendMessage(features[2].title, null) },
                        modifier = Modifier.weight(1f)
                    )
                    AiFeatureCard(
                        feature = features[3], 
                        onClick = { onSendMessage(features[3].title, null) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            items(messages) { message ->
                AiChatBubble(message)
            }

            if (isTyping) {
                item {
                    TypingIndicator()
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun AiChatBubble(message: AiChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isAi) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        if (message.isAi) {
            Icon(
                Icons.Default.AutoAwesome, 
                contentDescription = null, 
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(24.dp).padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Surface(
            color = if (message.isAi) Color(0xFFDCFCE7) else Color.White,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.imageBase64 != null) {
                    val bitmap = remember(message.imageBase64) {
                        try {
                            val imageBytes = Base64.decode(message.imageBase64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Attached food image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                Text(
                    text = formatAiMessage(message.text),
                    fontSize = 14.sp,
                    color = if (message.isAi) Color(0xFF166534) else Color(0xFF1F2937),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun formatAiMessage(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            if (i < text.length - 1 && text[i] == '*' && text[i + 1] == '*') {
                // Start of bold
                i += 2
                val endBold = text.indexOf("**", i)
                if (endBold != -1) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i, endBold))
                    }
                    i = endBold + 2
                } else {
                    // Unmatched **
                    append("**")
                }
            } else if (text[i] == '*' && (i == 0 || text[i - 1] == '\n') && (i == text.length - 1 || text[i + 1] == ' ')) {
                // Bullet point - replace with a cleaner dot
                append("•")
                i += 1
            } else {
                append(text[i])
                i++
            }
        }
    }
}

@Composable
fun QuickActionPill(text: String, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(50.dp),
        border = BorderStroke(1.dp, Color(0xFF22C55E)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF166534)
        )
    }
}

@Composable
fun AiFeatureCard(feature: AiFeature, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "pressEffect")

    Card(
        modifier = modifier
            .scale(scale)
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(feature.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(feature.icon, contentDescription = null, tint = Color.DarkGray.copy(alpha = 0.6f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                feature.title, 
                fontWeight = FontWeight.Bold, 
                fontSize = 14.sp, 
                color = Color(0xFF1F2937),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(start = 32.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "typing")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        Text("AI Coach is thinking...", fontSize = 12.sp, color = Color.Gray.copy(alpha = alpha), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AttachmentMenu(
    onClose: () -> Unit,
    onCameraClick: () -> Unit,
    onPhotosClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Create", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AttachmentItem("Camera", Icons.Default.CameraAlt, Color(0xFFF0FDF4), Color(0xFF22C55E), onCameraClick)
                AttachmentItem("Photos", Icons.Default.Image, Color(0xFFEFF6FF), Color(0xFF3B82F6), onPhotosClick)
                AttachmentItem("Document", Icons.Default.Description, Color(0xFFFEF2F2), Color(0xFFEF4444), {})
            }
        }
    }
}

@Composable
fun AttachmentItem(label: String, icon: ImageVector, bgColor: Color, iconColor: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Preview(showBackground = true)
@Composable
fun AICoachScreenPreview() {
    val sampleMessages = listOf(
        AiChatMessage("1", "Hello! I'm your AI Coach.", true),
        AiChatMessage("2", "How can I help you today?", true),
        AiChatMessage("3", "Tell me about my progress.", false)
    )
    val sampleFeatures = listOf(
        AiFeature("Progress Prediction", Icons.Outlined.AutoGraph, Color(0xFFE0F2F1)),
        AiFeature("Workout Tips", Icons.Outlined.Lightbulb, Color(0xFFFFF3E0)),
        AiFeature("Goal Adjustment", Icons.Outlined.Adjust, Color(0xFFE3F2FD)),
        AiFeature("Motivation", Icons.Outlined.LocalFireDepartment, Color(0xFFFCE4EC))
    )
    val sampleQuickActions = listOf(
        "Analyze my progress",
        "Suggest today’s workout",
        "Review my diet",
        "Predict my results"
    )

    AICoachScreenContent(
        messages = sampleMessages,
        isTyping = true,
        quickActions = sampleQuickActions,
        features = sampleFeatures
    )
}

@Composable
fun LabelBadge(text: String, bgColor: Color, textColor: Color) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

@Composable
fun TrainingDetailsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("👉 AI Training Details", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF1F2937))
            Spacer(modifier = Modifier.height(12.dp))
            
            TrainingDetailRow("Dataset File Name:", "dataset.json")
            TrainingDetailRow("Total Questions:", "692")
            TrainingDetailRow(
                "Categories:", 
                "• Workout\n• Diet\n• Food\n• Profile\n• Account"
            )
            TrainingDetailRow("Model Type:", "Custom Trained Retrieval Model")
            TrainingDetailRow("Training Method:", "Dataset-based question-answer matching using local processing")
            
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFFDCFCE7),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Model Trained Successfully", color = Color(0xFF166534), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TrainingDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label, 
            fontWeight = FontWeight.Bold, 
            fontSize = 13.sp, 
            color = Color.DarkGray,
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = value, 
            fontSize = 13.sp, 
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
    }
}
