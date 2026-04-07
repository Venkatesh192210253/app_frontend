package com.simats.myfitnessbuddy

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class QuickAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBackground: Brush,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState,
    onActionClick: (String) -> Unit
) {
    val foodActions = listOf(
        QuickAction(
            "Log Food",
            "Add meals manually",
            Icons.Default.Restaurant,
            Brush.linearGradient(listOf(Color(0xFF4A6FFF), Color(0xFF4A6FFF))),
            { onActionClick("Log Food") }
        ),
        QuickAction(
            "Barcode Scan",
            "Scan food barcode",
            Icons.Default.QrCodeScanner,
            Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFF9C6CFF))),
            { onActionClick("Barcode Scan") }
        ),
        QuickAction(
            "Voice Log",
            "Speak meal details",
            Icons.Default.Mic,
            Brush.linearGradient(listOf(Color(0xFF9C6CFF), Color(0xFF9C6CFF))),
            { onActionClick("Voice Log") }
        ),
        QuickAction(
            "AI Meal Scan",
            "Scan using camera",
            Brush.linearGradient(listOf(Color(0xFF00C896), Color(0xFF4A6FFF))),
            Icons.Default.AutoAwesome, // Placeholder for AI icon
            { onActionClick("AI Meal Scan") }
        )
    )

    val healthActions = listOf(
        QuickAction(
            "Add Water",
            "Track hydration",
            Icons.Default.LocalDrink,
            Brush.linearGradient(listOf(Color(0xFF4FC3F7), Color(0xFF29B6F6))),
            { onActionClick("Add Water") }
        ),
        QuickAction(
            "Update Weight",
            "Log current weight",
            Icons.Default.MonitorWeight,
            Brush.linearGradient(listOf(Color(0xFFFFA726), Color(0xFFFB8C00))),
            { onActionClick("Update Weight") }
        ),
        QuickAction(
            "Log Exercise",
            "Track workout",
            Icons.Default.FitnessCenter,
            Brush.linearGradient(listOf(Color(0xFF66BB6A), Color(0xFF43A047))),
            { onActionClick("Log Exercise") }
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.LightGray.copy(alpha = 0.5f), CircleShape)
            )
        },
        scrimColor = Color.Black.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Add",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(1), // Using 1 column to make cards horizontal as requested
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
            ) {
                items(foodActions) { action ->
                    QuickActionCard(action)
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Health Tracking",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(healthActions) { action ->
                    QuickActionCard(action)
                }
            }
        }
    }
}

// Custom constructor-like function for QuickAction to handle AI scan separately if needed
fun QuickAction(
    title: String,
    subtitle: String,
    brush: Brush,
    icon: ImageVector,
    onClick: () -> Unit
) = QuickAction(title, subtitle, icon, brush, onClick)

@Composable
fun QuickActionCard(action: QuickAction) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = action.onClick
            ),
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
                    .background(action.iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = action.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = action.subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
