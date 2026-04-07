package com.simats.myfitnessbuddy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    appPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    viewModel: ProfileViewModel
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val backgroundColor = Color(0xFFF4F6FA)
    
    // Form state initialized with current profile
    var name by remember { mutableStateOf(userProfile.name) }
    var email by remember { mutableStateOf(userProfile.email) }
    var phone by remember { mutableStateOf(userProfile.phone) }
    var age by remember { mutableStateOf(userProfile.age) }
    var gender by remember { mutableStateOf(userProfile.gender) }
    var height by remember { mutableStateOf(userProfile.height) }
    var username by remember { mutableStateOf(userProfile.username) }
    var bio by remember { mutableStateOf(userProfile.bio) }
    var profilePictureUri by remember { mutableStateOf(userProfile.profilePictureUri) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { profilePictureUri = it.toString() }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Sync form state when userProfile is loaded
    LaunchedEffect(userProfile) {
        if (userProfile.name.isNotEmpty() || name.isEmpty()) name = userProfile.name
        if (userProfile.email.isNotEmpty() || email.isEmpty()) email = userProfile.email
        if (userProfile.phone.isNotEmpty() || phone.isEmpty()) phone = userProfile.phone
        if (userProfile.age.isNotEmpty() || age.isEmpty()) age = userProfile.age
        if (userProfile.gender.isNotEmpty() || gender.isEmpty()) gender = userProfile.gender
        if (userProfile.height.isNotEmpty() || height.isEmpty()) height = userProfile.height
        
        if (userProfile.username.isNotEmpty() || username.isEmpty()) username = userProfile.username
        if (userProfile.bio.isNotEmpty() || bio.isEmpty()) bio = userProfile.bio
        if (userProfile.profilePictureUri != null || profilePictureUri == null) profilePictureUri = userProfile.profilePictureUri
    }

    Scaffold(
        snackbarHost = { 
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = appPadding.calculateBottomPadding())
            ) 
        },
        topBar = {
            Surface(shadowElevation = 4.dp) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Update your information", fontSize = 12.sp, color = Color.Gray)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF4A6FFF))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(600)) + expandVertically(),
            modifier = Modifier.fillMaxSize()
        ) {
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
                // 1. Profile Header Section
                item {
                    ProfileHeaderEdit(
                        initials = userProfile.initials,
                        profilePictureUri = profilePictureUri,
                        onChangePhoto = { launcher.launch("image/*") }
                    )
                }

                // 2. Personal Information Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Personal Information", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                            
                            EditField("Full Name", name) { name = it }
                            EditField("Email", email) { email = it }
                            EditField("Phone Number", phone) { phone = it }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.weight(1f)) { EditField("Age", age) { age = it } }
                                Box(modifier = Modifier.weight(1f)) { EditField("Gender", gender) { gender = it } }
                            }
                        }
                    }
                }

                // 3. Body Metrics Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Body Metrics", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.fillMaxWidth()) { EditField("Height (cm)", height) { height = it } }
                            }
                        }
                    }
                }

                // 4. Social Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Social", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                            
                            EditField("Username", username) { username = it }
                            EditField("Bio", bio, singleLine = false) { bio = it }
                        }
                    }
                }

                item {
                    val isLoading by viewModel.isLoading.collectAsState()
                    LoadingButton(
                        text = "Save Changes",
                        isLoading = isLoading,
                        onClick = {
                            val updated = userProfile.copy(
                                name = name,
                                email = email,
                                phone = phone,
                                age = age,
                                gender = gender,
                                height = height,
                                username = username,
                                bio = bio,
                                initials = if (name.isNotEmpty()) name.take(1).uppercase() + (if (name.contains(" ")) name.split(" ").last().take(1).uppercase() else "") else "U",
                                profilePictureUri = profilePictureUri
                            )
                            viewModel.updateProfileWithImage(context, updated)
                            scope.launch {
                                snackbarHostState.showSnackbar("Profile updated successfully!")
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().shadow(4.dp, CircleShape),
                        containerColor = Color(0xFF00C896),
                        shape = CircleShape
                    )
                }
                
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun ProfileHeaderEdit(
    initials: String,
    profilePictureUri: String?,
    onChangePhoto: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFBBF7D0)),
                contentAlignment = Alignment.Center
            ) {
                if (!profilePictureUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = profilePictureUri,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(initials, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C896))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onChangePhoto,
                shape = RoundedCornerShape(50.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Photo", fontSize = 14.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditField(
    label: String, 
    value: String, 
    placeholder: String = "",
    singleLine: Boolean = true, 
    onValueChange: (String) -> Unit
) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder, color = Color.LightGray) } } else null,
            shape = RoundedCornerShape(12.dp),
            singleLine = singleLine,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color(0xFFF9FAFB),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color(0xFF00C896).copy(alpha = 0.5f)
            )
        )
    }
}
