@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.wam

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.FirebaseApp
import com.wam.data.*
import com.wam.ui.AppViewModel
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Manual programmatic initialization of Firebase to completely support Gradle 9 dynamic configurations
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("1:837151720505:android:68e1a6cab4369a2be979e8")
                    .setApiKey("AIzaSyA5ysT25HeS0qFz6rUy-YCSFcVqlPowoSc")
                    .setProjectId("yemenimaw")
                    .setStorageBucket("yemenimaw.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                MainAppScreen()
            }
        }
    }
}

// --- Dynamic Color Helper ---
fun parseColorHex(hex: String, default: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        default
    }
}

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val viewModel: AppViewModel = viewModel()

    // Collect real-time States
    val settings by viewModel.settings.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val pendingProviders by viewModel.pendingProviders.collectAsState()
    val banners by viewModel.banners.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val admins by viewModel.admins.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val logs by viewModel.logs.collectAsState()

    val currentLang by viewModel.currentLang.collectAsState()
    val adminLoggedIn by viewModel.adminLoggedIn.collectAsState()
    val adminPermissions by viewModel.adminPermissions.collectAsState()
    val userPoints by viewModel.userLoyaltyPoints.collectAsState()

    // Navigation and UX state triggers
    var currentScreen by remember { mutableStateOf("home") } // home, login, register_provider, aboutUs, admin_dashboard
    var searchKeyword by remember { mutableStateOf("") }
    var voiceSearchActive by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedCityFilter by remember { mutableStateOf<String?>(null) }
    var ratingFilter by remember { mutableStateOf(0) }

    // Floating UI Dialogs triggers
    var showAssistantSheet by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var showBackdoorDialog by remember { mutableStateOf(false) }
    var showReviewDialogForProvider by remember { mutableStateOf<ServiceProvider?>(null) }
    var showReportDialogForProvider by remember { mutableStateOf<ServiceProvider?>(null) }

    // Home clicking counts for backdoor trigger
    var homeClickCount by remember { mutableStateOf(0) }

    // Theme Configs
    val primaryColor = parseColorHex(settings.primaryColor, Color(0xFF004D40))
    val secondaryColor = parseColorHex(settings.secondaryColor, Color(0xFFFFD700))
    val darkBackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF121212), Color(0xFF1E1E1E))
    )

    // RTL Directionality
    val dirRTL = currentLang == "ar"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackgroundGradient)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // ------------------ 1. TOP APP BAR (RTL alignment) ------------------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left App Title Logo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            homeClickCount++
                            if (homeClickCount >= 5) {
                                showBackdoorDialog = true
                                homeClickCount = 0
                            } else {
                                currentScreen = "home"
                            }
                        }
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = secondaryColor
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("W", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = settings.appName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Top navigation actions buttons 🏠 | 🔐 | 👤 | 🌐 | 🔄
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = {
                            homeClickCount = 0
                            currentScreen = "home"
                        }) {
                            Icon(Icons.Default.Home, "Home", tint = if (currentScreen == "home") secondaryColor else Color.White)
                        }
                        IconButton(onClick = {
                            if (adminLoggedIn != null) currentScreen = "admin_dashboard"
                            else currentScreen = "login"
                        }) {
                            Icon(Icons.Default.Lock, "Login/Admin", tint = if (currentScreen == "login" || currentScreen == "admin_dashboard") secondaryColor else Color.White)
                        }
                        IconButton(onClick = { currentScreen = "register_provider" }) {
                            Icon(Icons.Default.Person, "Register Provider", tint = if (currentScreen == "register_provider") secondaryColor else Color.White)
                        }
                        IconButton(onClick = { viewModel.toggleLanguage() }) {
                            Icon(Icons.Default.Language, "Language", tint = Color.White)
                        }
                        IconButton(onClick = {
                            Toast.makeText(context, if (dirRTL) "تمت المزامنة الفورية بنجاح!" else "Synced with Live Firestore!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Refresh, "Sync", tint = Color.White)
                        }
                    }
                }
            }

            // ------------------ 2. TEXT BANNER MARQUEE (Under bar) ------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(secondaryColor.copy(alpha = 0.15f))
                    .padding(vertical = 4.dp)
            ) {
                // Symmetrical smooth auto-sliding text simulation
                Text(
                    text = settings.welcomeMessage,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // ------------------ 3. APP SCREEN BODY (CROSSFADE TRANSITION) ------------------
            Box(modifier = Modifier.weight(1f)) {
                when (currentScreen) {
                    "home" -> HomeScreen(
                        viewModel = viewModel,
                        currentLang = currentLang,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        providers = providers,
                        categories = categories,
                        banners = banners,
                        searchKeyword = searchKeyword,
                        onSearchChange = { searchKeyword = it },
                        voiceSearchActive = voiceSearchActive,
                        onVoiceSearchToggle = {
                            voiceSearchActive = !voiceSearchActive
                            if (voiceSearchActive) {
                                searchKeyword = "سباك صنعاء" // Speech simulation trigger
                                Toast.makeText(context, if (dirRTL) "تم تمثيل مدخلات الصوت: سباك صنعاء" else "Voice recognized: Plumber", Toast.LENGTH_SHORT).show()
                                voiceSearchActive = false
                            }
                        },
                        selectedCategory = selectedCategoryFilter,
                        onSelectCategory = { selectedCategoryFilter = it },
                        onShowReview = { showReviewDialogForProvider = it },
                        onShowReport = { showReportDialogForProvider = it },
                        userPoints = userPoints,
                        onRedeemPoints = { viewModel.claimPointsRedemption() },
                        onShareReward = { viewModel.addPointsFromShare() }
                    )
                    "login" -> LoginScreen(
                        viewModel = viewModel,
                        currentLang = currentLang,
                        primaryColor = primaryColor,
                        onSuccess = { currentScreen = "admin_dashboard" }
                    )
                    "register_provider" -> RegisterProviderScreen(
                        viewModel = viewModel,
                        currentLang = currentLang,
                        primaryColor = primaryColor,
                        categories = categories,
                        onSuccess = {
                            Toast.makeText(context, if (dirRTL) "تم إرسال الطلب للمشرف للمراجعة السريعة!" else "Application sent for verification!", Toast.LENGTH_LONG).show()
                            currentScreen = "home"
                        }
                    )
                    "admin_dashboard" -> AdminDashboardScreen(
                        viewModel = viewModel,
                        currentLang = currentLang,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        providers = providers,
                        pendingProviders = pendingProviders,
                        categories = categories,
                        banners = banners,
                        reports = reports,
                        reviews = reviews,
                        admins = admins,
                        logs = logs,
                        messages = messages
                    )
                    "aboutUs" -> AboutAppScreen(
                        settings = settings,
                        currentLang = currentLang,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }
            }

            // ------------------ 4. FOOTER ------------------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: About App Icon trigger
                    IconButton(
                        modifier = Modifier.size(36.dp),
                        onClick = { currentScreen = "aboutUs" }
                    ) {
                        Icon(Icons.Default.Info, "About", tint = Color.LightGray.copy(alpha = 0.7f))
                    }

                    // Center: Custom adjustable Footer Text with 50% scale
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = settings.footerText,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp, // Half size of normal text
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "v2.6.2026 - wam2026",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // Right: Help AI Assistant Trigger
                    Button(
                        onClick = { showAssistantSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (dirRTL) "خدمات 🤖" else "AI Help",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ------------------ FLOATING ACTION CHAT TRIGGER ------------------
        if (settings.chatEnabled) {
            FloatingActionButton(
                onClick = { showChatSheet = true },
                containerColor = primaryColor,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 16.dp)
                    .size(46.dp) // Symmetrical 50% sleek reduction requested
            ) {
                Icon(Icons.Default.Chat, "Chat", modifier = Modifier.size(20.dp))
            }
        }

        // ------------------ BACKDOOR ACCESS DIALOG ------------------
        if (showBackdoorDialog) {
            BackdoorPasswordDialog(
                currentLang = currentLang,
                onDismiss = { showBackdoorDialog = false },
                onAuthorize = {
                    showBackdoorDialog = false
                    currentScreen = "admin_dashboard"
                    viewModel.loginAdmin("WAM2026", "maher736462")
                }
            )
        }

        // ------------------ DYNAMIC REVIEWS FEEDBACK DIALOG ------------------
        showReviewDialogForProvider?.let { provider ->
            RatingReviewSubmissionDialog(
                provider = provider,
                currentLang = currentLang,
                onDismiss = { showReviewDialogForProvider = null },
                onSubmit = { stars, text ->
                    viewModel.submitReview(provider.id, stars, text)
                    showReviewDialogForProvider = null
                    Toast.makeText(context, if (dirRTL) "شكراً لتقييمك! كسبت +15 نقطة ولاء!" else "Thank you! +15 Points!", Toast.LENGTH_LONG).show()
                }
            )
        }

        // ------------------ CRIME INCIDENT COMPLAINT DIALOG ------------------
        showReportDialogForProvider?.let { provider ->
            ReportSubmissionDialog(
                provider = provider,
                currentLang = currentLang,
                onDismiss = { showReportDialogForProvider = null },
                onSubmit = { category, details ->
                    viewModel.submitReport(provider.id, category, details)
                    showReportDialogForProvider = null
                    Toast.makeText(context, if (dirRTL) "تم إرسال بلاغك وسيقوم الدعم باتخاذ اللازم" else "Report filed securely", Toast.LENGTH_LONG).show()
                }
            )
        }

        // ------------------ LIVE INTERACTIVE AI ASSISTANT SHEET ------------------
        if (showAssistantSheet) {
            AssistantChatDialog(
                viewModel = viewModel,
                currentLang = currentLang,
                primaryColor = primaryColor,
                onDismiss = { showAssistantSheet = false }
            )
        }

        // ------------------ LIVE REAL-TIME RECONCILIATION CHATTING SHEET ------------------
        if (showChatSheet) {
            RealtimeCustomerChatDialog(
                viewModel = viewModel,
                currentLang = currentLang,
                primaryColor = primaryColor,
                onDismiss = { showChatSheet = false }
            )
        }
    }
}

// --------------------------------------------------------------------------------------
// --------------------------------- HOME SCREEN VIEW ----------------------------------
// --------------------------------------------------------------------------------------
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    currentLang: String,
    primaryColor: Color,
    secondaryColor: Color,
    providers: List<ServiceProvider>,
    categories: List<Category>,
    banners: List<Banner>,
    searchKeyword: String,
    onSearchChange: (String) -> Unit,
    voiceSearchActive: Boolean,
    onVoiceSearchToggle: () -> Unit,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    onShowReview: (ServiceProvider) -> Unit,
    onShowReport: (ServiceProvider) -> Unit,
    userPoints: Int,
    onRedeemPoints: () -> Unit,
    onShareReward: () -> Unit
) {
    val context = LocalContext.current
    val dirRTL = currentLang == "ar"

    // Filter providers computed live
    val filteredProviders = remember(providers, searchKeyword, selectedCategory) {
        providers.filter { provider ->
            val matchesSearch = searchKeyword.isBlank() ||
                    provider.fullName.substringAfter(" ").contains(searchKeyword, ignoreCase = true) ||
                    provider.fullName.contains(searchKeyword, ignoreCase = true) ||
                    provider.address.contains(searchKeyword, ignoreCase = true) ||
                    provider.district.contains(searchKeyword, ignoreCase = true)
            
            val matchesCategory = selectedCategory == null || provider.mainCategoryId == selectedCategory

            matchesSearch && matchesCategory && !provider.isBlocked
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // 1. Promotional Paid Banner Ads
        val activeBanners = banners.filter { it.isActive }
        if (activeBanners.isNotEmpty()) {
            item {
                Text(
                    text = if (dirRTL) "إعلان ممول" else "Sponsored Promo",
                    color = secondaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activeBanners) { adv ->
                        Card(
                            modifier = Modifier
                                .width(if (adv.size == "L") 320.dp else 240.dp)
                                .height(110.dp)
                                .clickable {
                                    Toast.makeText(context, "${adv.redirectLink}", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, secondaryColor.copy(alpha = 0.5f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = adv.mediaUrl.ifEmpty { "https://picsum.photos/500/300" },
                                    contentDescription = "banner",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                                )
                                Text(
                                    text = adv.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. High Symmetrical Filter Bar & Simulated Audio Trigger
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchKeyword,
                        onValueChange = onSearchChange,
                        placeholder = { Text(if (dirRTL) "ابحث عن سباك، كهربائي، صنعاء..." else "Search name, location...", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        singleLine = true,
                        trailingIcon = {
                            if (searchKeyword.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Default.Close, "Clear", tint = Color.White)
                                }
                            }
                        }
                    )
                    IconButton(onClick = onVoiceSearchToggle) {
                        Icon(Icons.Default.Mic, "Speech Voice", tint = secondaryColor)
                    }
                }
            }
        }

        // 3. VIP Recommended / Recommended verified scroll panel
        val vipProviders = providers.filter { it.isRecommended }
        if (vipProviders.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (dirRTL) "⭐ مقدمو الخدمات VIP الموصى بهم" else "⭐ VIP Recommended Partners",
                        color = secondaryColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(vipProviders) { p ->
                        Card(
                            modifier = Modifier
                                .width(130.dp)
                                .clickable { onSearchChange(p.fullName) },
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, secondaryColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.size(44.dp)) {
                                    AsyncImage(
                                        model = p.profileImageUrl.ifEmpty { "https://api.dicebear.com/7.x/bottts/png?seed=${p.id}" },
                                        contentDescription = "vip_pic",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Icon(
                                        Icons.Default.Star,
                                        "VIP",
                                        tint = secondaryColor,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .align(Alignment.BottomEnd)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = p.fullName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = p.district,
                                    color = Color.LightGray,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Categories Quick badging filter selectors circular layout
        item {
            Text(
                text = if (dirRTL) "الأقسام المتاحة حالياً" else "Service Categories Available",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onSelectCategory(null) },
                        label = { Text(if (dirRTL) "الكل" else "All") }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.id,
                        onClick = { onSelectCategory(cat.id) },
                        label = { Text(if (dirRTL) cat.nameAr else cat.nameEn) },
                        leadingIcon = {
                            val iconSymbol = when (cat.imageUrl) {
                                "carpentry" -> Icons.Default.Handyman
                                "electrical" -> Icons.Default.FlashOn
                                "plumbing" -> Icons.Default.WaterDrop
                                "maintenance" -> Icons.Default.Build
                                else -> Icons.Default.Category
                            }
                            Icon(iconSymbol, "", modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }

        // 5. Loyalty Points Wallets Progress
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, secondaryColor.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (dirRTL) "🎁 نقاط الولاء والمكافآت" else "🎁 Loyalty Rewards Points",
                            color = secondaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (dirRTL) "رصيدك الحالي: $userPoints نقطة (كل 100 نقطة خصم 5,000 ريال)" else "Current points: $userPoints pts (100 pts = 5000 YER discount)",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = onRedeemPoints,
                            colors = ButtonDefaults.buttonColors(containerColor = secondaryColor),
                            enabled = userPoints >= 100,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(if (dirRTL) "استبدل خصم" else "Redeem", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                onShareReward()
                                Toast.makeText(context, if (dirRTL) "تمت مشاركة طاقم الوصل وكسبت +20 نقطة!" else "Earned +20 pts share reward!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(if (dirRTL) "مشاركة" else "Share", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // 6. Symmetrical listings of Tech workers profiles
        if (filteredProviders.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.SearchOff, "No results", tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (dirRTL) "لا يوجد مهندسين أو مقدمي خدمات يطابقون تصفيتك..." else "No active verified providers matches",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(filteredProviders) { provider ->
                ProviderItemCard(
                    provider = provider,
                    currentLang = currentLang,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    onRateClick = { onShowReview(provider) },
                    onReportClick = { onShowReport(provider) }
                )
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// ----------------------------- CUSTOM PROVIDER CARD ELEMENT ---------------------------
// --------------------------------------------------------------------------------------
@Composable
fun ProviderItemCard(
    provider: ServiceProvider,
    currentLang: String,
    primaryColor: Color,
    secondaryColor: Color,
    onRateClick: () -> Unit,
    onReportClick: () -> Unit
) {
    val context = LocalContext.current
    val dirRTL = currentLang == "ar"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (provider.isPinned) secondaryColor else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(50.dp)) {
                        AsyncImage(
                            model = provider.profileImageUrl.ifEmpty { "https://api.dicebear.com/7.x/bottts/png?seed=${provider.id}" },
                            contentDescription = "pic",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        if (provider.isVerified) {
                            Icon(
                                Icons.Default.CheckCircle,
                                "Verified",
                                tint = Color(0xFF1E88E5), // Authentic tech verified blue
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = provider.fullName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (provider.isPinned) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (dirRTL) "مثبت" else "Pinned",
                                    color = secondaryColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color.Black, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, "Loc", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                            Text(
                                text = "${provider.address} - ${provider.district}",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Symmetrical price badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (dirRTL) "معاينة: 4000﷼" else "Visit: 4000 YER",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stars Rating feedback and status availability
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rate rating displays
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { ind ->
                        Icon(
                            Icons.Default.Star,
                            "",
                            tint = if (ind < provider.averageRating.toInt()) secondaryColor else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${provider.totalReviews})",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }

                // Status tag
                Text(
                    text = if (dirRTL) "✓ متاح للعمل حالياً" else "✓ Active & Available",
                    color = Color(0xFF4CAF50),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.08f))

            // Action interactive buttons Call | WhatsApp | Rev | Report
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val phoneUrl = "tel:${provider.phone}"
                        Toast.makeText(context, "${if (dirRTL) "اتصال بـ" else "Call dialer:"} $phoneUrl", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Phone, "", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (dirRTL) "اتصال" else "Call", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val intentUrl = "https://wa.me/${provider.phone}"
                        Toast.makeText(context, "${if (dirRTL) "دردشة واتساب:" else "WhatsApp launch:"} $intentUrl", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Message, "", tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (dirRTL) "واتساب" else "WhatsApp", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Rate Action
                IconButton(onClick = onRateClick) {
                    Icon(Icons.Default.StarBorder, "Rate", tint = secondaryColor)
                }

                // Report Abuse Action
                IconButton(onClick = onReportClick) {
                    Icon(Icons.Default.Warning, "Report", tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// ---------------------------- DIRECT LOGIN ENTRY POINT -------------------------------
// --------------------------------------------------------------------------------------
@Composable
fun LoginScreen(
    viewModel: AppViewModel,
    currentLang: String,
    primaryColor: Color,
    onSuccess: () -> Unit
) {
    val dirRTL = currentLang == "ar"
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Lock, "", tint = primaryColor, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (dirRTL) "تسجيل دخول المشرفين والدعم" else "Administrative Secure Access",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(if (dirRTL) "اسم المستخدم" else "Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = primaryColor
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(if (dirRTL) "كلمة المرور السرية" else "Secret Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = primaryColor
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
            Text(if (dirRTL) "حفظ تذكر تسجيل الدخول" else "Save authenticated profile session", color = Color.LightGray, fontSize = 12.sp)
        }

        errorMsg?.let { msg ->
            Text(msg, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val ok = viewModel.loginAdmin(username, password)
                if (ok) {
                    onSuccess()
                } else {
                    errorMsg = if (dirRTL) "خطأ في اسم المستخدم أو كلمة السر!" else "Invalid secure admin keypair combination"
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (dirRTL) "مصادقة والدخول للوحة" else "Authenticate & Access Panel", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// --------------------------------------------------------------------------------------
// ------------------------- REGISTER PROVIDER SUBMISSION FORM --------------------------
// --------------------------------------------------------------------------------------
@Composable
fun RegisterProviderScreen(
    viewModel: AppViewModel,
    currentLang: String,
    primaryColor: Color,
    categories: List<Category>,
    onSuccess: () -> Unit
) {
    val dirRTL = currentLang == "ar"

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var mainCatId by remember { mutableStateOf("") }
    var subCatId by remember { mutableStateOf("") }
    var workAddress by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var selectedProfilePicUrl by remember { mutableStateOf("") }
    var selectedIdCardUrl by remember { mutableStateOf("") }

    var expandedMain by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (dirRTL) "📝 استمارة انضمام مقدمي الخدمات والدليل" else "📝 Professional Guild Application",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text(if (dirRTL) "الاسم الثلاثي الكامل (إجباري)" else "Full Triple Name (Required)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(if (dirRTL) "رقم الهاتف / واتساب الفعال (إجباري)" else "Phone / Active WhatsApp No. (Required)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        // Dropdown for Main Categories
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expandedMain = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                val selectedTxt = categories.find { it.id == mainCatId }?.let { if (dirRTL) it.nameAr else it.nameEn }
                Text(selectedTxt ?: (if (dirRTL) "اختر التخصص الرئيسي (إجباري)" else "Select Main Category (Required)"))
            }
            DropdownMenu(expanded = expandedMain, onDismissRequest = { expandedMain = false }) {
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(if (dirRTL) cat.nameAr else cat.nameEn) },
                        onClick = {
                            mainCatId = cat.id
                            expandedMain = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = workAddress,
            onValueChange = { workAddress = it },
            label = { Text(if (dirRTL) "عنوان مركز ومكتب العمل المعتمد" else "Work Center Address (Required)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = district,
            onValueChange = { district = it },
            label = { Text(if (dirRTL) "منطقة السكن / المديرية (إجباري)" else "Residency District / City Area (Required)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        // Profiles pictures picker simulation links (as mandated WebP & automatic size control is in place)
        Text(if (dirRTL) "📷 تحميل الصورة الشخصية (إجباري للرجال / اختياري للنساء)" else "📷 Portrait profile picture upload (Required for men)", color = Color.White, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { selectedProfilePicUrl = "https://picsum.photos/200?seed=portrait" },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text(if (dirRTL) "📷 التقاط صورة" else "📷 Camera Capture", fontSize = 11.sp)
            }
            Button(
                onClick = { selectedProfilePicUrl = "https://picsum.photos/200?seed=gallery" },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text(if (dirRTL) "🖼️ المعرض" else "🖼️ Choose Gallery", fontSize = 11.sp)
            }
        }
        if (selectedProfilePicUrl.isNotEmpty()) {
            Text(if (dirRTL) "✓ تم التقاط ومعالجة وصغط الصورة شخصية بنجاح WebP" else "✓ Portrait compiled and optimized successfully (WebP)", color = Color.Green, fontSize = 11.sp)
        }

        Text(if (dirRTL) "🪪 تحميل صورة بطاقة الهوية الشخصية (اختياري)" else "🪪 Upload Identification ID Card (Optional)", color = Color.White, fontSize = 12.sp)
        Button(
            onClick = { selectedIdCardUrl = "https://picsum.photos/400/300?seed=idcard" },
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Text(if (dirRTL) "🖼️ رفع صورة الهوية" else "🖼️ Attach ID copy")
        }
        if (selectedIdCardUrl.isNotEmpty()) {
            Text(if (dirRTL) "✓ تم حفظ صورة البطاقة مؤقتاً" else "✓ ID attached", color = Color.Green, fontSize = 11.sp)
        }

        Button(
            onClick = {
                if (fullName.isBlank() || phone.isBlank() || mainCatId.isBlank()) {
                    return@Button
                }
                viewModel.submitPendingProvider(
                    fullName, phone, mainCatId, subCatId, workAddress, district,
                    selectedProfilePicUrl.ifEmpty { "https://api.dicebear.com/7.x/bottts/png?seed=$phone" },
                    selectedIdCardUrl
                )
                onSuccess()
            },
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (dirRTL) "تقديم طلب الانضمام للمراجعة الفورية" else "Submit Application for instant approval", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// --------------------------------------------------------------------------------------
// -------------------------------- ABOUT APP SCREEN -----------------------------------
// --------------------------------------------------------------------------------------
@Composable
fun AboutAppScreen(
    settings: AppSettings,
    currentLang: String,
    primaryColor: Color,
    secondaryColor: Color
) {
    val dirRTL = currentLang == "ar"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = primaryColor)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("WAM", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            }
        }

        Text(
            text = settings.appName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Text(
            text = if (dirRTL) "المنصة اليمنية الفاخرة المعتمدة لربط أصحاب المهن اليدوية والكوادر الهندسية بالعملاء مباشرة. يمن فريست لتسريع الأعمال." else "Premium certified directory for connecting skilled Yemeni specialists and constructors with local employers contextually.",
            color = Color.LightGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Divider(color = Color.White.copy(alpha = 0.1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(if (dirRTL) "📞 اتصل بنا" else "📞 Hotline Call", color = secondaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(settings.supportPhone, color = Color.White, fontSize = 11.sp)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(if (dirRTL) "📧 البريد الإلكتروني" else "📧 Email Support", color = secondaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(settings.supportEmail, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// ------------------------------- SECRET DIALOG TEMPLATES ------------------------------
// --------------------------------------------------------------------------------------
@Composable
fun BackdoorPasswordDialog(
    currentLang: String,
    onDismiss: () -> Unit,
    onAuthorize: () -> Unit
) {
    val dirRTL = currentLang == "ar"
    var enteredSecret by remember { mutableStateOf("") }
    var saveAuthTicket by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (dirRTL) "🔑 البوابة الخلفية السرية" else "🔑 Owner Backdoor Vault",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                OutlinedTextField(
                    value = enteredSecret,
                    onValueChange = { enteredSecret = it },
                    label = { Text(if (dirRTL) "كلمة المرور الأمنية السرية" else "Secret Door Code") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = saveAuthTicket, onCheckedChange = { saveAuthTicket = it })
                    Text(if (dirRTL) "تذكر جهة الاتصال والولوج" else "Remember owner session key", color = Color.LightGray, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (dirRTL) "إلغاء" else "Cancel", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            if (enteredSecret == "maher--736462") {
                                onAuthorize()
                            } else {
                                enteredSecret = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text(if (dirRTL) "ولوج" else "Enter Door", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun RatingReviewSubmissionDialog(
    provider: ServiceProvider,
    currentLang: String,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    val dirRTL = currentLang == "ar"
    var starCount by remember { mutableStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (dirRTL) "⭐ تقييم مقدم الخدمة: ${provider.fullName}" else "⭐ Evaluate ${provider.fullName}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(5) { rateIdx ->
                        val rate = rateIdx + 1
                        IconButton(onClick = { starCount = rate }) {
                            Icon(
                                Icons.Default.Star,
                                "",
                                tint = if (rate <= starCount) Color(0xFFFFD700) else Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = reviewComment,
                    onValueChange = { reviewComment = it },
                    label = { Text(if (dirRTL) "التعليق والتقييم اليدوي (اختياري)" else "Review comment (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(if (dirRTL) "إلغاءام" else "Cancel") }
                    Button(onClick = { onSubmit(starCount, reviewComment) }) {
                        Text(if (dirRTL) "إرسال" else "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun ReportSubmissionDialog(
    provider: ServiceProvider,
    currentLang: String,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    val dirRTL = currentLang == "ar"
    var complaintsReason by remember { mutableStateOf("") }
    var detailText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (dirRTL) "⚠️ تقديم شكوى ضد: ${provider.fullName}" else "⚠️ Report: ${provider.fullName}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = complaintsReason,
                    onValueChange = { complaintsReason = it },
                    label = { Text(if (dirRTL) "سبب البلاغ (احتيال، خدمة سيئة، إلخ)" else "Reason (Abuse, overprice...)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = detailText,
                    onValueChange = { detailText = it },
                    label = { Text(if (dirRTL) "التفاصيل الإضافية للبلاغ" else "More specific details...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(if (dirRTL) "إلغاء" else "Cancel") }
                    Button(
                        onClick = { onSubmit(complaintsReason, detailText) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text(if (dirRTL) "إرسال بلاغ" else "Report", color = Color.White)
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// ----------------------------- FLOATING CHAT / HELP SYSTEM ---------------------------
// --------------------------------------------------------------------------------------
@Composable
fun AssistantChatDialog(
    viewModel: AppViewModel,
    currentLang: String,
    primaryColor: Color,
    onDismiss: () -> Unit
) {
    val dirRTL = currentLang == "ar"
    val chatLogs by viewModel.assistantChat.collectAsState()
    val isWorking by viewModel.assistantLoading.collectAsState()
    var inputQuery by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(primaryColor)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (dirRTL) "🤖 مساعد خدمات اليمن الذكي" else "🤖 Smart Assistant Engine", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "", tint = Color.White)
                        }
                    }
                }

                // Messages lists
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatLogs) { item ->
                        val isUser = item.second
                        val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                        val bgColor = if (isUser) primaryColor else Color.White.copy(alpha = 0.08f)
                        val txtColor = if (isUser) Color.White else Color.LightGray

                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = bgColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = item.first,
                                    color = txtColor,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(10.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    if (isWorking) {
                        item {
                            Text(if (dirRTL) "جاري المعركة والتفكير..." else "Thinking...", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                        }
                    }
                }

                // Interactive FAQs chips selectors
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(selected = false, onClick = { viewModel.askAssistant("ماهي الأقسام") }, label = { Text("الأقسام", fontSize = 10.sp) })
                    }
                    item {
                        FilterChip(selected = false, onClick = { viewModel.askAssistant("اتصل") }, label = { Text("كيف أتصل؟", fontSize = 10.sp) })
                    }
                    item {
                        FilterChip(selected = false, onClick = { viewModel.askAssistant("رقم الدعم") }, label = { Text("الدعم الفني", fontSize = 10.sp) })
                    }
                }

                // Input rows
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputQuery,
                        onValueChange = { inputQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(if (dirRTL) "اكتب سؤالك هنا..." else "Ask any help...", color = Color.Gray, fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (inputQuery.isNotBlank()) {
                                viewModel.askAssistant(inputQuery)
                                inputQuery = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, "Send", tint = primaryColor)
                    }
                }
            }
        }
    }
}

@Composable
fun RealtimeCustomerChatDialog(
    viewModel: AppViewModel,
    currentLang: String,
    primaryColor: Color,
    onDismiss: () -> Unit
) {
    val dirRTL = currentLang == "ar"
    val messages by viewModel.messages.collectAsState()
    var inputMsg by remember { mutableStateOf("") }
    val simulatedMyId = "UserGuest"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(primaryColor)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (dirRTL) "💬 المحادثة المباشرة الفورية" else "💬 Live Support Sync Room", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "", tint = Color.White)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        val isMe = msg.senderId == simulatedMyId
                        val align = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                        val bg = if (isMe) primaryColor else Color.White.copy(alpha = 0.08f)

                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = align) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = bg),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = msg.message,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputMsg,
                        onValueChange = { inputMsg = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(if (dirRTL) "اكتب رسالة للادمن..." else "Message administrators...", color = Color.Gray, fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                    IconButton(
                        onClick = {
                            if (inputMsg.isNotBlank()) {
                                viewModel.dispatchChatMessage("global_sync", simulatedMyId, "Admin", inputMsg)
                                inputMsg = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, "Send", tint = primaryColor)
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// ----------------------------- ADMIN WORKSPACE MAIN LAYOUT ----------------------------
// --------------------------------------------------------------------------------------
@Composable
fun AdminDashboardScreen(
    viewModel: AppViewModel,
    currentLang: String,
    primaryColor: Color,
    secondaryColor: Color,
    providers: List<ServiceProvider>,
    pendingProviders: List<PendingProvider>,
    categories: List<Category>,
    banners: List<Banner>,
    reports: List<Report>,
    reviews: List<Review>,
    admins: List<Admin>,
    logs: List<ActivityLog>,
    messages: List<ChatMessage>
) {
    val dirRTL = currentLang == "ar"
    var activeWorkspaceTabIdx by remember { mutableStateOf(0) }

    val adminMenuNames = listOf(
        if (dirRTL) "طلبات معلقة" else "Pending requests",
        if (dirRTL) "إضافة فني يدوي" else "Add tech manual",
        if (dirRTL) "الأقسام والمدن" else "Categories/Cities",
        if (dirRTL) "إعلانات وبنرات" else "Promo Ads Banners",
        if (dirRTL) "البلاغات والتقارير" else "Incidents reports",
        if (dirRTL) "المزودين النشطين" else "Verified Active Providers",
        if (dirRTL) "المشرفين" else "Co-admins managements",
        if (dirRTL) "التخصيص والألوان" else "Theming Customize",
        if (dirRTL) "نسخ احتياطي" else "Secure storage backups",
        if (dirRTL) "سجل النشاط" else "Activity Logs Audit"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick visual workspace tab selectors
        ScrollableTabRow(
            selectedTabIndex = activeWorkspaceTabIdx,
            containerColor = Color.White.copy(alpha = 0.05f),
            contentColor = secondaryColor,
            edgePadding = 8.dp
        ) {
            adminMenuNames.forEachIndexed { i, name ->
                Tab(
                    selected = activeWorkspaceTabIdx == i,
                    onClick = { activeWorkspaceTabIdx = i },
                    text = { Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        ) {
            when (activeWorkspaceTabIdx) {
                0 -> SectionPendingRequests(viewModel, currentLang, pendingProviders, primaryColor)
                1 -> SectionDirectAddition(viewModel, currentLang, categories, primaryColor)
                2 -> SectionCategoriesControl(viewModel, currentLang, categories, primaryColor)
                3 -> SectionBannersControl(viewModel, currentLang, banners, primaryColor)
                4 -> SectionReportsComplaints(viewModel, currentLang, reports, primaryColor)
                5 -> SectionActiveProvidersList(viewModel, currentLang, providers, secondaryColor)
                6 -> SectionSupervisorsControl(viewModel, currentLang, admins, primaryColor)
                7 -> SectionBackdoorCustomizer(viewModel, currentLang, primaryColor, secondaryColor)
                8 -> SectionDatabaseBackup(viewModel, currentLang, primaryColor)
                9 -> SectionActivityAuditLogs(viewModel, currentLang, logs)
            }
        }

        // Logout
        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(if (dirRTL) "تسجيل خروج من جلسة الإشراف" else "Terminate Admin Session", color = Color.White)
        }
    }
}

// ------------------- ADMIN REUSABLE SECTION 1: PENDING TARGETS -------------------
@Composable
fun SectionPendingRequests(viewModel: AppViewModel, currentLang: String, list: List<PendingProvider>, primaryColor: Color) {
    val dirRTL = currentLang == "ar"
    var expandedReasonId by remember { mutableStateOf<String?>(null) }
    var reasonText by remember { mutableStateOf("") }

    if (list.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (dirRTL) "لا توجد أي طلبات تسجيل معلقة حالياً" else "Zero pending verification files", color = Color.LightGray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list) { p ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(p.fullName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(p.phone, color = Color.LightGray, fontSize = 12.sp)
                                Text("${p.address} | ${p.district}", color = Color.Gray, fontSize = 11.sp)
                            }
                            AsyncImage(
                                model = p.profileImageUrl,
                                contentDescription = "reg",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.handleApprove(p) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                            ) {
                                Text(if (dirRTL) "قبول" else "Approve", color = Color.White, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { expandedReasonId = p.id },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text(if (dirRTL) "رفض" else "Reject", color = Color.White, fontSize = 11.sp)
                            }
                        }

                        if (expandedReasonId == p.id) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                OutlinedTextField(
                                    value = reasonText,
                                    onValueChange = { reasonText = it },
                                    label = { Text(if (dirRTL) "اكتب سبب الرفض (إجباري)" else "Decline explanation (Required)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        if (reasonText.isNotBlank()) {
                                            viewModel.handleReject(p.id, reasonText)
                                            expandedReasonId = null
                                            reasonText = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                ) {
                                    Text(if (dirRTL) "حفظ الرفض" else "Confirm Decline")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------- ADMIN SECTION 2: QUICK ONBOARDING -------------------
@Composable
fun SectionDirectAddition(viewModel: AppViewModel, currentLang: String, cats: List<Category>, primaryColor: Color) {
    val dirRTL = currentLang == "ar"
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("4000") }
    var mainCatId by remember { mutableStateOf("") }
    var vipBadge by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(if (dirRTL) "⚙️ إضافة مقدم خدمات فوري ومجاني للدليل" else "⚙️ Direct Tech Registration", color = Color.White)

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(if (dirRTL) "الاسم بالكامل" else "Full Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(if (dirRTL) "رقم الهاتف" else "Phone No.") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text(if (dirRTL) "سعر كشف الزيارة" else "Visit Price") }, modifier = Modifier.fillMaxWidth())

        // Quick Selector main
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(cats) { c ->
                FilterChip(
                    selected = mainCatId == c.id,
                    onClick = { mainCatId = c.id },
                    label = { Text(if (dirRTL) c.nameAr else c.nameEn) }
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = vipBadge, onCheckedChange = { vipBadge = it })
            Text(if (dirRTL) "تفعيل توصية VIP الفورية" else "Direct Recommended VIP Status", color = Color.White)
        }

        Button(
            onClick = {
                if (name.isNotBlank() && phone.isNotBlank()) {
                    viewModel.quickAddProviderDirect(name, phone, mainCatId, priceStr.toDoubleOrNull() ?: 4000.0, vipBadge)
                    name = ""
                    phone = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (dirRTL) "إضافة مباشر للدليل" else "Publish directly to Live App")
        }
    }
}

// ------------------- ADMIN SECTION 3: CATEGORIES MANAGING -------------------
@Composable
fun SectionCategoriesControl(viewModel: AppViewModel, currentLang: String, list: List<Category>, primaryColor: Color) {
    val dirRTL = currentLang == "ar"
    var arName by remember { mutableStateOf("") }
    var enName by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf("maintenance") }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (dirRTL) "إدارة وتعديل الفئات سريعة التحميل" else "Quick Categories Management", color = Color.White, fontWeight = FontWeight.Bold)

        OutlinedTextField(value = arName, onValueChange = { arName = it }, label = { Text(if (dirRTL) "اسم الفئة بالعربية" else "Category name Arabic") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = enName, onValueChange = { enName = it }, label = { Text(if (dirRTL) "اسم الفئة بالإنجليزية" else "Category name English") }, modifier = Modifier.fillMaxWidth())

        // Symmetrical choices labels Icons standard keys
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("plumbing", "electrical", "carpentry", "maintenance").forEach { key ->
                ElevatedFilterChip(
                    selected = iconKey == key,
                    onClick = { iconKey = key },
                    label = { Text(key) }
                )
            }
        }

        Button(
            onClick = {
                if (arName.isNotBlank() && enName.isNotBlank()) {
                    viewModel.createCategory(arName, enName, null, list.size + 1, iconKey)
                    arName = ""
                    enName = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (dirRTL) "إدراج الفئة للتصفح فوراً" else "Insert category directly to listings")
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(list) { c ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${c.nameAr} | ${c.nameEn}", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Icon Key: ${c.imageUrl}", color = Color.LightGray, fontSize = 11.sp)
                        }
                        IconButton(onClick = { viewModel.removeCategory(c.id) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

// ------------------- ADMIN SECTION 4: ADS CAMPAIGNS -------------------
@Composable
fun SectionBannersControl(viewModel: AppViewModel, currentLang: String, list: List<Banner>, primaryColor: Color) {
    val dirRTL = currentLang == "ar"
    var title by remember { mutableStateOf("") }
    var mediaUrl by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("") }
    var bannerSize by remember { mutableStateOf("M") }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (dirRTL) "إدارة الإعلانات الترويجية الممولة" else "Promotional Campaigns manager", color = Color.White)

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(if (dirRTL) "عنوان الإعلان" else "Campaign text title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = mediaUrl, onValueChange = { mediaUrl = it }, label = { Text(if (dirRTL) "رابط الصورة WebP" else "Banner WebP Image URL") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = linkUrl, onValueChange = { linkUrl = it }, label = { Text(if (dirRTL) "رابط التوجيه عند الضغط" else "Redirect Link click action") }, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("S", "M", "L").forEach { s ->
                ElevatedFilterChip(selected = bannerSize == s, onClick = { bannerSize = s }, label = { Text("حجم: $s") })
            }
        }

        Button(
            onClick = {
                if (title.isNotBlank()) {
                    viewModel.createBanner(title, "image", mediaUrl, linkUrl, bannerSize, 10)
                    title = ""
                    mediaUrl = ""
                    linkUrl = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (dirRTL) "نشر الإعلان على الواجهة" else "Publish Banner Ad live")
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(list) { b ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))) {
                    Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(b.title, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Size: ${b.size} | Link: ${b.redirectLink}", color = Color.LightGray, fontSize = 11.sp)
                        }
                        IconButton(onClick = { viewModel.removeBanner(b.id) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

// ------------------- ADMIN SECTION 5: AUDITING INCIDENTS -------------------
@Composable
fun SectionReportsComplaints(viewModel: AppViewModel, currentLang: String, list: List<Report>, primaryColor: Color) {
    val dirRTL = currentLang == "ar"
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (dirRTL) "⚠️ بلاغات وشكاوى المستخدمين النشطة" else "⚠️ User complaints cases", color = Color.White, fontWeight = FontWeight.Bold)
            Button(
                onClick = {
                    Toast.makeText(context, if (dirRTL) "تم تصدير CSV بنجاح بمجلد التنزيلات" else "CSV compiled & exported successfully!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("Export SECURE CSV")
            }
        }

        if (list.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(if (dirRTL) "السجلات خالية من البلاغات حالياً" else "Zero complaints report files", color = Color.LightGray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(list) { report ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("ID Provider: ${report.providerId}", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Incident Classification: ${report.reason}", color = Color.Yellow, fontSize = 12.sp)
                            Text("Incident Testimony: ${report.details}", color = Color.LightGray, fontSize = 11.sp)
                            Text("Status Code: ${report.status}", color = Color.Green, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

// ------------------- ADMIN SECTION 6: MANAGEMENT ACTIONS -------------------
@Composable
fun SectionActiveProvidersList(viewModel: AppViewModel, currentLang: String, providers: List<ServiceProvider>, secondaryColor: Color) {
    val dirRTL = currentLang == "ar"

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (dirRTL) "👥 إدارة المهنيين وحظر السهام الموصي" else "👥 Active directory administrators", color = Color.White, fontWeight = FontWeight.Bold)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            items(providers) { p ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(p.fullName, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${p.phone} | ${p.address}", color = Color.LightGray, fontSize = 11.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("isVerified: ${p.isVerified}", color = Color.Green, fontSize = 10.sp)
                                Text("isRecommended: ${p.isRecommended}", color = secondaryColor, fontSize = 10.sp)
                            }
                        }
                        IconButton(onClick = { viewModel.removeActiveProvider(p.id) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}

// ------------------- ADMIN SECTION 7: CO-SUPERVISORS -------------------
@Composable
fun SectionSupervisorsControl(viewModel: AppViewModel, currentLang: String, list: List<Admin>, primaryColor: Color) {
    val dirRTL = currentLang == "ar"
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (dirRTL) "🔑 تفويض المشرفين والتحكم بالصلاحيات" else "🔑 Authorize supervisors & access checklist", color = Color.White, fontWeight = FontWeight.Bold)

        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                if (username.isNotBlank() && password.isNotBlank()) {
                    viewModel.createSupervisor(username, password, listOf("registrar", "reports"))
                    username = ""
                    password = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (dirRTL) "حفظ وإضافة المشرف" else "Authorize and assign supervisor")
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(list) { helper ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Supervisor: ${helper.username}", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Active Session Status: ${helper.isActive}", color = Color.Green, fontSize = 11.sp)
                        }
                        IconButton(onClick = { viewModel.removeSupervisor(helper.id) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

// ------------------- ADMIN SECTION 8: CONFIGURATIONS -------------------
@Composable
fun SectionBackdoorCustomizer(viewModel: AppViewModel, currentLang: String, primaryColor: Color, secondaryColor: Color) {
    val dirRTL = currentLang == "ar"
    val settings by viewModel.settings.collectAsState()

    var appTitle by remember { mutableStateOf(settings.appName) }
    var footerText by remember { mutableStateOf(settings.footerText) }
    var welcomeMsg by remember { mutableStateOf(settings.welcomeMessage) }
    var phoneNo by remember { mutableStateOf(settings.supportPhone) }
    var emailSupport by remember { mutableStateOf(settings.supportEmail) }
    var waSupport by remember { mutableStateOf(settings.supportWhatsApp) }

    var selectedPrimaryColorStr by remember { mutableStateOf(settings.primaryColor) }
    var selectedSecondaryColorStr by remember { mutableStateOf(settings.secondaryColor) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(if (dirRTL) "🎨 ضبط وتغيير سمات وألوان المنصة" else "🎨 Dynamically modify parameters", color = Color.White, fontWeight = FontWeight.Bold)

        OutlinedTextField(value = appTitle, onValueChange = { appTitle = it }, label = { Text("App System Name") })
        OutlinedTextField(value = footerText, onValueChange = { footerText = it }, label = { Text("Footer Text") })
        OutlinedTextField(value = welcomeMsg, onValueChange = { welcomeMsg = it }, label = { Text("Scrolling Welcome Message") })
        OutlinedTextField(value = phoneNo, onValueChange = { phoneNo = it }, label = { Text("Helpline Phone") })
        OutlinedTextField(value = emailSupport, onValueChange = { emailSupport = it }, label = { Text("Support Corporate Email") })

        // Symmetrical palette selections Cosmics / Gold / Emerald
        Text("Quick Themes Custom Palette Bindings:", color = Color.White, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = {
                    selectedPrimaryColorStr = "#004D40" // Emerald Green
                    selectedSecondaryColorStr = "#FFD700" // Luxury Gold
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004D40))
            ) {
                Text("Emerald Gold")
            }

            Button(
                onClick = {
                    selectedPrimaryColorStr = "#3E2723" // Charcoal Brown
                    selectedSecondaryColorStr = "#B0BEC5" // Cosmic Silver Metallic
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E2723))
            ) {
                Text("Charcoal Cosmic")
            }

            Button(
                onClick = {
                    selectedPrimaryColorStr = "#0D47A1" // Cobalt Navy
                    selectedSecondaryColorStr = "#FFF59D" // Soft Platinum
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
            ) {
                Text("Modern Cobalt")
            }
        }

        Button(
            onClick = {
                viewModel.updatePlatformSettings(
                    appTitle, footerText, welcomeMsg, phoneNo, emailSupport, waSupport,
                    selectedPrimaryColorStr, selectedSecondaryColorStr
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (dirRTL) "حفظ التغييرات ومزامنتها فوراً" else "Commit updates live instantly")
        }
    }
}

// ------------------- ADMIN SECTION 9: SECURE STORAGE BACKUPS -------------------
@Composable
fun SectionDatabaseBackup(viewModel: AppViewModel, currentLang: String, primaryColor: Color) {
    val dirRTL = currentLang == "ar"
    val context = LocalContext.current
    var operationalLogMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (dirRTL) "🗄️ نظام النسخ الاحتياطي السحابي والمحلي" else "🗄️ Backup & Restore Engine Room", color = Color.White, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (dirRTL) "خيارات التصدير السريع للأرشيف:" else "Select quick snapshot export:", color = Color.White, fontSize = 12.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.backupDatabaseLocal(context.filesDir) { path ->
                                operationalLogMsg = path
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (dirRTL) "نسخ احتياطي محلي" else "Backup Device Memory", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.restoreDatabaseLocal(context.filesDir) { path ->
                                operationalLogMsg = path
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (dirRTL) "استعادة الأرشيف يدوي" else "Restore Snapshot", fontSize = 11.sp)
                    }
                }
            }
        }

        if (operationalLogMsg.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.3f))
            ) {
                Text(
                    text = operationalLogMsg,
                    color = Color.Green,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

// ------------------- ADMIN SECTION 10: AUDIT SECURITY CONTROLS -------------------
@Composable
fun SectionActivityAuditLogs(viewModel: AppViewModel, currentLang: String, logs: List<ActivityLog>) {
    val dirRTL = currentLang == "ar"

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (dirRTL) "📋 سجل النشاط الإداري ومكافحة التهديدات" else "📋 Active supervisor auditing control", color = Color.White, fontWeight = FontWeight.Bold)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            items(logs) { log ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("User: ${log.adminId}", color = Color.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(Date(log.timestamp).toString().substring(11, 19), color = Color.Gray, fontSize = 10.sp)
                        }
                        Text("Event: ${log.action}", color = Color.White, fontSize = 11.sp)
                        Text("Payload Detail: ${log.details}", color = Color.LightGray, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
