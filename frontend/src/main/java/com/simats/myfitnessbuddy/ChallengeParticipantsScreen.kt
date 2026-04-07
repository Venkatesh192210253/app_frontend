package com.simats.myfitnessbuddy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeParticipantsScreen(
    challengeId: String,
    challengeName: String,
    onBack: () -> Unit,
    appPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: ChallengeParticipantsViewModel = viewModel()
) {
    val participants by viewModel.participants.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(challengeId) {
        viewModel.loadParticipants(challengeId)
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Challenge Stats", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(challengeName, fontSize = 12.sp, color = Color.Gray)
                        }
                    },
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Participants & Stats", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                }

                if (participants.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No participants yet.", color = Color.Gray)
                        }
                    }
                } else {
                    items(participants) { participant ->
                        ParticipantStatRow(participant)
                    }
                }
            }
        }
    }
}

@Composable
fun ParticipantStatRow(participant: com.simats.myfitnessbuddy.data.remote.ChallengeParticipantResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    participant.initials,
                    color = Color(0xFF166534),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(participant.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1F2937))
                Text("@${participant.username}", fontSize = 12.sp, color = Color.Gray)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(participant.progress_value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF22C55E))
                if (participant.is_completed) {
                    Text("Completed", fontSize = 10.sp, color = Color(0xFF166534), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
