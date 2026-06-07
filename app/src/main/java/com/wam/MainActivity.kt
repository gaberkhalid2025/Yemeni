package com.wam

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wam.data.ActivityLog
import com.wam.data.AppSettings
import com.wam.data.Category
import com.wam.data.ChatMessage
import com.wam.data.FAQItem
import com.wam.data.Moderator
import com.wam.data.ServiceProvider
import com.wam.ui.AppViewModel
import com.wam.ui.theme.YemenServicesTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appSettings by viewModel.settings.collectAsState()
            val layoutDirection = if (viewModel.currentLanguage.collectAsState().value == "ar") {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            // Real-time primary branding synced synchronously with Firestore!
            val primaryThemeColor = remember(appSettings.primaryColor) {
                parsePrimaryColor(appSettings.primaryColor)
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                YemenServicesTheme(primaryColor = primaryThemeColor) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (appSettings.isMaintenanceMode && viewModel.adminSession.collectAsState().value == null) {
                            MaintenanceScreen(
                                message = appSettings.maintenanceMessage,
                                viewModel = viewModel
                            )
                        } else {
                            MainNavigationHost(viewModel = viewModel, primaryColor = primaryThemeColor)
                        }
                    }
                }
            }
        }
    }
}

// Helper to translate text inputs or settings configured values into precise Material Compose Colors
fun parsePrimaryColor(colorStr: String): Color {
    return try {
        if (colorStr.trim().startsWith("#")) {
            Color(android.graphics.Color.parseColor(colorStr))
        } else {
            when (colorStr.lowercase().trim()) {
                "cosmic silver" -> Color(0xFFC0C0C0)
                "luxury gold" -> Color(0xFFFFDF00)
                "yemen red" -> Color(0xFFCE1126)
                "navy accent" -> Color(0xFF1E3A8A)
                "emerald green" -> Color(0xFF10B981)
                else -> Color(0xFFFFDF00) // Luxury gold default
            }
        }
    } catch (e: Exception) {
        Color(0xFFFFDF00)
    }
}

// Dynamic Action to make telephone calls
fun initCallIntent(context: Context, phoneNumber: String) {
    try {
        val u = Uri.parse("tel:" + phoneNumber.trim())
        val intent = Intent(Intent.ACTION_DIAL, u)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "لا يمكن إجراء الاتصال بهذا الرقم", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MaintenanceScreen(message: String, viewModel: AppViewModel) {
    var backdoorInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isAttempting by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121214))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Maintenance Icon",
                tint = Color(0xFFFFDF00),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "التطبيق قيد الصيانة",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                color = Color.LightGray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Backdoor entry for emergencies
            OutlinedTextField(
                value = backdoorInput,
                onValueChange = { backdoorInput = it },
                label = { Text("رمز المرور السري للأدمن", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFFDF00),
                    unfocusedBorderColor = Color.DarkGray
                ),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(0.8f).testTag("maintenance_backdoor_field")
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    isAttempting = true
                    val success = viewModel.attemptLogin("Owner", backdoorInput, true)
                    isAttempting = false
                    if (!success) {
                        backdoorInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDF00)),
                modifier = Modifier.fillMaxWidth(0.8f).testTag("maintenance_login_btn")
            ) {
                Text("تخطي الصيانة والتحقق", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MainNavigationHost(viewModel: AppViewModel, primaryColor: Color) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController, viewModel, primaryColor)
        }
        composable("login") {
            LoginScreen(navController, viewModel, primaryColor)
        }
        composable("admin_dashboard") {
            AdminDashboardScreen(navController, viewModel, primaryColor)
        }
        composable("about") {
            AboutScreen(navController, viewModel, primaryColor)
        }
    }
}

