package com.simats.myfitnessbuddy

import com.simats.myfitnessbuddy.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SubscriptionScreen(
    onSignInClick: () -> Unit,
    onSkipClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var selectedPlan by remember { mutableStateOf(1) } // 0: Monthly, 1: Yearly, 2: Lifetime

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
            .padding(top = 64.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Branding
        Icon(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "MyFitnessBuddy Logo",
            modifier = Modifier.size(80.dp),
            tint = Color.Unspecified
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "MyFitnessBuddy",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )
        
        Text(
            text = "PREMIUM",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryGreen,
            modifier = Modifier
                .background(PrimaryGreen.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Unlock Your Best Self",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Get exclusive features to reach your fitness goals faster and more effectively.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Premium Features
        PremiumFeatureItem(
            icon = Icons.Default.AutoGraph, 
            title = "AI Coaching", 
            description = "Personalized tips based on your activity."
        )
        PremiumFeatureItem(
            icon = Icons.Default.Restaurant, 
            title = "Premium Recipes", 
            description = "Access to 500+ nutrition-verified meals."
        )
        PremiumFeatureItem(
            icon = Icons.Default.Groups, 
            title = "Elite Challenges", 
            description = "Join exclusive high-stakes fitness groups."
        )
        PremiumFeatureItem(
            icon = Icons.Default.Block, 
            title = "Ad-Free Experience", 
            description = "Focus on your fitness without interruptions."
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Subscription Plans
        Text(
            text = "Choose Your Plan",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PlanCard(
            title = "Monthly",
            price = "₹799/mo",
            isSelected = selectedPlan == 0,
            onClick = { selectedPlan = 0 }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        PlanCard(
            title = "Yearly (Best Value)",
            price = "₹5,999/yr",
            subtitle = "Save 33% compared to monthly",
            isSelected = selectedPlan == 1,
            onClick = { selectedPlan = 1 }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        PlanCard(
            title = "Lifetime Access",
            price = "₹12,499",
            subtitle = "One-time payment, forever access",
            isSelected = selectedPlan == 2,
            onClick = { selectedPlan = 2 }
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Buttons
        ScaleButton(
            onClick = onSignInClick,
            modifier = Modifier.fillMaxWidth(),
            containerColor = AccentGreen
        ) {
            Text(
                "Sign in for Free", 
                color = Color.White, 
                fontWeight = FontWeight.Bold, 
                fontSize = 18.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Skip for Now",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray,
            modifier = Modifier
                .clickable { onSkipClick() }
                .padding(8.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Renewable subscription. Cancel anytime.",
            fontSize = 10.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PremiumFeatureItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(12.dp),
            color = PrimaryGreen.copy(alpha = 0.1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.padding(10.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            Text(text = description, fontSize = 13.sp, color = Color.Gray)
        }
    }
}

@Composable
fun PlanCard(
    title: String,
    price: String,
    subtitle: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PrimaryGreen else Color(0xFFEEEEEE)
    val backgroundColor = if (isSelected) PrimaryGreen.copy(alpha = 0.05f) else Color.White
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    text = price,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = if (isSelected) PrimaryGreen else Color.Black
                )
            }
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = if (isSelected) PrimaryGreen.copy(alpha = 0.8f) else Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
