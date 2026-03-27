package com.simats.myfitnessbuddy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecurityScreen(
    appPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit = {},
    viewModel: PrivacySecurityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backgroundColor = Color(0xFFF4F6FA)
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    if (showDeleteDialog) {
        var deletePassword by remember { mutableStateOf("") }
        var isDeleting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text("Delete Account?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This action is permanent and cannot be undone. All your workout data, nutrition logs, and profile info will be lost.")
                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = { deletePassword = it },
                        label = { Text("Confirm with Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        viewModel.deleteAccount(
                            password = deletePassword,
                            onSuccess = {
                                isDeleting = false
                                showDeleteDialog = false
                                android.widget.Toast.makeText(context, "Account deleted", android.widget.Toast.LENGTH_LONG).show()
                                onAccountDeleted()
                            },
                            onError = { errorMsg ->
                                isDeleting = false
                                android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444)),
                    enabled = deletePassword.isNotBlank() && !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color(0xFFEF4444))
                    } else {
                        Text("Delete Forever")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    if (showPasswordDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var isChanging by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { if (!isChanging) showPasswordDialog = false },
            title = { Text("Change Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your current password and a new one.")
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Old Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isChanging = true
                        viewModel.changePassword(
                            oldPass = oldPassword,
                            newPass = newPassword,
                            onSuccess = {
                                isChanging = false
                                showPasswordDialog = false
                                android.widget.Toast.makeText(context, "Password changed successfully", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onError = { errorMsg ->
                                isChanging = false
                                android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    enabled = oldPassword.isNotBlank() && newPassword.isNotBlank() && !isChanging
                ) {
                    if (isChanging) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }, enabled = !isChanging) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Privacy & Security", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Control your data", fontSize = 12.sp, color = Color.Gray)
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
            // 1. Account Privacy Card
            item {
                SettingsCategoryCard(title = "Account Privacy") {
                    Column {
                        SwitchRow(
                            title = "Private Account",
                            subtitle = "Only friends can see your activity",
                            checked = uiState.privateAccount,
                            onCheckedChange = { viewModel.updatePrivateAccount(it) }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        SwitchRow(
                            title = "Show Profile in Search",
                            subtitle = "Allow others to find you",
                            checked = uiState.showProfileInSearch,
                            onCheckedChange = { viewModel.updateShowProfileInSearch(it) }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        SwitchRow(
                            title = "Show Activity Status",
                            subtitle = "Let others see when you're active",
                            checked = uiState.showActivityStatus,
                            onCheckedChange = { viewModel.updateShowActivityStatus(it) }
                        )
                    }
                }
            }

            // 2. Data Sharing Card
            item {
                SettingsCategoryCard(title = "Data Sharing") {
                    Column {
                        SwitchRow(title = "Share Workout Data", subtitle = "Sync metrics with verified partners", checked = uiState.shareWorkoutData, onCheckedChange = { viewModel.updateShareWorkoutData(it) })
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        SwitchRow(title = "Share Diet Data", subtitle = "Allow nutrition analysis sharing", checked = uiState.shareDietData, onCheckedChange = { viewModel.updateShareDietData(it) })
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        SwitchRow(title = "Share Progress Photos", subtitle = "Visible to selected fitness groups", checked = uiState.shareProgressPhotos, onCheckedChange = { viewModel.updateShareProgressPhotos(it) })
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        SwitchRow(title = "Appear on Leaderboards", subtitle = "Compete with the community", checked = uiState.appearOnLeaderboards, onCheckedChange = { viewModel.updateAppearOnLeaderboards(it) })
                    }
                }
            }

            // 3. Security Card
    item {
        SettingsCategoryCard(title = "Security") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecurityItem(
                    title = "Change Password", 
                    subtitle = null, 
                    onClick = { showPasswordDialog = true }
                )

                SecurityItem("Connected Devices", "${uiState.connectedDevicesCount} devices")
            }
        }
    }

            // 4. Blocked Users Card
            item {
                SettingsCategoryCard(title = "Blocked Users") {
                    Text(
                        "You haven't blocked anyone yet.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // 5. Data Management Card
            item {
                SettingsCategoryCard(title = "Data Management") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { 
                                viewModel.downloadData(
                                    onSuccess = { msg -> android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show() },
                                    onError = { msg -> android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show() }
                                ) 
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
                        ) {
                            Text("Download My Data", fontWeight = FontWeight.SemiBold)
                        }
                        
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Text("Delete Account", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun SecurityItem(title: String, subtitle: String?, subtitleColor: Color = Color.Gray, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
        color = Color(0xFFF9FAFB)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = subtitleColor)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }
    }
}