@Composable
fun HomeScreen(navController: NavController, viewModel: AppViewModel, primaryColor: Color) {
    val context = LocalContext.current
    val currentLang by viewModel.currentLanguage.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val filteredProviders by viewModel.filteredProviders.collectAsState()
    val adminSession by viewModel.adminSession.collectAsState()

    var activeSupportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Logo",
                            tint = primaryColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = settings.appName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("app_title_home")
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick Language Switch
                        TextButton(
                            onClick = {
                                if (currentLang == "ar") viewModel.setLanguage("en") else viewModel.setLanguage("ar")
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = primaryColor),
                            modifier = Modifier.testTag("home_lang_toggle_btn")
                        ) {
                            Text(if (currentLang == "ar") "EN" else "عربي", fontWeight = FontWeight.Bold)
                        }

                        // Admin Console Shortcut Node
                        IconButton(
                            onClick = {
                                if (adminSession != null) {
                                    navController.navigate("admin_dashboard")
                                } else {
                                    navController.navigate("login")
                                }
                            },
                            modifier = Modifier.testTag("home_admin_btn")
                        ) {
                            Icon(
                                imageVector = if (adminSession != null) Icons.Default.Lock else Icons.Default.Person,
                                contentDescription = "Security Panel",
                                tint = if (adminSession != null) primaryColor else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        bottomBar = {
            FooterWidget(navController, viewModel, primaryColor)
        },
        floatingActionButton = {
            // Configurable floating chat widget
            if (!settings.chatButtonHidden) {
                val sIconGlow = settings.assistantIconGlow || settings.liveChatIconGlow
                val badgeShape = CircleShape

                FloatingActionButton(
                    onClick = { activeSupportDialog = true },
                    containerColor = primaryColor,
                    contentColor = if ((primaryColor.red * 0.299f + primaryColor.green * 0.587f + primaryColor.blue * 0.114f) > 0.5f) Color.Black else Color.White,
                    shape = badgeShape,
                    modifier = Modifier
                        .size(settings.chatButtonSize.dp)
                        .testTag("floating_assistant_shortcut_btn")
                        .border(if (sIconGlow) 2.dp else 0.dp, Color.White, badgeShape)
                ) {
                    Icon(
                        imageVector = if (settings.assistantIconSymbol == "Face") Icons.Default.Face else Icons.Default.MailOutline,
                        contentDescription = "AI Assistant Help"
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Welcome Section
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(primaryColor.copy(alpha = 0.1f))
                        .border(0.5.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = if (currentLang == "ar") "دليل الخدمات الفورية" else "Direct Yemeni Service Directory",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = settings.welcomeMessage,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Search Bar Widget
            item {
                val q by viewModel.searchQuery.collectAsState()
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    OutlinedTextField(
                        value = q,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        label = { Text(if (currentLang == "ar") "ابحث عن مهندس، تخصص، منطقة هاتف..." else "Search engineer, area, phone...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Looking") },
                        trailingIcon = {
                            if (q.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Erase")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_search_input")
                    )
                }
            }

            // Categories list (Firestore snapshot listener backed!) Let's make it beautiful horizontally
            item {
                val selectedCat by viewModel.selectedCategory.collectAsState()
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = if (currentLang == "ar") "اختر تخصص الخدمة" else "Select Specialization",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCat == null,
                                onClick = { viewModel.selectCategory(null) },
                                label = { Text(if (currentLang == "ar") "الكل 🌐" else "All") }
                            )
                        }
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCat == cat.id,
                                onClick = { viewModel.selectCategory(cat.id) },
                                label = { Text(text = "${cat.iconHex} ${if (currentLang == "ar") cat.nameAr else cat.nameEn}") }
                            )
                        }
                    }
                }
            }

            // Recommended (Pinned / Subscribed) Providers in horizontal carousel
            val pinnedList = filteredProviders.filter { it.isPinned || it.isSubscribed }
            if (pinnedList.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = if (currentLang == "ar") "⭐ الأخصائيين الموثقين والمقترحين" else "Recommended Specialists",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(pinnedList) { provider ->
                                RecommendedProviderCard(provider, primaryColor) {
                                    initCallIntent(context, provider.phone)
                                }
                            }
                        }
                    }
                }
            }

            // Main directory providers list header
            item {
                Text(
                    text = if (currentLang == "ar") "🔍 نتائج البحث والدليل المباشر" else "Direct Directory Records",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Empty state check
            if (filteredProviders.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Empty", tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (currentLang == "ar") "لا توجد نتائج مطابقة، جرب تصفح أقسام أخرى" else "No providers matching criteria.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredProviders) { provider ->
                    ServiceProviderRowCard(provider, primaryColor, currentLang, viewModel) {
                        initCallIntent(context, provider.phone)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    // Interactive Dialog supporting real-time messaging with live chats + Gemini AI
    if (activeSupportDialog) {
        SupportChatDialog(
            viewModel = viewModel,
            primaryColor = primaryColor,
            currentLang = currentLang,
            onClose = { activeSupportDialog = false }
        )
    }
}

// ---------------- UI components ----------------

@Composable
fun RecommendedProviderCard(
    provider: ServiceProvider,
    primaryColor: Color,
    onCall: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(180.dp)
            .border(1.dp, primaryColor, RoundedCornerShape(12.dp))
            .clickable { onCall() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(primaryColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("👤", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = provider.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = "📍 ${provider.residenceArea}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFFFDF00), modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(3.dp))
                val avg = if (provider.ratingCount > 0) provider.ratingSum.toFloat() / provider.ratingCount else 5.0f
                Text(text = String.format("%.1f", avg), color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onCall,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text("اتصل الآن 📱", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun ServiceProviderRowCard(
    provider: ServiceProvider,
    primaryColor: Color,
    currentLang: String,
    viewModel: AppViewModel,
    onCall: () -> Unit
) {
    var yourRatingStars by remember { mutableStateOf(0) }
    var userVotedByStar by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = provider.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (provider.isVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified Badge",
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📱 ${if (currentLang == "ar") "هاتف" else "Phone"}: ${provider.phone}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = onCall,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text(if (currentLang == "ar") "اتصل 📞" else "Call", color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📍 ${provider.residenceArea} | ${provider.workAddress}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = "Star", tint = Color(0xFFFFDF00), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    val average = if (provider.ratingCount > 0) provider.ratingSum.toFloat() / provider.ratingCount else 5.0f
                    Text(
                        text = "${String.format("%.1f", average)} (${provider.ratingCount} ${if (currentLang == "ar") "تقييم" else "votes"})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-Card rating component interactive
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (userVotedByStar) (if (currentLang == "ar") "نشكر تصويتك! ❤️" else "Rated!") else (if (currentLang == "ar") "قيم عمل المهندس:" else "Rate expert:"),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    for (star in 1..5) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star $star",
                            tint = if (star <= yourRatingStars) Color(0xFFFFDF00) else Color.Gray,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    if (!userVotedByStar) {
                                        yourRatingStars = star
                                        userVotedByStar = true
                                        viewModel.rateProvider(provider.id, star)
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FooterWidget(navController: NavController, viewModel: AppViewModel, primaryColor: Color) {
    val settings by viewModel.settings.collectAsState()
    val footerText = settings.footerText
    val opacity = settings.footerOpacity
    val fHeight = settings.footerHeightScale
    val fFontSize = settings.footerFontSize

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = opacity))
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = opacity))
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(fHeight.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "V2.6.2026",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = opacity),
                fontSize = fFontSize.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = footerText,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = opacity),
                fontSize = fFontSize.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("footer_center_text")
            )

            IconButton(
                onClick = { navController.navigate("about") },
                modifier = Modifier.testTag("about_app_footer_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "About Creator",
                    tint = primaryColor.copy(alpha = opacity)
                )
            }
        }
    }
}

@Composable
fun SupportChatDialog(
    viewModel: AppViewModel,
    primaryColor: Color,
    currentLang: String,
    onClose: () -> Unit
) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isAiGenerating by viewModel.isAiGenerating.collectAsState()
    var inputStr by remember { mutableStateOf("") }
    var isToAssistant by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentLang == "ar") "الدعم المباشر ومستشار الذكاء الاصطناعي" else "Interactive AI Advisor",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Exit")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxHeight(0.6f)) {
                // Selector switch: Support vs Gemini
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val activeBg = primaryColor
                    val activeFg = if ((primaryColor.red * 0.299f + primaryColor.green * 0.587f + primaryColor.blue * 0.114f) > 0.5f) Color.Black else Color.White

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isToAssistant) activeBg else Color.Transparent)
                            .clickable { isToAssistant = true }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentLang == "ar") "المساعد الذكي (Gemini)" else "Gemini AI",
                            fontWeight = FontWeight.Bold,
                            color = if (isToAssistant) activeFg else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isToAssistant) activeBg else Color.Transparent)
                            .clickable { isToAssistant = false }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentLang == "ar") "غرفة المحادثة العامة" else "Global Chat Board",
                            fontWeight = FontWeight.Bold,
                            color = if (!isToAssistant) activeFg else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp
                        )
                    }
                }

                if (settings.isChatServiceDisabled && !isToAssistant) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = settings.chatServiceDisabledReason,
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        reverseLayout = true
                    ) {
                        // Display messages matching current mode
                        val filteredList = chatMessages.filter { it.isToAssistant == isToAssistant }.reversed()
                        items(filteredList) { msg ->
                            val isMe = msg.sender == (viewModel.adminSession.value ?: "Owner")
                            val isRobot = msg.sender.contains("Gemini") || msg.sender.contains("المساعد")

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMe) primaryColor.copy(alpha = 0.2f)
                                        else if (isRobot) primaryColor.copy(alpha = 0.1f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = msg.sender,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = primaryColor
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = msg.message, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (isAiGenerating && isToAssistant) {
                        LinearProgressIndicator(
                            color = primaryColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                        )
                        Text(
                            text = if (currentLang == "ar") "جاري إعداد المشورة الذكية بلهجة يمانية أصيلة..." else "Consulting Gemini AI...",
                            fontSize = 10.sp,
                            color = primaryColor,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!(settings.isChatServiceDisabled && !isToAssistant)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputStr,
                        onValueChange = { inputStr = it },
                        placeholder = { Text(if (currentLang == "ar") "اكتب رسالتك..." else "Type message...") },
                        modifier = Modifier.weight(1f).testTag("chat_input_textfield")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (inputStr.trim().isNotEmpty()) {
                                viewModel.sendChatMessage(inputStr, isToAssistant)
                                inputStr = ""
                            }
                        },
                        modifier = Modifier.testTag("chat_send_message_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = primaryColor)
                    }
                }
            }
        }
    )
}

