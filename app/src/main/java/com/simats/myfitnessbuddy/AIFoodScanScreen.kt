package com.simats.myfitnessbuddy

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun AIFoodScanScreen(
    onBack: () -> Unit,
    appPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: DiaryViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var detectedFoodResponse by remember { mutableStateOf<com.simats.myfitnessbuddy.data.remote.FoodScanResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color.Black
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            if (hasCameraPermission && capturedImageUri == null) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onImageCaptured = { uri ->
                        capturedImageUri = uri
                        isAnalyzing = true
                        errorMessage = null
                        
                        // Capture logic: Read bytes from URI
                        val imageBytes = try {
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        } catch (e: Exception) {
                            null
                        }

                        if (imageBytes != null) {
                            viewModel.scanFoodImage(
                                imageBytes = imageBytes, 
                                onSuccess = { response ->
                                    isAnalyzing = false
                                    detectedFoodResponse = response
                                },
                                onError = { error ->
                                    isAnalyzing = false
                                    errorMessage = error
                                }
                            )
                        } else {
                            isAnalyzing = false
                            errorMessage = "Could not read captured image"
                        }
                    }
                )

                // Overlay UI
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }

                Text(
                    "Center food in frame",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    fontSize = 14.sp
                )
            } else if (!hasCameraPermission) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Camera permission required", color = Color.White)
                }
            }

            // Results UI
            AnimatedVisibility(
                visible = detectedFoodResponse != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                detectedFoodResponse?.let { food ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4A6FFF).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF4A6FFF))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Detected Food (${food.meal_type})", fontSize = 12.sp, color = Color.Gray)
                                    Text(food.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.DarkGray)
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            var selectedMealType by remember { mutableStateOf(food.meal_type) }
                            Text("Log to:", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { type ->
                                    val isSelected = selectedMealType.equals(type, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedMealType = type },
                                        label = { Text(type, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF4A6FFF).copy(alpha = 0.1f),
                                            selectedLabelColor = Color(0xFF4A6FFF)
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isSelected) Color(0xFF4A6FFF) else Color.LightGray
                                        )
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                AiScanMetric("Calories", "${food.calories} kcal")
                                AiScanMetric("Macros", "P: ${food.protein}g | C: ${food.carbs}g | F: ${food.fat}g")
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { detectedFoodResponse = null; capturedImageUri = null },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Retake")
                                }
                                Button(
                                    onClick = { 
                                        with(viewModel) {
                                            addFoodToMeal(selectedMealType, food.toFoodEntry())
                                        }
                                        onBack() 
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6FFF))
                                ) {
                                    Text("Confirm & Add")
                                }
                            }
                        }
                    }
                }
            }
            
            if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.8f))) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(errorMessage!!, color = Color.White)
                            Button(onClick = { errorMessage = null; capturedImageUri = null }) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            }

            if (isAnalyzing) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("AI Analyzing Meal...", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}


@Composable
fun AiScanMetric(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.DarkGray)
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onImageCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    val mainExecutor = ContextCompat.getMainExecutor(context)

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraX", "Binding failed", e)
            }
        }, mainExecutor)
    }

    Box(modifier = modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        
        // Capture Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.3f))
                .padding(8.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable {
                    val photoFile = java.io.File(
                        context.cacheDir,
                        "scan_${System.currentTimeMillis()}.jpg"
                    )
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        mainExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                onImageCaptured(Uri.fromFile(photoFile))
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("CameraX", "Capture failed: ${exception.message}", exception)
                            }
                        }
                    )
                }
        )
        
        // Gallery Button
        IconButton(
            onClick = { /* Open Gallery placeholder */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 60.dp, end = 48.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
        }
    }
}
