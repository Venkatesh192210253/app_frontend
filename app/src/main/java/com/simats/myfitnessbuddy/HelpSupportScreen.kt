package com.simats.myfitnessbuddy

import kotlinx.coroutines.launch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simats.myfitnessbuddy.data.remote.RetrofitClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    appPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    onNavigateToGuide: () -> Unit = {},
    onNavigateToWebView: (String, String) -> Unit = { _, _ -> },
    viewModel: HelpSupportViewModel = viewModel<HelpSupportViewModel>()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()
    val backgroundColor = Color(0xFFF4F6FA)

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Help & Support", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("We’re here to help you", fontSize = 12.sp, color = Color.Gray)
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
        val context = LocalContext.current
        
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + appPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + appPadding.calculateBottomPadding() + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 0. Loading or Error State
            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00C896))
                    }
                }
            } else if (uiState.error != null) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(uiState.error!!, color = Color.Red, fontSize = 14.sp)
                        TextButton(onClick = { viewModel.fetchFaqs() }) {
                            Text("Retry", color = Color(0xFF4A6FFF))
                        }
                    }
                }
            }

            // 1. Contact Us Card
            item {
                SettingsCategoryCard(title = "Contact Us") {
                    Column {
                        ContactRow(Icons.Default.Description, "Getting Started Guide", "Learn how the app works") {
                            onNavigateToGuide()
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        ContactRow(Icons.Default.QuestionAnswer, "FAQs", "Frequently asked questions") {
                            scope.launch {
                                scrollState.animateScrollToItem(2) // 2 is the FAQ item index
                            }
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        ContactRow(Icons.Default.Chat, "Live Chat", "Chat with our support team") {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/917674992476?text=Hello%20Support!%20I%20need%20help%20with%20MyFitnessBuddy"))
                            context.startActivity(intent)
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        ContactRow(Icons.Default.Email, "Email Support", "support@myfitnessbuddy.com") {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:support@myfitnessbuddy.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Support Request")
                            }
                            context.startActivity(intent)
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        ContactRow(Icons.Default.Phone, "Call Us", "+91-7674992476") {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+917674992476"))
                            context.startActivity(intent)
                        }
                    }
                }
            }

            // 2. Frequently Asked Questions Card
            item {
                SettingsCategoryCard(title = "Frequently Asked Questions") {
                    Column {
                        if (uiState.faqs.isEmpty() && !uiState.isLoading) {
                            Text("No FAQs available.", modifier = Modifier.padding(16.dp), color = Color.Gray)
                        } else {
                            uiState.faqs.forEachIndexed { index, faq ->
                                FaqItemRow(faq) { viewModel.toggleFaq(faq.id) }
                                if (index < uiState.faqs.size - 1) {
                                    Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            }

            // 3. App Information Card
            item {
                SettingsCategoryCard(title = "App Information") {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoRow("Version", "1.0.0")
                        InfoRow("Build", "2026.02.14")
                        InfoRow("Developer", "My Fitness Buddy")
                    }
                }
            }

            // 4. Resources Card
            item {
                SettingsCategoryCard(title = "Resources") {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { 
                                val url = "${RetrofitClient.BASE_URL}api/support/terms/"
                                onNavigateToWebView("Terms of Service", url)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
                        ) {
                            Text("Terms of Service", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { 
                                val url = "${RetrofitClient.BASE_URL}api/support/privacy/"
                                onNavigateToWebView("Privacy Policy", url)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
                        ) {
                            Text("Privacy Policy", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 5. Bottom Button
            item {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(4.dp, CircleShape),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C896))
                ) {
                    Text("Back to Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun ContactRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color(0xFF4A6FFF).copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF4A6FFF), modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.DarkGray)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun FaqItemRow(faq: FaqItem, onToggle: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(faq.question, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.rotate(if (faq.isExpanded) 180f else 0f),
                tint = Color.Gray
            )
        }
        AnimatedVisibility(
            visible = faq.isExpanded,
            enter = expandVertically(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300))
        ) {
            Text(
                faq.answer,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                fontSize = 13.sp,
                color = Color.Gray,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
    }
}