@Composable
fun LoginScreen(navController: NavController, viewModel: AppViewModel, primaryColor: Color) {
    val currentLang by viewModel.currentLanguage.collectAsState()
    var userStr by remember { mutableStateOf("") }
    var passStr by remember { mutableStateOf("") }
    var isRememberMe by remember { mutableStateOf(true) }
    var errorTxt by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Security Access", tint = primaryColor, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (currentLang == "ar") "البلوج والتحقق الأمني - لوحة التحكم" else "Admin Authentication",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = userStr,
                    onValueChange = { userStr = it },
                    label = { Text(if (currentLang == "ar") "اسم المستخدم" else "Username") },
                    modifier = Modifier.fillMaxWidth().testTag("login_user_input")
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = passStr,
                    onValueChange = { passStr = it },
                    label = { Text(if (currentLang == "ar") "رمز المرور (Password)" else "Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("login_password_input")
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isRememberMe, onCheckedChange = { isRememberMe = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (currentLang == "ar") "حفظ جلستي على هذا الجهاز" else "Keep me signed in", fontSize = 12.sp)
                }

                if (errorTxt.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = errorTxt, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (currentLang == "ar") "تراجع" else "Back")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val ok = viewModel.attemptLogin(userStr, passStr, isRememberMe)
                            if (ok) {
                                navController.navigate("admin_dashboard") {
                                    popUpTo("home")
                                }
                            } else {
                                errorTxt = if (currentLang == "ar") "بيانات التحقق غير مطابقة، يرجى إعادة المحاولة" else "Invalid credentials, retry again"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.weight(1.5f).testTag("login_submit_btn")
                    ) {
                        Text(if (currentLang == "ar") "ولوج للنظام" else "Sign In", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDashboardScreen(navController: NavController, viewModel: AppViewModel, primaryColor: Color) {
    val currentLang by viewModel.currentLanguage.collectAsState()
    val adminSession by viewModel.adminSession.collectAsState()
    val permissions = viewModel.getActiveUserPermissions()
    var activeTab by remember { mutableStateOf(0) }

    if (adminSession == null) {
        navController.navigate("home")
        return
    }

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (currentLang == "ar") "مسار الإدارة والامتثال" else "Admin Dashboard Mode",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = primaryColor
                        )
                        Text(
                            text = "${if (currentLang == "ar") "أهلاً بك" else "Logged in"}: ${adminSession} [${permissions.role}]",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }

                    Row {
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Exit Node", tint = Color.Red)
                        }
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.Home, contentDescription = "Home Node", tint = primaryColor)
                        }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Tab Switcher Row for Admin tabs
                ScrollableTabRow(
                    selectedTabIndex = activeTab,
                    edgePadding = 16.dp,
                    contentColor = primaryColor,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                        Text(text = if (currentLang == "ar") "مقدمو الخدمات" else "Providers", modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
                    }
                    if (permissions.canEditCategories) {
                        Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                            Text(text = if (currentLang == "ar") "الأقسام" else "Categories", modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (permissions.role == "owner") {
                        Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                            Text(text = if (currentLang == "ar") "المشرفين" else "Moderators", modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (permissions.canManageSettings) {
                        Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) {
                            Text(text = if (currentLang == "ar") "تخصيص الهوية" else "Identity Settings", modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                    Tab(selected = activeTab == 4, onClick = { activeTab = 4 }) {
                        Text(text = if (currentLang == "ar") "الأوديت والمحادثات" else "Audit & Logs", modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                0 -> ProvidersManagementTab(viewModel, currentLang, permissions, primaryColor)
                1 -> if (permissions.canEditCategories) CategoriesManagementTab(viewModel, currentLang, primaryColor)
                2 -> if (permissions.role == "owner") ModeratorsManagementTab(viewModel, currentLang, primaryColor)
                3 -> if (permissions.canManageSettings) DynamicConfigBrandingTab(viewModel, currentLang, primaryColor)
                4 -> LogAuditTab(viewModel, currentLang, primaryColor)
            }
        }
    }
}

// --- Tab 0: Providers Management Panel ---
@Composable
fun ProvidersManagementTab(viewModel: AppViewModel, currentLang: String, perms: Moderator, primaryColor: Color) {
    val providers by viewModel.allProviders.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showEditor by remember { mutableStateOf(false) }
    var selectedProvider by remember { mutableStateOf<ServiceProvider?>(null) }

    // Forms states
    var pId by remember { mutableStateOf("") }
    var pName by remember { mutableStateOf("") }
    var pPhone by remember { mutableStateOf("") }
    var pCatId by remember { mutableStateOf("") }
    var pResArea by remember { mutableStateOf("") }
    var pWorkAddr by remember { mutableStateOf("") }
    var pGender by remember { mutableStateOf("male") }
    var pVerified by remember { mutableStateOf(false) }
    var pPinned by remember { mutableStateOf(false) }
    var pSubscribed by remember { mutableStateOf(false) }
    var pStatus by remember { mutableStateOf("active") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = {
                    selectedProvider = null
                    pId = ""
                    pName = ""
                    pPhone = ""
                    pCatId = categories.firstOrNull()?.id ?: ""
                    pResArea = ""
                    pWorkAddr = ""
                    pGender = "male"
                    pVerified = false
                    pPinned = false
                    pSubscribed = false
                    pStatus = "active"
                    showEditor = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (currentLang == "ar") "➕ رقم تسويقي / إضافة مزود خدمة جديد" else "Add New Service Provider", color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        items(providers) { p ->
            val catName = categories.find { it.id == p.categoryId }?.let { if (currentLang == "ar") it.nameAr else it.nameEn } ?: "---"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = p.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = "📞 ${p.phone} | $catName", fontSize = 12.sp, color = Color.Gray)
                    Text(text = "📍 ${p.residenceArea} | ${p.workAddress}", fontSize = 11.sp, color = Color.LightGray)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Action control parameters
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { viewModel.toggleProviderStatus(p, "verified") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Verify", tint = if (p.isVerified) primaryColor else Color.Gray)
                            }
                            IconButton(
                                onClick = { viewModel.toggleProviderStatus(p, "pinned") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = "Pin", tint = if (p.isPinned) Color(0xFFFFDF00) else Color.Gray)
                            }
                            IconButton(
                                onClick = { viewModel.toggleProviderStatus(p, "subscribed") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = "Subscribed", tint = if (p.isSubscribed) Color.Red else Color.Gray)
                            }
                        }

                        // Mutation trigger buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    selectedProvider = p
                                    pId = p.id
                                    pName = p.name
                                    pPhone = p.phone
                                    pCatId = p.categoryId
                                    pResArea = p.residenceArea
                                    pWorkAddr = p.workAddress
                                    pGender = p.gender
                                    pVerified = p.isVerified
                                    pPinned = p.isPinned
                                    pSubscribed = p.isSubscribed
                                    pStatus = p.status
                                    showEditor = true
                                },
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(if (currentLang == "ar") "تعديل" else "Edit", fontSize = 11.sp)
                            }

                            if (perms.canDeleteProviders) {
                                Button(
                                    onClick = { viewModel.deleteProvider(p.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Text(if (currentLang == "ar") "حذف" else "Delete", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(text = if (selectedProvider == null) "إضافة مقدم خدمة" else "تعديل بيانات مقدم الخدمة", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        OutlinedTextField(value = pName, onValueChange = { pName = it }, label = { Text("الاسم الكامل") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = pPhone, onValueChange = { pPhone = it }, label = { Text("رقم الهاتف اليمني") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("القسم المهني المسؤول:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            categories.forEach { cat ->
                                Box(
                                    modifier = Modifier
                                        .background(if (pCatId == cat.id) primaryColor else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                        .clickable { pCatId = cat.id }
                                        .padding(8.dp)
                                ) {
                                    Text(text = cat.nameAr, fontSize = 10.sp, color = if (pCatId == cat.id) Color.Black else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(value = pResArea, onValueChange = { pResArea = it }, label = { Text("مديرية / منطقة الإقامة") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = pWorkAddr, onValueChange = { pWorkAddr = it }, label = { Text("عنوان العمل والورشة") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("الجنس المهني:")
                            Spacer(modifier = Modifier.width(12.dp))
                            RadioButton(selected = pGender == "male", onClick = { pGender = "male" })
                            Text("ذكر")
                            Spacer(modifier = Modifier.width(12.dp))
                            RadioButton(selected = pGender == "female", onClick = { pGender = "female" })
                            Text("أنثى")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addOrUpdateProvider(
                            id = if (pId.isEmpty()) java.util.UUID.randomUUID().toString() else pId,
                            name = pName,
                            phone = pPhone,
                            categoryId = pCatId,
                            residenceArea = pResArea,
                            workAddress = pWorkAddr,
                            gender = pGender,
                            isVerified = pVerified,
                            isPinned = pPinned,
                            isSubscribed = pSubscribed,
                            status = pStatus
                        )
                        showEditor = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("حفظ التغييرات", color = Color.Black)
                }
            }
        )
    }
}

// --- Tab 1: Categories Panel ---
@Composable
fun CategoriesManagementTab(viewModel: AppViewModel, currentLang: String, primaryColor: Color) {
    val categories by viewModel.categories.collectAsState()
    var catId by remember { mutableStateOf("") }
    var nameAr by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var iconEmoji by remember { mutableStateOf("🔧") }

    Column(modifier = Modifier.padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("إضافة / تحديث تصنيف مهني جديد", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = nameAr, onValueChange = { nameAr = it }, label = { Text("الاسم بالعربية") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("الاسم بالإنجليزية") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = iconEmoji, onValueChange = { iconEmoji = it }, label = { Text("رمز التعبير (Emoji)") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (nameAr.isNotEmpty() && nameEn.isNotEmpty()) {
                            viewModel.addOrUpdateCategory(
                                id = if (catId.isEmpty()) "cat_" + java.util.UUID.randomUUID().toString().take(6) else catId,
                                nameAr = nameAr,
                                nameEn = nameEn,
                                iconHex = iconEmoji
                            )
                            catId = ""
                            nameAr = ""
                            nameEn = ""
                            iconEmoji = "🔧"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("مزامنة القسم لـ Firestore", color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cat.iconHex, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(cat.nameAr, fontWeight = FontWeight.Bold)
                            Text(cat.nameEn, fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Row {
                        IconButton(onClick = {
                            catId = cat.id
                            nameAr = cat.nameAr
                            nameEn = cat.nameEn
                            iconEmoji = cat.iconHex
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Cat", tint = primaryColor)
                        }
                        IconButton(onClick = { viewModel.deleteCategory(cat.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Cat", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

// --- Tab 2: Moderators Control ---
@Composable
fun ModeratorsManagementTab(viewModel: AppViewModel, currentLang: String, primaryColor: Color) {
    val moderators by viewModel.moderators.collectAsState()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var canEditCategories by remember { mutableStateOf(true) }
    var canDeleteProviders by remember { mutableStateOf(false) }
    var canManageSettings by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("إضافة مشرف فريق جديد لوحة القيادة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("اسم المشرف") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("رمز المرور السري") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(8.dp))
                Text("الامتيازات والتراخيص اللامركزية:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = canEditCategories, onCheckedChange = { canEditCategories = it })
                    Text("تعديل الأقسام")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = canDeleteProviders, onCheckedChange = { canDeleteProviders = it })
                    Text("حذف مقدمي الخدمات")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = canManageSettings, onCheckedChange = { canManageSettings = it })
                    Text("التحكم بإعدادات الهوية")
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (username.isNotEmpty() && password.isNotEmpty()) {
                            val mod = Moderator(
                                username = username.trim(),
                                password = password,
                                role = "moderator",
                                canEditCategories = canEditCategories,
                                canDeleteProviders = canDeleteProviders,
                                canManageSettings = canManageSettings,
                                isActive = true
                            )
                            viewModel.addOrUpdateModerator(mod)
                            username = ""
                            password = ""
                            canEditCategories = true
                            canDeleteProviders = false
                            canManageSettings = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ترسيم المشرف الجديد", color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(moderators) { mod ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(mod.username, fontWeight = FontWeight.Bold)
                        Text("الرتبة: ${mod.role} | نشط: ${mod.isActive}", fontSize = 11.sp, color = Color.Gray)
                        Text("صلاحيات: تعديل أقسام (${mod.canEditCategories}) | حذف مقدمين (${mod.canDeleteProviders}) | تفعيل نظام (${mod.canManageSettings})", fontSize = 9.sp, color = Color.LightGray)
                    }

                    IconButton(onClick = { viewModel.deleteModerator(mod.username) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Mod", tint = Color.Red)
                    }
                }
            }
        }
    }
}

// --- Tab 3: Settings Panel (Identity Synced to Firestore!) ---
@Composable
fun DynamicConfigBrandingTab(viewModel: AppViewModel, currentLang: String, primaryColor: Color) {
    val settings by viewModel.settings.collectAsState()

    var dAppName by remember { mutableStateOf(settings.appName) }
    var dPrimaryColorDesc by remember { mutableStateOf(settings.primaryColor) }
    var dWelcomeMsg by remember { mutableStateOf(settings.welcomeMessage) }
    var dFooterText by remember { mutableStateOf(settings.footerText) }
    var dSupportPhone by remember { mutableStateOf(settings.supportPhone) }
    var dAdminPass by remember { mutableStateOf(settings.adminPassword) }
    var dMaintenance by remember { mutableStateOf(settings.isMaintenanceMode) }
    var dMaintenanceMsg by remember { mutableStateOf(settings.maintenanceMessage) }

    // Synchronize properties across updates
    LaunchedEffect(settings) {
        dAppName = settings.appName
        dPrimaryColorDesc = settings.primaryColor
        dWelcomeMsg = settings.welcomeMessage
        dFooterText = settings.footerText
        dSupportPhone = settings.supportPhone
        dAdminPass = settings.adminPassword
        dMaintenance = settings.isMaintenanceMode
        dMaintenanceMsg = settings.maintenanceMessage
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("تخصيص هوية التطبيق وتحديث Firestore", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primaryColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text("التغييرات المحفوظة ستلاحظ بشكل متبادل وعلى كل هواتف المستخدمين فوراً بفضل Snapshot Listener.", fontSize = 11.sp, color = Color.Gray)
        }

        item {
            OutlinedTextField(value = dAppName, onValueChange = { dAppName = it }, label = { Text("اسم التطبيق الرئيسي") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = dPrimaryColorDesc, onValueChange = { dPrimaryColorDesc = it }, label = { Text("اللون الأساسي (لوحة الغلاف أو كود Hex كـ #FFD700)") }, placeholder = { Text("Cosmic Silver, Luxury Gold, Yemen Red...") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = dWelcomeMsg, onValueChange = { dWelcomeMsg = it }, label = { Text("ترحيب التطبيق الرئيسي") }, maxLines = 2, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = dFooterText, onValueChange = { dFooterText = it }, label = { Text("نص الهامش السفلي (الفوتر)") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = dSupportPhone, onValueChange = { dSupportPhone = it }, label = { Text("رقم هاتف الدعم الرئيسي الدائم") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = dAdminPass, onValueChange = { dAdminPass = it }, label = { Text("رمز مرور البلوك السري للبرمجيات") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = dMaintenance, onCheckedChange = { dMaintenance = it })
                Text("تفعيل وضع الصيانة العام (إغلاق التطبيق)")
            }
        }

        if (dMaintenance) {
            item {
                OutlinedTextField(value = dMaintenanceMsg, onValueChange = { dMaintenanceMsg = it }, label = { Text("رسالة الصيانة للمستخدمين") }, modifier = Modifier.fillMaxWidth())
            }
        }

        item {
            Button(
                onClick = {
                    val updated = settings.copy(
                        appName = dAppName,
                        primaryColor = dPrimaryColorDesc,
                        welcomeMessage = dWelcomeMsg,
                        footerText = dFooterText,
                        supportPhone = dSupportPhone,
                        adminPassword = dAdminPass,
                        isMaintenanceMode = dMaintenance,
                        maintenanceMessage = dMaintenanceMsg
                    )
                    viewModel.saveSettings(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("حفظ وتحديث هوية الخادم الموحدة", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- Tab 4: System Audit Logs ---
@Composable
fun LogAuditTab(viewModel: AppViewModel, currentLang: String, primaryColor: Color) {
    val auditLogs by viewModel.logs.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("سجلات الأوديت والحركات الأمنية", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = primaryColor)

            Button(
                onClick = { viewModel.clearChat() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text("مسح غرفة المحادثات العامة", fontSize = 10.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(auditLogs.reversed()) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(text = log.username, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = primaryColor)
                            Text(text = log.actionType, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = log.details, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen(navController: NavController, viewModel: AppViewModel, primaryColor: Color) {
    val currentLang by viewModel.currentLanguage.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentLang == "ar") "عن التطبيق والدليل" else "About App",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = primaryColor
                )
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Go back", tint = primaryColor)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(primaryColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("⭐", fontSize = 40.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "دليل الخدمات الفورية اليمني الأول WAM",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "The Yemeni Premium Service Directory",
                fontSize = 13.sp,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "كود مرجعي المطور: MAW",
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "نسخة إصدار النظام: V2.6.2026",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "التطبيق يعتمد بالكامل على خوادم Google Firebase Firestore المستمرة والمزامنة الفورية عبر الشبكة اللامركزية.",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text(if (currentLang == "ar") "العودة للرئيسية" else "Back to Home", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
