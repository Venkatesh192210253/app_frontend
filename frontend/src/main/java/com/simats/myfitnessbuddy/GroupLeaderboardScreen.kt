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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupLeaderboardScreen(
    groupId: String,
    onBack: () -> Unit,
    appPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: GroupDetailViewModel = viewModel()
) {
    val members by viewModel.members.collectAsState()
    val groupData by viewModel.groupData.collectAsState()

    LaunchedEffect(groupId) {
        viewModel.loadGroupDetail(groupId)
    }

    if (groupData == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF22C55E))
        }
    } else {
        val data = groupData!!
        Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                TopAppBar(
                    title = { Text("Group Leaderboard", fontWeight = FontWeight.Bold) },
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
            // 1. Group Info Header
            item {
                LeaderboardHeader(data)
            }

            // 2. Top 3 Podium (Visual)
            item {
                TopThreePodium(members.take(3))
            }

            // 3. Leaderboard List
            item {
                Text("All Members", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937), modifier = Modifier.padding(vertical = 8.dp))
            }

            items(members) { member ->
                MemberRankRow(member)
            }
        }
    }
    }
}


@Composable
fun LeaderboardHeader(data: GroupDetailData) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(data.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.DarkGray)
            Text("Active Challenge: ${data.activeChallenge}", color = Color(0xFF22C55E), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun TopThreePodium(topMembers: List<MemberModel>) {
    Row(
        modifier = Modifier.fillMaxWidth().height(200.dp).padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place
        if (topMembers.size >= 2) {
            PodiumStep(topMembers[1], "#2", Color(0xFF94A3B8), 120.dp)
        }
        
        // 1st Place
        if (topMembers.isNotEmpty()) {
            PodiumStep(topMembers[0], "#1", Color(0xFFFFD700), 160.dp)
        }
        
        // 3rd Place
        if (topMembers.size >= 3) {
            PodiumStep(topMembers[2], "#3", Color(0xFFCD7F32), 100.dp)
        }
    }
}

@Composable
fun PodiumStep(member: MemberModel, rankLabel: String, color: Color, height: androidx.compose.ui.unit.Dp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(member.initials, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(member.name.split(" ").first(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.6f)))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(rankLabel, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text("${member.points} pts", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp)
            }
        }
    }
}
