package com.simats.myfitnessbuddy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
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
import com.simats.myfitnessbuddy.data.remote.ChallengeResponse

data class ChallengeModel(
    val id: String,
    val title: String,
    val description: String,
    val deadline: String,
    val participants: Int,
    val reward: String,
    val isJoined: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChallengesScreen(
    groupId: String,
    onBack: () -> Unit,
    onNavigateToParticipants: (String) -> Unit,
    appPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: GroupChallengesViewModel = viewModel()
) {
    val challenges by viewModel.challenges.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(groupId) {
        viewModel.loadChallenges(groupId)
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                TopAppBar(
                    title = { Text("Group Challenges", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF22C55E))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        },
        containerColor = Color(0xFFF4F6FA)
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF22C55E))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + appPadding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Available Challenges", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                }

                if (challenges.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No challenges found for this group.", color = Color.Gray)
                        }
                    }
                } else {
                    items(challenges) { challenge ->
                        ChallengeCard(
                            challenge = ChallengeModel(
                                id = challenge.id,
                                title = challenge.name,
                                description = challenge.description ?: "",
                                deadline = "${challenge.duration_days} days remaining",
                                participants = 0,
                                reward = "${challenge.points_reward} Points",
                                isJoined = false // This could be improved if backend sent join status
                            ),
                            onJoin = {
                                viewModel.joinChallenge(challenge.id) {
                                    onNavigateToParticipants(challenge.id)
                                }
                            },
                            onDelete = {
                                viewModel.deleteChallenge(challenge.id, groupId)
                            },
                            onViewStats = {
                                onNavigateToParticipants(challenge.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeCard(
    challenge: ChallengeModel,
    onJoin: () -> Unit,
    onDelete: () -> Unit,
    onViewStats: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(challenge.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (challenge.isJoined) {
                        Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(50.dp)) {
                            Text("Joined", color = Color(0xFF166534), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Delete Challenge", color = Color.Red) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View Stats") },
                                onClick = {
                                    showMenu = false
                                    onViewStats()
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(challenge.description, fontSize = 13.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(challenge.deadline, fontSize = 12.sp, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFACC15), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(challenge.reward, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                }
            }

            if (!challenge.isJoined) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onJoin,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Join Challenge", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
