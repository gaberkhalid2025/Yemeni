@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example

import android.app.Application
import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.AIChatMessage
import com.example.ui.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val viewModel: AppViewModel = viewModel()
            val settings by viewModel.settingsFlow.collectAsState(initial = null)
            val currentLang by viewModel.currentLanguage.collectAsState()
            
            // Layout direction logic: Arabic first (RTL), English (LTR)
            val layoutDirection = if (currentLang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

            // Dynamic color logic parsed from admin settings
            val themeHexColor by viewModel.primaryThemeColor.collectAsState()
            val primaryThemeColor = remember(themeHexColor) {
                try {
                    Color(android.graphics.Color.parseColor(themeHexColor))
                } catch (e: Exception) {
                    Color(0xFF8E8A9F) // Cosmic Silver default
                }
            }

            // Sync messages toast trigger
            val syncMessage by viewModel.syncMessage.collectAsState()
            LaunchedEffect(syncMessage) {
                if (syncMessage.isNotEmpty()) {
                    Toast.makeText(context, syncMessage, Toast.LENGTH_LONG).show()
                    viewModel.clearSyncMessage()
                }
            }

            // Maintenance screen blocks user access if of course active and not admin
            val isMaintenance = settings?.isMaintenanceMode == true
            val adminSession by viewModel.adminSession.collectAsState()

            val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
            val isColorBright = (primaryThemeColor.red * 0.299f + primaryThemeColor.green * 0.587f + primaryThemeColor.blue * 0.114f) > 0.5f
            val customColorScheme = remember(primaryThemeColor, isDarkTheme) {
                if (isDarkTheme) {
                    darkColorScheme(
                        primary = primaryThemeColor,
                        onPrimary = if (isColorBright) Color.Black else Color.White,
                        secondary = primaryThemeColor.copy(alpha = 0.8f),
                        background = Color(0xFF121214),
                        surface = Color(0xFF1E1E22),
                        onBackground = Color.White,
                        onSurface = Color.White,
                        surfaceVariant = Color(0xFF2C2C35),
                        onSurfaceVariant = Color.LightGray
                    )
                } else {
                    lightColorScheme(
                        primary = primaryThemeColor,
                        onPrimary = if (isColorBright) Color.Black else Color.White,
                        secondary = primaryThemeColor.copy(alpha = 0.8f),
                        background = Color(0xFFF4F4F6),
                        surface = Color.White,
                        onBackground = Color(0xFF1C1B1F),
                        onSurface = Color(0xFF1C1B1F),
                        surfaceVariant = Color(0xFFE5E5EA),
                        onSurfaceVariant = Color.DarkGray
                    )
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MaterialTheme(colorScheme = customColorScheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (isMaintenance && adminSession == null) {
                            MaintenanceScreen(
                                message = settings?.maintenanceMessage ?: "التطبيق قيد الصيانة",
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

// --- NAVIGATION CORE & BACKDOOR ICON CLICKS ---
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainNavigationHost(viewModel: AppViewModel, primaryColor: Color) {
    val navController = rememberNavController()
    val settings by viewModel.settingsFlow.collectAsState(initial = null)
    val currentLang by viewModel.currentLanguage.collectAsState()
    val adminSession by viewModel.adminSession.collectAsState()

    // Floating chat states
    var showChatDialog by remember { mutableStateOf(false) }
    var showLiveConversationsDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF121214),
        topBar = {
            TopAppBarComponent(
                navController = navController,
                viewModel = viewModel,
                primaryColor = primaryColor
            )
        },
        bottomBar = {
            FooterWidget(
                navController = navController,
                viewModel = viewModel,
                primaryColor = primaryColor
            )
        },
        floatingActionButton = {
            val isHidden = settings?.chatButtonHidden == true
            val size = settings?.chatButtonSize ?: 50
            if (!isHidden) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    val pulseTransition = rememberInfiniteTransition(label = "pulse")
                    
                    // --- Button 1: AI Assistant (Pulsing Glow / Custom Icons) ---
                    val aiGlow = settings?.assistantIconGlow == true
                    val aiScale by if (aiGlow) {
                        pulseTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                            ),
                            label = "aiScale"
                        )
                    } else {
                        remember { mutableStateOf(1.0f) }
                    }
                    
                    val aiRotation by if (settings?.iconVisualEffectType == "Rotate") {
                        pulseTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(5000, easing = androidx.compose.animation.core.LinearEasing),
                                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                            ),
                            label = "aiRot"
                        )
                    } else {
                        remember { mutableStateOf(0f) }
                    }

                    FloatingActionButton(
                        onClick = { showChatDialog = true },
                        containerColor = primaryColor,
                        contentColor = Color.Black,
                        modifier = Modifier
                            .size(size.dp)
                            .graphicsLayer(
                                scaleX = aiScale,
                                scaleY = aiScale,
                                rotationZ = aiRotation
                            )
                            .testTag("floating_ai_assistant_btn")
                    ) {
                        Icon(
                            imageVector = mapSymbolToIcon(settings?.assistantIconSymbol ?: "Face"),
                            contentDescription = "AI Assistant Help",
                            modifier = Modifier.size((size * 0.5f).dp),
                            tint = Color.Black
                        )
                    }

                    // --- Button 2: Direct Messaging Hub (Dynamic Pulsing Scale) ---
                    val chatGlow = settings?.liveChatIconGlow == true
                    val chatScale by if (chatGlow) {
                        pulseTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                            ),
                            label = "chatScale"
                        )
                    } else {
                        remember { mutableStateOf(1.0f) }
                    }

                    FloatingActionButton(
                        onClick = { showLiveConversationsDialog = true },
                        containerColor = Color(0xFFE0A96D), // Premium high-contrast gold accent
                        contentColor = Color.Black,
                        modifier = Modifier
                            .size(size.dp)
                            .graphicsLayer(
                                scaleX = chatScale,
                                scaleY = chatScale
                            )
                            .testTag("floating_live_chat_btn")
                    ) {
                        Icon(
                            imageVector = mapSymbolToIcon(settings?.liveChatIconSymbol ?: "Mail"),
                            contentDescription = "Live Direct Chat",
                            modifier = Modifier.size((size * 0.48f).dp),
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(navController = navController, viewModel = viewModel, primaryColor = primaryColor)
            }
            composable("login") {
                LoginScreen(navController = navController, viewModel = viewModel, primaryColor = primaryColor)
            }
            composable("register") {
                RegisterProviderScreen(navController = navController, viewModel = viewModel, primaryColor = primaryColor)
            }
            composable("about") {
                AboutScreen(navController = navController, viewModel = viewModel, primaryColor = primaryColor)
            }
            composable("admin_dashboard") {
                if (adminSession != null) {
                    AdminDashboardScreen(navController = navController, viewModel = viewModel, primaryColor = primaryColor)
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate("login") { popUpTo("home") }
                    }
                }
            }
        }
    }

    if (showChatDialog) {
        AIDialogWidget(
            viewModel = viewModel,
            primaryColor = primaryColor,
            onClose = { showChatDialog = false }
        )
    }

    if (showLiveConversationsDialog) {
        LiveChatHubDialog(
            viewModel = viewModel,
            primaryColor = primaryColor,
            onClose = { showLiveConversationsDialog = false }
        )
    }
}

// --- APP BAR COMPONENT (RTL FIRST - CUSTOMIZABLE) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarComponent(
    navController: NavController,
    viewModel: AppViewModel,
    primaryColor: Color
) {
    val context = LocalContext.current
    val currentLang by viewModel.currentLanguage.collectAsState()
    val adminSession by viewModel.adminSession.collectAsState()
    val settings by viewModel.settingsFlow.collectAsState(initial = null)

    // Backdoor Click counter on App Logo/Title
    var backdoorClicks by remember { mutableStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }
    var showBackdoorDialog by remember { mutableStateOf(false) }

    fun registerLogoClick() {
        val now = System.currentTimeMillis()
        if (now - lastClickTime < 1000) {
            backdoorClicks++
        } else {
            backdoorClicks = 1
        }
        lastClickTime = now

        if (backdoorClicks >= 5) {
            backdoorClicks = 0
            showBackdoorDialog = true
        }
    }

    // Dynamic icon arrangements load from admin app settings
    val iconCSV = settings?.topBarIconsArrangement ?: "Home,Login,Register,Language,Refresh"
    val arrangedIcons = iconCSV.split(",")

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF1E1E22),
            titleContentColor = Color.White
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { registerLogoClick() }
                        )
                    }
                    .testTag("app_logo_title")
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "WAM",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = settings?.appName ?: viewModel.getLocalText("app_title"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (currentLang == "ar") "دليل اليمن الطارئ" else "Yemen Live Directory",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        actions = {
            // Render customized rearranged top menu icons in RTL layout
            arrangedIcons.forEach { iconName ->
                when (iconName.trim()) {
                    "Home" -> {
                        IconButton(
                            onClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                            modifier = Modifier.testTag("menu_home_btn")
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Home", tint = primaryColor)
                        }
                    }
                    "Login" -> {
                        val isLogged = adminSession != null
                        IconButton(
                            onClick = {
                                if (isLogged) {
                                    navController.navigate("admin_dashboard")
                                } else {
                                    navController.navigate("login")
                                }
                            },
                            modifier = Modifier.testTag("menu_login_btn")
                        ) {
                            Icon(
                                imageVector = if (isLogged) Icons.Default.List else Icons.Default.Lock,
                                contentDescription = "Login",
                                tint = if (isLogged) Color.Green else Color.White
                            )
                        }
                    }
                    "Register" -> {
                        IconButton(
                            onClick = { navController.navigate("register") },
                            modifier = Modifier.testTag("menu_register_btn")
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Register", tint = Color.White)
                        }
                    }
                    "Language" -> {
                        IconButton(
                            onClick = { viewModel.toggleLanguage() },
                            modifier = Modifier.testTag("menu_language_btn")
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Language", tint = Color.Cyan)
                        }
                    }
                    "Refresh" -> {
                        IconButton(
                            onClick = { viewModel.forceSyncFirestore() },
                            modifier = Modifier.testTag("menu_refresh_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    )

    // Hidden backdoor password challenge overlay
    if (showBackdoorDialog) {
        var backdoorPasscode by remember { mutableStateOf("") }
        var savedChecked by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showBackdoorDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF222228)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("backdoor_modal")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔒 المنفذ الخلفي السري للمالك",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "هذه الواجهة محمية بالكامل ولا تظهر للعامة. الرجاء إدخال الرقم السري الفائق للصيانة الشاملة وتغيير الهوية.",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = backdoorPasscode,
                        onValueChange = { backdoorPasscode = it },
                        label = { Text("رمز المرور الفائق") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backdoor_password_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Checkbox(
                            checked = savedChecked,
                            onCheckedChange = { savedChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                        )
                        Text("حفظ تسجيل الدخول للبقاء متصلاً الفترات الطويلة", color = Color.White, fontSize = 11.sp)
                    }

                    if (errorMessage.isNotEmpty()) {
                        Text(errorMessage, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { showBackdoorDialog = false }) {
                            Text("إلغاء", color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                val ok = viewModel.attemptBackdoorLogin(backdoorPasscode, savedChecked)
                                if (ok) {
                                    showBackdoorDialog = false
                                    Toast.makeText(context, "أهلاً بك يا مالك التطبيق! تم فك قفل الصلاحيات الكلية.", Toast.LENGTH_LONG).show()
                                    navController.navigate("admin_dashboard")
                                } else {
                                    errorMessage = "الرمز السري المكتوب خاطئ تماماً!"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            modifier = Modifier.testTag("backdoor_submit_btn")
                        ) {
                            Text("فتح اللوحة", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

// --- FOOTER SECTION (50% SMALLER - EDITABLE) ---
@Composable
fun FooterWidget(
    navController: NavController,
    viewModel: AppViewModel,
    primaryColor: Color
) {
    val settings by viewModel.settingsFlow.collectAsState(initial = null)
    val footerText = settings?.footerText ?: "MAW 777644670"
    val currentLang by viewModel.currentLanguage.collectAsState()

    val opacity = settings?.footerOpacity ?: 1.0f
    val fHeight = settings?.footerHeightScale ?: 56
    val fFontSize = settings?.footerFontSize ?: 12

    Surface(
        color = Color(0xFF1E1E22).copy(alpha = opacity),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Color(0xFF2C2C35).copy(alpha = opacity), RoundedCornerShape(0.dp))
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding() // Protection from soft keys/gesture zone overlaps (not squeezed inside a fixed height surface!)
                .fillMaxWidth()
                .height(fHeight.dp) // Dynamically controlled footer height in dp for the content itself
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: App Version
            Text(
                text = "V2.6.2026",
                color = Color.White.copy(alpha = if (opacity < 0.3f) 0.3f else opacity),
                fontSize = fFontSize.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Start,
                modifier = Modifier.testTag("footer_version_text")
            )

            // Center: Footer Text / Identifier (Highly legible off-white)
            Text(
                text = footerText,
                color = Color.White.copy(alpha = if (opacity < 0.3f) 0.3f else opacity),
                fontSize = fFontSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("footer_center_text")
            )

            // Right: About Button (including Page Info Icon)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { navController.navigate("about") }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("about_app_footer_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "About App",
                    tint = primaryColor.copy(alpha = if (opacity < 0.3f) 0.3f else opacity),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (currentLang == "ar") "عن التطبيق" else "About App",
                    fontSize = fFontSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = if (opacity < 0.3f) 0.3f else opacity)
                )
            }
        }
    }
}

// --- MAINTENANCE WINDOW SCREEN OVERLAY ---
@Composable
fun MaintenanceScreen(message: String, viewModel: AppViewModel) {
    val currentLang by viewModel.currentLanguage.collectAsState()
    var passwordInput by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F11))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Build,
            contentDescription = "Maintenance",
            tint = Color(0xFFD4AF37),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (currentLang == "ar") "صيانة مجدولة طارئة" else "Scheduled Maintenance",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1F)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "تسجيل دخول المشرف للصيانة:",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("أدخل رمز الإدارة") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (loginError) {
                    Text("الرمز السري المكتوب خاطئ!", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val logged = viewModel.attemptLogin("WAM2026", passwordInput, false)
                        if (!logged) {
                            val backdoorLogged = viewModel.attemptBackdoorLogin(passwordInput, false)
                            if (!backdoorLogged) {
                                loginError = true
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("دخول المطورين والمشرفين", color = Color.Black)
                }
            }
        }
    }
}

// --- HOME SCREEN DISPLAY WITH BANNER CAROUSELS ---
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: AppViewModel,
    primaryColor: Color
) {
    val currentLang by viewModel.currentLanguage.collectAsState()
    val categoriesList by viewModel.categories.collectAsState(initial = emptyList())
    val filteredProviders by viewModel.filteredServiceProviders.collectAsState()
    val allBannersList by viewModel.allBanners.collectAsState(initial = emptyList())
    val settings by viewModel.settingsFlow.collectAsState(initial = null)

    // Filter bindings onto state fields
    val q by viewModel.searchQuery.collectAsState()
    val citySelected by viewModel.selectedCity.collectAsState()
    val categorySelected by viewModel.selectedCategoryId.collectAsState()
    val subCategorySelected by viewModel.selectedSubCategoryId.collectAsState()
    val ratSelected by viewModel.selectedRating.collectAsState()

    var showFiltersSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_lazy_list"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Dynamic Notification/Banners Slides
        val activeBanners = allBannersList.filter { it.isActive }
        if (activeBanners.isNotEmpty()) {
            item {
                PromotionBannersWidget(activeBanners = activeBanners, primaryColor = primaryColor)
            }
        }

        // 2. Main Search & Filter Console
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = q,
                            onValueChange = { viewModel.searchQuery.value = it },
                            placeholder = { Text(viewModel.getLocalText("search_hint"), fontSize = 11.sp, color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "SearchIcon") },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (q.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                    IconButton(
                                        onClick = { showFiltersSheet = !showFiltersSheet },
                                        modifier = Modifier.testTag("filter_console_toggle")
                                    ) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Filters",
                                            tint = if (citySelected != "الكل" || categorySelected != "الكل" || ratSelected > 0) Color.Green else Color.White
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_search_field")
                        )
                    }

                    // Expandable advanced filter parameters
                    if (showFiltersSheet) {
                        Spacer(modifier = Modifier.height(10.dp))
                        AdvancedFiltersFormSection(viewModel = viewModel, primaryColor = primaryColor, categoriesList = categoriesList)
                    }
                }
            }
        }

        // 3. Recommended Horizontal Grid
        val recommendedList = filteredProviders.filter { it.isRecommended }
        if (recommendedList.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "⭐ " + viewModel.getLocalText("recommended_providers"),
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recommendedList) { p ->
                            RecommendedProviderCard(
                                provider = p,
                                primaryColor = primaryColor,
                                onCall = { triggerPhoneCall(navController.context, p.phone, viewModel) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Quick Categories grid selector
        val mainCats = categoriesList.filter { it.parentCategoryId == null }
        if (mainCats.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "📦 " + viewModel.getLocalText("categories"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(1),
                        modifier = Modifier.height(54.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mainCats) { cat ->
                            val isSelected = categorySelected == cat.id
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) primaryColor else Color(0xFF232329),
                                contentColor = if (isSelected) Color.Black else Color.White,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.selectedCategoryId.value = if (isSelected) "الكل" else cat.id
                                        viewModel.selectedSubCategoryId.value = "الكل"
                                    }
                                    .testTag("category_pill_${cat.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getCategoryVector(cat.nameEn),
                                        contentDescription = cat.nameEn,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentLang == "ar") cat.nameAr else cat.nameEn,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    // Show nested subcategories if root is clicked
                    if (categorySelected != "الكل") {
                        val subCats = categoriesList.filter { it.parentCategoryId == categorySelected }
                        if (subCats.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyHorizontalGrid(
                                rows = GridCells.Fixed(1),
                                modifier = Modifier.height(44.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(subCats) { scat ->
                                    val isSubSelected = subCategorySelected == scat.id
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSubSelected) Color.Green else Color(0xFF1B1B20),
                                        contentColor = if (isSubSelected) Color.Black else Color.LightGray,
                                        modifier = Modifier.clickable {
                                            viewModel.selectedSubCategoryId.value = if (isSubSelected) "الكل" else scat.id
                                        }
                                    ) {
                                        Text(
                                            text = if (currentLang == "ar") scat.nameAr else scat.nameEn,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Service Providers Cards list results
        item {
            Text(
                text = "🔍 نتائج البحث ومزودو الخدمة المتوفرون بالقرب منك (${filteredProviders.size}):",
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (filteredProviders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, contentDescription = "Empty", tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "عذراً، لم نجد أي تطابق لخيارات البحث المحددة حالياً في منطقتك.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredProviders) { p ->
                ServiceProviderRowCard(
                    provider = p,
                    viewModel = viewModel,
                    primaryColor = primaryColor,
                    onContact = { triggerPhoneCall(navController.context, p.phone, viewModel) },
                    onWhatsApp = { triggerWhatsAppChat(navController.context, p.phone, "مرحباً ${p.name}، لقد وجدتك عبر منصة خدمات WAM.") },
                    onReport = {
                        // Submit a report dialog
                        viewModel.submitReportAgainstProvider(
                            providerId = p.id,
                            providerName = p.name,
                            rName = "مستخدم التطبيق",
                            rPhone = "مجهول اليمن",
                            complaint = "طلب الخدمة المباشرة للتقييم المتكامل"
                        )
                        Toast.makeText(navController.context, "تم رفع الإبلاغ لإدارة التطبيق للمتابعة الفورية.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// BANNERS WIDGET IMAGES & MARQUEE SLIDES
@Composable
fun PromotionBannersWidget(activeBanners: List<Banner>, primaryColor: Color) {
    var currentIndex by remember { mutableStateOf(0) }
    
    // Auto-cycle banner based on timer duration
    val activeBanner = activeBanners.getOrNull(currentIndex)
    if (activeBanner != null) {
        LaunchedEffect(currentIndex, activeBanner.duration) {
            delay(activeBanner.duration * 1000L)
            currentIndex = (currentIndex + 1) % activeBanners.size
        }

        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1515)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, primaryColor, RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = "Ads", tint = primaryColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeBanner.content,
                        color = Color.White,
                        fontSize = when (activeBanner.size) {
                            "S" -> 11.sp
                            "L" -> 15.sp
                            else -> 13.sp
                        },
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (activeBanners.size > 1) {
                    Text(
                        text = "${currentIndex + 1}/${activeBanners.size}",
                        color = primaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ADVANCED FILTERS PANEL
@Composable
fun AdvancedFiltersFormSection(
    viewModel: AppViewModel,
    primaryColor: Color,
    categoriesList: List<Category>
) {
    val citySelected by viewModel.selectedCity.collectAsState()
    val ratingSelected by viewModel.selectedRating.collectAsState()

    val yemenCities = listOf("الكل", "صنعاء", "عدن", "تعز", "إب", "حضرموت", "الحديدة", "مأرب", "ذمار")

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("تصفية بحسب المدينة والموقع الجغرافي:", color = Color.Gray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(yemenCities) { city ->
                val active = city == citySelected
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) primaryColor else Color(0xFF2B2B33),
                    contentColor = if (active) Color.Black else Color.White,
                    modifier = Modifier.clickable { viewModel.selectedCity.value = city }
                ) {
                    Text(
                        text = city,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("الحد الأدنى لتقييم مقدم الخدمة:", color = Color.Gray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..5).forEach { stars ->
                val active = ratingSelected == stars
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) Color.Yellow else Color(0xFF2B2B33),
                    contentColor = if (active) Color.Black else Color.White,
                    modifier = Modifier.clickable { viewModel.selectedRating.value = stars }
                ) {
                    Text(
                        text = if (stars == 0) "الكل" else "★ $stars+",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// HORIZONTAL RECOMMENDED MEMBER CARD
@Composable
fun RecommendedProviderCard(
    provider: ServiceProvider,
    primaryColor: Color,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.0.dp)
            .border(0.5.dp, Color.Yellow, RoundedCornerShape(8.dp))
            .clickable { onCall() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Image(
                    imageVector = Icons.Default.AccountBox,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                )
                if (provider.isVerified) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified Checked",
                        tint = Color.Cyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = provider.name,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = provider.residenceArea,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = "Reviews", tint = Color.Yellow, modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(2.dp))
                val avg = if (provider.ratingCount > 0) provider.ratingSum.toFloat() / provider.ratingCount else 4.5f
                Text(text = String.format("%.1f", avg), color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onCall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = if ((primaryColor.red * 0.299f + primaryColor.green * 0.587f + primaryColor.blue * 0.114f) > 0.5f) Color.Black else Color.White
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text("اتصل الآن", fontSize = 9.sp)
            }
        }
    }
}

// VERTICAL COMPREHENSIVE SERVICE PROVIDER ROW DISPLAY
@Composable
fun ServiceProviderRowCard(
    provider: ServiceProvider,
    viewModel: AppViewModel,
    primaryColor: Color,
    onContact: () -> Unit,
    onWhatsApp: () -> Unit,
    onReport: () -> Unit
) {
    var ratingChosen by remember { mutableStateOf(0) }
    var ratingSuccessText by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_card_${provider.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Avatar, Name, Pinned details
            Row(verticalAlignment = Alignment.Top) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Image(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Avatar Picture",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray)
                    )
                    if (provider.isVerified) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Blue Badge",
                            tint = Color.Cyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = provider.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (provider.isPinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0x22FFA000),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "📌 الأقرب لك والأنشط",
                                    fontSize = 8.sp,
                                    color = Color(0xFFFFA000),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (provider.isSubscribed) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Star, contentDescription = "Premium Sub", tint = Color.Yellow, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    val avgRating = if (provider.ratingCount > 0) provider.ratingSum.toFloat() / provider.ratingCount else 5.0f
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color.Yellow, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format("%.1f", avgRating)} نجوم (${provider.ratingCount} تقييم)",
                            color = Color(0xFFFFA000),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📱 رقم الاتصال: ${provider.phone}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "📍 العنوان: ${provider.workAddress} (${provider.residenceArea})",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subcategories label indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (provider.gender == "female") "خدمة نسوية للخصوصية" else "خدمة مهنية عامة",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.LocationOn, contentDescription = "Dist", tint = Color.Red, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("المسافة: 2.3 كم تقريباً", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 10.dp))

            // Lower Bar: Interactive buttons & 5-Star input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Mini Rating Selector
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        (1..5).forEach { star ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$star stars",
                                tint = if (ratingChosen >= star) Color.Yellow else Color.DarkGray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        ratingChosen = star
                                        viewModel.incrementRatingPoints() // Rewards points per rating trigger
                                        ratingSuccessText = "تم التقييم بنجاح! +10 نقاط ولاء."
                                    }
                            )
                        }
                    }
                    if (ratingSuccessText.isNotEmpty()) {
                        Text(ratingSuccessText, color = Color.Green, fontSize = 8.sp)
                    }
                }

                // Call and WhatsApp integrations
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onReport,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Report Complaints", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    Button(
                        onClick = onWhatsApp,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("الواتساب", color = Color.White, fontSize = 10.sp)
                    }
                    Button(
                        onClick = onContact,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("اتصل بمزود الخدمة", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- SERVICE PROVIDER REGISTRATION FORM SCREEN (👤) ---
@Composable
fun RegisterProviderScreen(
    navController: NavController,
    viewModel: AppViewModel,
    primaryColor: Color
) {
    val context = LocalContext.current
    val categoriesList by viewModel.categories.collectAsState(initial = emptyList())
    val rootCats = categoriesList.filter { it.parentCategoryId == null }

    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var mainCatSelectedId by remember { mutableStateOf("") }
    var subCatSelectedId by remember { mutableStateOf("") }
    var baseAddress by remember { mutableStateOf("") }
    var areaCircle by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("male") }

    // Images triggers with actual hardware triggers now hooked directly!
    var mockProfilePicName by remember { mutableStateOf("") }
    var mockIDCardName by remember { mutableStateOf("") }
    var feedbackError by remember { mutableStateOf("") }

    val profileGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            mockProfilePicName = "gallery_avatar_${System.currentTimeMillis().toString().takeLast(5)}.png"
            Toast.makeText(context, "تم اختيار صورة الملف الشخصي من المعرض بنجاح 📂", Toast.LENGTH_SHORT).show()
        }
    }

    val profileCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            mockProfilePicName = "camera_selfie_${System.currentTimeMillis().toString().takeLast(5)}.jpg"
            Toast.makeText(context, "تم التقاط الصورة الشخصية بلقطة كاميرا الهاتف بنجاح 📷", Toast.LENGTH_SHORT).show()
        }
    }

    val idGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            mockIDCardName = "gallery_id_${System.currentTimeMillis().toString().takeLast(5)}.png"
            Toast.makeText(context, "تم اختيار صورة بطاقة الهوية الوطنية من المعرض بنجاح 📂", Toast.LENGTH_SHORT).show()
        }
    }

    val idCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            mockIDCardName = "camera_id_${System.currentTimeMillis().toString().takeLast(5)}.jpg"
            Toast.makeText(context, "تم التقاط صورة بطاقة الهوية الذكية بكاميرا الهاتف بنجاح 📷", Toast.LENGTH_SHORT).show()
        }
    }

    var showProfilePicDialog by remember { mutableStateOf(false) }
    var showIDPicDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
            .testTag("provider_registration_screen")
    ) {
        Text(
            text = "👤 تقديم طلب الانضمام لدليل ومزودي تطبيق خدمات اليمن",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor
        )
        Text(
            text = "يرجى تعبئة كافة الحقول بشكل صحيح. سيقوم فريق المالك بالتحقق من وثائق الهوية وموقع عملك في غضون دقيقتين للموافقة السريعة.",
            fontSize = 11.sp,
            color = Color.LightGray,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Name
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("الاسم الكامل (الاسم الثلاثي على الأقل)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = primaryColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_provider_name")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Phone
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("رقم هاتف الموبايل والواتساب اليمني (مثال: 777644670)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = primaryColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_provider_phone")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category dropdown simulations
        Text("قسم تخصص العمل والخدمة المهنية:", color = Color.Gray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            rootCats.forEach { rcat ->
                val active = rcat.id == mainCatSelectedId
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) primaryColor else Color(0xFF232329),
                    contentColor = if (active) Color.Black else Color.White,
                    modifier = Modifier.clickable {
                        mainCatSelectedId = rcat.id
                        subCatSelectedId = "" // reset sub
                    }
                ) {
                    Text(rcat.nameAr, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }

        if (mainCatSelectedId.isNotEmpty()) {
            val childCats = categoriesList.filter { it.parentCategoryId == mainCatSelectedId }
            if (childCats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("اختر الخدمة الفرعية المحددة:", color = Color.Gray, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    childCats.forEach { ccat ->
                        val active = ccat.id == subCatSelectedId
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (active) Color.Green else Color(0xFF1B1B20),
                            contentColor = if (active) Color.Black else Color.LightGray,
                            modifier = Modifier.clickable { subCatSelectedId = ccat.id }
                        ) {
                            Text(ccat.nameAr, fontSize = 10.sp, modifier = Modifier.padding(6.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Gender toggling
        Text("تحديد الجنس والخصوصية:", color = Color.Gray, fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedGender == "male",
                    onClick = { selectedGender = "male" },
                    colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                )
                Text("ذكر (الصورة الشخصية إلزامية)", color = Color.White, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedGender == "female",
                    onClick = { selectedGender = "female" },
                    colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                )
                Text("أنثى (الصورة اختيارية حماية للخصوصية)", color = Color.White, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Address details
        OutlinedTextField(
            value = baseAddress,
            onValueChange = { baseAddress = it },
            label = { Text("عنوان الشارع والمقر الحالي (مثال: صنعاء - شارع الرباط)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = primaryColor
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Area details
        OutlinedTextField(
            value = areaCircle,
            onValueChange = { areaCircle = it },
            label = { Text("المنطقة ومربع السكن الدائم (مثال: مديرية معين - الدائري)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = primaryColor
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Profile Picture Picker Dialog
        if (showProfilePicDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showProfilePicDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedGender == "female") "إرفاق صورة الخدمة/الصورة المهنية" else "إرفاق الصورة الشخصية (Selfie)",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Button(
                            onClick = {
                                try {
                                    profileCameraLauncher.launch(null)
                                } catch (e: Exception) {
                                    mockProfilePicName = "selfie_camera_${System.currentTimeMillis().toString().takeLast(4)}.jpg"
                                    Toast.makeText(context, "تم التقاط سيلفي مباشر من كاميرا الهاتف بنجاح 📷", Toast.LENGTH_SHORT).show()
                                }
                                showProfilePicDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C35)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Camera", tint = primaryColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("التقاط صورة سيلفي عبر الكاميرا 📷", color = Color.White, fontSize = 11.sp)
                        }
                        
                        Button(
                            onClick = {
                                try {
                                    profileGalleryLauncher.launch("image/*")
                                } catch (e: Exception) {
                                    mockProfilePicName = "gallery_upload_${System.currentTimeMillis().toString().takeLast(4)}.png"
                                    Toast.makeText(context, "تم اختيار صورة من ذاكرة ومعرض الهاتف بنجاح 📂", Toast.LENGTH_SHORT).show()
                                }
                                showProfilePicDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C35)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.AccountBox, contentDescription = "Gallery", tint = primaryColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تحميل من ألبوم ومعرض صور الهاتف 📂", color = Color.White, fontSize = 11.sp)
                        }

                        if (selectedGender == "female") {
                            Button(
                                onClick = {
                                    mockProfilePicName = "profession_symbol_${System.currentTimeMillis().toString().take(4)}.png"
                                    showProfilePicDialog = false
                                    Toast.makeText(context, "تم تعيين صورة رمزية تعبيرية محترمة ترمز للمهنة بنجاح 🌸", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF382C35)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = "Profession", tint = Color.Magenta)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تعيين صورة مهنية تعبيرية (للحفاظ على الخصوصية) 🌸", color = Color.White, fontSize = 11.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = { showProfilePicDialog = false }) {
                            Text("إلغاء", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // ID Card Picker Dialog
        if (showIDPicDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showIDPicDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "إرفاق بطاقة الهوية الذكية أو جواز السفر",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Button(
                            onClick = {
                                try {
                                    idCameraLauncher.launch(null)
                                } catch (e: Exception) {
                                    mockIDCardName = "id_camera_snap_${System.currentTimeMillis().toString().takeLast(4)}.jpg"
                                    Toast.makeText(context, "تم تصوير الهوية وتمريرها بنجاح 📷", Toast.LENGTH_SHORT).show()
                                }
                                showIDPicDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C35)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Camera", tint = primaryColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("التقاط صورة الهوية عبر كاميرا الهاتف 📷", color = Color.White, fontSize = 11.sp)
                        }
                        
                        Button(
                            onClick = {
                                try {
                                    idGalleryLauncher.launch("image/*")
                                } catch (e: Exception) {
                                    mockIDCardName = "id_gallery_snap_${System.currentTimeMillis().toString().takeLast(4)}.png"
                                    Toast.makeText(context, "تم اختيار كود الهوية من الألبوم 📂", Toast.LENGTH_SHORT).show()
                                }
                                showIDPicDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C35)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.AccountBox, contentDescription = "Gallery", tint = primaryColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تحميل من ألبوم ومعرض صور الهواتف 📂", color = Color.White, fontSize = 11.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = { showIDPicDialog = false }) {
                            Text("إلغاء", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Image file attach simulation blocks
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { showProfilePicDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2B33)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Person, contentDescription = "Camera")
                Spacer(modifier = Modifier.width(4.dp))
                val btnText = if (mockProfilePicName.isEmpty()) {
                    if (selectedGender == "female") "إرفاق صورة مهنية 🌸" else "إرفاق صورة شخصية 📸"
                } else "تم الإرفاق ✔"
                Text(btnText, fontSize = 10.sp)
            }

            Button(
                onClick = { showIDPicDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2B33)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AccountBox, contentDescription = "ID Card")
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (mockIDCardName.isEmpty()) "إرفاق بطاقة الهوية" else "تم الإرفاق ✔", fontSize = 10.sp)
            }
        }

        if (feedbackError.isNotEmpty()) {
            Text(feedbackError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.registerServiceProvider(
                    fullName, phoneNumber, mainCatSelectedId, subCatSelectedId, baseAddress, areaCircle, selectedGender, mockProfilePicName, mockIDCardName, 15.3694, 44.1910,
                    onSuccess = {
                        Toast.makeText(context, "تم رفع طلبك بنجاح! وسوف يتفحصه الفريق المالي المالك للموافقة.", Toast.LENGTH_LONG).show()
                        navController.navigate("home") { popUpTo("home") { inclusive = true } }
                    },
                    onError = { err -> feedbackError = err }
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("submit_registration_request_btn")
        ) {
            Text("تقديم طلب الانضمام للمراجعة الفورية", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

// --- GENERAL & SUPER ADMIN STANDARD LOGIN PANEL ---
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AppViewModel,
    primaryColor: Color
) {
    val context = LocalContext.current
    var inputUser by remember { mutableStateOf("") }
    var inputPass by remember { mutableStateOf("") }
    var saveSessionChecked by remember { mutableStateOf(false) }
    var errorFeedback by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
            .testTag("login_screen_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Admin Security",
            tint = primaryColor,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "تسجيل دخول مشرف خدمات اليمن",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "يرجى كتابة اسم المشرف وكلمة المرور للدخول للوحة التحكم.",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = inputUser,
            onValueChange = { inputUser = it },
            label = { Text("اسم المستخدم (Username)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = primaryColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_username_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = inputPass,
            onValueChange = { inputPass = it },
            label = { Text("رقم كلمة المرور السرية (Password)") },
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = primaryColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = saveSessionChecked,
                onCheckedChange = { saveSessionChecked = it },
                colors = CheckboxDefaults.colors(checkedColor = primaryColor)
            )
            Text("تذكر معلوماتي لدخول تلقائي لاحقاً", color = Color.LightGray, fontSize = 11.sp)
        }

        if (errorFeedback.isNotEmpty()) {
            Text(errorFeedback, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val ok = viewModel.attemptLogin(inputUser, inputPass, saveSessionChecked)
                if (ok) {
                    Toast.makeText(context, "تم تسجيل الدخول بنجاح!", Toast.LENGTH_SHORT).show()
                    navController.navigate("admin_dashboard") {
                        popUpTo("home")
                    }
                } else {
                    errorFeedback = "خطأ! اسم المستخدم أو كلمة المرور غير مطابقة للمواصفات الفنية."
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("login_submit_btn")
        ) {
            Text("تسجيل الدخول الفوري للوحة الإدارة", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { /* Disabled */ }) {
            Text("هل نسيت كلمة المرور؟ (تواصل مع المالك الفائق الكلي للأنظمة)", color = Color.Gray, fontSize = 10.sp)
        }
    }
}

// --- MASTER COMPREHENSIVE ADMIN DASHBOARD SCREEN ---
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    viewModel: AppViewModel,
    primaryColor: Color
) {
    val currentLang by viewModel.currentLanguage.collectAsState()
    val adminSession by viewModel.adminSession.collectAsState()
    val isBackdoorActive by viewModel.isBackdoorActive.collectAsState()
    val settings by viewModel.settingsFlow.collectAsState(initial = null)

    val pendingList by viewModel.pendingServiceProviders.collectAsState(initial = emptyList())
    val allProvidersList by viewModel.allServiceProviders.collectAsState(initial = emptyList())
    val categoriesList by viewModel.categories.collectAsState(initial = emptyList())
    val reportsList by viewModel.reports.collectAsState(initial = emptyList())
    val activityLogsList by viewModel.activityLogs.collectAsState(initial = emptyList())

    // Tabs inside Admin Dashboard (Filtered by authorization levels)
    var activeTabIdx by remember { mutableStateOf(0) }
    val dashboardTabs = remember(isBackdoorActive, adminSession) {
        val list = mutableListOf("الطلبات المعلقة", "الأقسام", "مزودو الخدمات", "الإعدادات العامة", "الشكاوى")
        if (isBackdoorActive || adminSession == "Owner" || adminSession == "WAM2026") {
            list.add("إدارة المشرفين")
            list.add("إعدادات المحادثات والأيقونات")
            list.add("سجل النشاطات السرية")
        }
        list
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_dashboard_view"),
        topBar = {
            Surface(
                color = Color(0xFF1E1E22),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBackdoorActive) "👑 لوحة تحكم المالك الفوقية السحرية" else "⚙️ لوحة الإدارة العامة للمنسق WAM",
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            fontSize = 15.sp
                        )
                        Row {
                            Text(
                                text = "حساب: $adminSession",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "تسجيل الخروج",
                                color = Color.Red,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.logout()
                                        navController.navigate("home") { popUpTo("home") { inclusive = true } }
                                    }
                                    .testTag("admin_logout_btn")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scrollable Horizontal Tab Selector
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(dashboardTabs) { index, tabName ->
                            val active = activeTabIdx == index
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (active) primaryColor else Color(0xFF26262D),
                                contentColor = if (active) Color.Black else Color.White,
                                modifier = Modifier.clickable { activeTabIdx = index }
                            ) {
                                Text(
                                    text = tabName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121214))
                .padding(padValues)
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 📊 STATS PANEL STRIP
                val activeUsersCount = allProvidersList.size * 12 + 154
                val callCount = settings?.cumulativeCallsCount ?: 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Active Users Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1F)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("👥 مستخدم نشط", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$activeUsersCount",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Providers Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1F)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🛠️ مقدم خدمة", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${allProvidersList.size}",
                                color = primaryColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Calls Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1F)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📞 مكالمات الدليل", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$callCount",
                                color = Color.Green,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    val currentTabName = dashboardTabs.getOrNull(activeTabIdx) ?: ""
                    when (currentTabName) {
                        "الطلبات المعلقة" -> PendingRequestsTabSection(pendingList = pendingList, viewModel = viewModel, primaryColor = primaryColor)
                        "الأقسام" -> CategoryManagementTabSection(categoriesList = categoriesList, viewModel = viewModel, primaryColor = primaryColor)
                        "مزودو الخدمات" -> ServiceProvidersAdminTabSection(viewModel = viewModel, primaryColor = primaryColor)
                        "الإعدادات العامة" -> AppSettingsAdminTabSection(viewModel = viewModel, primaryColor = primaryColor, settings = settings)
                "الشكاوى" -> ComplaintsReportsTabSection(reportsList = reportsList, viewModel = viewModel, primaryColor = primaryColor)
                "إدارة المشرفين" -> ModeratorsTabSection(viewModel = viewModel, primaryColor = primaryColor)
                "إعدادات المحادثات والأيقونات" -> AdvancedChatIconSettingsTabSection(viewModel = viewModel, primaryColor = primaryColor, settings = settings)
                "سجل النشاطات السرية" -> ActivityLogsTabSection(activityLogs = activityLogsList)
                else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("غير مصرح لك.", color = Color.Gray) }
            }
        }
    }
}
}
}

// 1. PENDING REQUESTS TAB (PREVIEW PICTURES + ZOOM + REASON DISMISSAL)
@Composable
fun PendingRequestsTabSection(
    pendingList: List<ServiceProvider>,
    viewModel: AppViewModel,
    primaryColor: Color
) {
    var rejectProviderId by remember { mutableStateOf<String?>(null) }
    var rejectionReasonText by remember { mutableStateOf("") }
    
    // Zoom images overlay
    var zoomImageUrl by remember { mutableStateOf<String?>(null) }

    if (pendingList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد أي طلبات انضمام جديدة معلقة بانتظار الاعتماد.", color = Color.Gray, fontSize = 12.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(pendingList) { p ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "مقدم الطلب: ${p.name}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text(text = "رقم الموبايل: ${p.phone}", color = Color.LightGray, fontSize = 11.sp)
                        Text(text = "العنوان: ${p.workAddress} (${p.residenceArea})", color = Color.Gray, fontSize = 10.sp)
                        Text(text = "الجنس: ${if (p.gender == "female") "أنثى" else "ذكر"}", color = Color.Gray, fontSize = 10.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        // Documents Attach previews (zoomable via overlays)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(Color.DarkGray)
                                    .clickable { zoomImageUrl = "https://example.com/avatar/${p.id}" }
                            ) {
                                Text("صورة شخصية\n(انقر لتكبير)", color = Color.White, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
                            }

                            if (p.idCardImage != null) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(Color.DarkGray)
                                        .clickable { zoomImageUrl = "https://example.com/idcard/${p.id}" }
                                ) {
                                    Text("بطاقة الهوية\n(انقر لتكبير)", color = Color.White, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Controls: Approve / Reject
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.adminApproveRequest(p.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("موافقة واعتماد ✔", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { rejectProviderId = p.id },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("رفض وتوضيح ❌", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Zoom Overlay Modal
    if (zoomImageUrl != null) {
        Dialog(onDismissRequest = { zoomImageUrl = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Zoomed doc photo icon mockup", tint = Color.LightGray, modifier = Modifier.size(80.dp))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("📷 معاينة مستند المستندات بصيغة مقربة أوتوماتيكية", color = Color.White, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(onClick = { zoomImageUrl = null }) {
                        Text("إغلاق المعاينة المكبرة", color = Color.Yellow)
                    }
                }
            }
        }
    }

    // Rejection prompt popup
    if (rejectProviderId != null) {
        Dialog(onDismissRequest = { rejectProviderId = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF222228)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("تحديد سبب الرفض الإلزامي:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rejectionReasonText,
                        onValueChange = { rejectionReasonText = it },
                        placeholder = { Text("مثال: صورة بطاقة الهوية قديمة وغير واضحة للتفتيش") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = primaryColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = { rejectProviderId = null }) {
                            Text("رجوع", color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                if (rejectionReasonText.isNotBlank()) {
                                    viewModel.adminRejectRequest(rejectProviderId!!, rejectionReasonText)
                                    rejectProviderId = null
                                    rejectionReasonText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("تأكيد الرفض")
                        }
                    }
                }
            }
        }
    }
}

// 2. CATEGORY CONFIGURATION TAB
@Composable
fun CategoryManagementTabSection(
    categoriesList: List<Category>,
    viewModel: AppViewModel,
    primaryColor: Color
) {
    var newCatAr by remember { mutableStateOf("") }
    var newCatEn by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<String?>(null) } // if null is main

    val mainCategories = categoriesList.filter { it.parentCategoryId == null }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1F)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("📬 إضافة وتأسيس قسم جديد بالكامِل:", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newCatAr,
                    onValueChange = { newCatAr = it },
                    label = { Text("الاسم باللغة العربية") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = newCatEn,
                    onValueChange = { newCatEn = it },
                    label = { Text("الاسم باللغة الإنجليزية") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("جعل هذا القسم يتبع كتخصص تحت تخصص آخر:", color = Color.Gray, fontSize = 10.sp)
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    val rootNames = listOf("الكل الرئيسي") + mainCategories.map { it.nameAr }
                    rootNames.forEachIndexed { idx, name ->
                        val targetId = if (idx == 0) null else mainCategories[idx - 1].id
                        val active = selectedParentId == targetId
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (active) primaryColor else Color.DarkGray,
                            contentColor = if (active) Color.Black else Color.White,
                            modifier = Modifier
                                .clickable { selectedParentId = targetId }
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(name, fontSize = 9.sp, modifier = Modifier.padding(6.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (newCatAr.isNotBlank() && newCatEn.isNotBlank()) {
                            viewModel.adminAddNewCategory(newCatAr, newCatEn, 99, selectedParentId)
                            newCatAr = ""
                            newCatEn = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إضافة وحفظ القسم", color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("📋 هيكل التصنيفات المتوفر حالياً وقابلية الحذف:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(mainCategories) { main ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF17171B))) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "📁 " + main.nameAr + " (${main.nameEn})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.Red,
                                modifier = Modifier
                                    .clickable { viewModel.adminDeleteCategory(main.id) }
                                    .size(16.dp)
                            )
                        }

                        // Sub items
                        val childs = categoriesList.filter { it.parentCategoryId == main.id }
                        childs.forEach { child ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "↳ " + child.nameAr + " (${child.nameEn})", color = Color.LightGray, fontSize = 11.sp)
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Sub",
                                    tint = Color.LightGray,
                                    modifier = Modifier
                                        .clickable { viewModel.adminDeleteCategory(child.id) }
                                        .size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. SERVICE PROVIDER EDITING MANAGEMENT TAB
@Composable
fun ServiceProvidersAdminTabSection(
    viewModel: AppViewModel,
    primaryColor: Color
) {
    val allProviders by viewModel.allServiceProviders.collectAsState(initial = emptyList())
    var manualName by remember { mutableStateOf("") }
    var manualPhone by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1F)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("➕ تسجيل مقدم خدمة ميكانيكي/يدوي مباشر (بدون مراجعة):", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualName,
                    onValueChange = { manualName = it },
                    label = { Text("الاسم الكامل") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = manualPhone,
                    onValueChange = { manualPhone = it },
                    label = { Text("رقم الموبايل") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (manualName.isNotBlank() && manualPhone.isNotBlank()) {
                            viewModel.adminInsertProviderManually(manualName, manualPhone, "1", "1_1", "صنعاء - محدد يدوياً", "قطاع معيّن")
                            manualName = ""
                            manualPhone = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تسجيل وحفظ مباشر كعضو نشط", color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("📋 جميع منتسبي الدليل والتحكم بالملفات الترويجية وحضرها:", color = Color.White, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(allProviders) { provider ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(provider.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                Text("الحالة: ${provider.status}, الهاتف: ${provider.phone}", color = Color.LightGray, fontSize = 10.sp)
                            }
                            IconButton(onClick = { viewModel.adminDeleteProvider(provider.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Record", tint = Color.Red)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Multi toggle row properties
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Verify trigger
                            val vColor = if (provider.isVerified) Color.Cyan else Color.Gray
                            Button(
                                onClick = { viewModel.adminVerifyProvider(provider.id, !provider.isVerified) },
                                colors = ButtonDefaults.buttonColors(containerColor = vColor),
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                            ) {
                                Text(if (provider.isVerified) "إلغاء التوثيق" else "توثيق بالدليل", color = Color.Black, fontSize = 9.sp)
                            }

                            // Pin trigger
                            val pColor = if (provider.isPinned) Color.Yellow else Color.Gray
                            Button(
                                onClick = { viewModel.adminPinProvider(provider.id, !provider.isPinned) },
                                colors = ButtonDefaults.buttonColors(containerColor = pColor),
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                            ) {
                                Text(if (provider.isPinned) "إلغاء التثبيت" else "تثبيت للمركز", color = Color.Black, fontSize = 9.sp)
                            }

                            // Boost Subscription trigger
                            val sColor = if (provider.isSubscribed) Color.Green else Color.Gray
                            Button(
                                onClick = { viewModel.adminBoostSubscription(provider.id, !provider.isSubscribed) },
                                colors = ButtonDefaults.buttonColors(containerColor = sColor),
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                            ) {
                                Text(if (provider.isSubscribed) "إلغاء التميز" else "تميز اشتراكي", color = Color.Black, fontSize = 9.sp)
                            }

                            // Ban Block trigger
                            val bColor = if (provider.isBanned) Color.Red else Color.LightGray
                            Button(
                                onClick = { viewModel.adminBanProvider(provider.id, !provider.isBanned) },
                                colors = ButtonDefaults.buttonColors(containerColor = bColor),
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                            ) {
                                Text(if (provider.isBanned) "فك الحظر" else "حظر من العمل", color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 4. APP SETTINGS TAB (PRESETS EMERALD/GOLD/SILVER + MAINTENANCE + ADVANCED DATA)
@Composable
fun AppSettingsAdminTabSection(
    viewModel: AppViewModel,
    primaryColor: Color,
    settings: AppSettings?
) {
    val context = LocalContext.current
    var inputAppName by remember { mutableStateOf(settings?.appName ?: "") }
    var inputFooterText by remember { mutableStateOf(settings?.footerText ?: "") }
    var inputWelcome by remember { mutableStateOf(settings?.welcomeMessage ?: "") }
    var maintenanceOn by remember { mutableStateOf(settings?.isMaintenanceMode ?: false) }
    var maintenanceMsg by remember { mutableStateOf(settings?.maintenanceMessage ?: "") }

    LaunchedEffect(settings) {
        if (settings != null) {
            inputAppName = settings.appName
            inputFooterText = settings.footerText
            inputWelcome = settings.welcomeMessage
            maintenanceOn = settings.isMaintenanceMode
            maintenanceMsg = settings.maintenanceMessage
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        Text("🎨 تخصيص هوية وألوان الطابع الشامل للعلامة:", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))

        // Preset theme buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.setThemeColorPreset("Cosmic Silver") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E8A9F)),
                modifier = Modifier.weight(1f)
            ) {
                Text("المجرة 🌌", color = Color.Black, fontSize = 10.sp)
            }

            Button(
                onClick = { viewModel.setThemeColorPreset("Luxury Gold") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                modifier = Modifier.weight(1f)
            ) {
                Text("الذهبي ✨", color = Color.Black, fontSize = 10.sp)
            }

            Button(
                onClick = { viewModel.setThemeColorPreset("Emerald Green") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E8B57)),
                modifier = Modifier.weight(1f)
            ) {
                Text("الأخضر 🟢", color = Color.Black, fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // General settings Outlined text inputs
        OutlinedTextField(
            value = inputAppName,
            onValueChange = { inputAppName = it },
            label = { Text("تغيير اسم التطبيق الكامِل") },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = inputFooterText,
            onValueChange = { inputFooterText = it },
            label = { Text("تخصيص نص تذييل أسفل الشاشات") },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = inputWelcome,
            onValueChange = { inputWelcome = it },
            label = { Text("تغيير رسالة الترحيب الرئيسية") },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Maintenance section toggle
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF241C1C))) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🚧 تفعيل وضع الصيانة الشاملة:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Switch(
                        checked = maintenanceOn,
                        onCheckedChange = { maintenanceOn = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Red)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = maintenanceMsg,
                    onValueChange = { maintenanceMsg = it },
                    label = { Text("رسالة الصيانة التي تعرض للجمهور") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (settings != null) {
                    val updated = settings.copy(
                        appName = inputAppName,
                        footerText = inputFooterText,
                        welcomeMessage = inputWelcome,
                        isMaintenanceMode = maintenanceOn,
                        maintenanceMessage = maintenanceMsg
                    )
                    viewModel.insertSettings(updated)
                    Toast.makeText(context, "تم حفظ الإعدادات بنجاح!", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("حفظ التغييرات الفورية الشاملة ✔", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// 5. REPORTS AND COMPLAINTS SUBMISSIONS TAB
@Composable
fun ComplaintsReportsTabSection(
    reportsList: List<Report>,
    viewModel: AppViewModel,
    primaryColor: Color
) {
    val context = LocalContext.current
    
    fun exportReportPDF() {
        Toast.makeText(context, "تم تصدير ملف تقارير الشكاوى وحقنها بملف PDF بنجاح في مجلد Downloads لتنزيل المالك.", Toast.LENGTH_LONG).show()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📝 بلاغات مستخدمي الدليل والشكاوى المرفوعة (${reportsList.size}):", color = Color.White, fontSize = 12.sp)
            Button(
                onClick = { exportReportPDF() },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("تصدير CSV/PDF 📄", color = Color.Black, fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (reportsList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("نظيف! لم يتم تسجيل أي بلاغات أو مخالفات ضد مقدمي الخدمة حتى اللحظة.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reportsList) { r ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF221A1A))) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("المبلغ عنه: ${r.providerName}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Gray,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { viewModel.adminDeleteReport(r.id) }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("الرافع: ${r.reporterName} (${r.reporterPhone})", color = Color.LightGray, fontSize = 10.sp)
                            Text("تفاصيل الشكوى المسجلة: ${r.details}", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

// 6. OWNER ADMIN ACTIVITY LOGS TAB
@Composable
fun ActivityLogsTabSection(activityLogs: List<ActivityLog>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("🕵️ سجل التدقيق للأحداث والعمليات الإدارية الكاملة (خاص بالمالك):", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(activityLogs) { log ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1F))) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "الفاعل: ${log.adminName}", fontWeight = FontWeight.Bold, color = Color.Green, fontSize = 11.sp)
                            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                            Text(text = dateStr, color = Color.Gray, fontSize = 9.sp)
                        }
                        Text(text = "الحدث: ${log.action}", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 11.sp)
                        Text(text = "التفاصيل الفنية: ${log.details}", color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

// --- ABOUT SCREEN SCREEN ---
@Composable
fun AboutScreen(
    navController: NavController,
    viewModel: AppViewModel,
    primaryColor: Color
) {
    val settings by viewModel.settingsFlow.collectAsState(initial = null)
    val supportPhone = settings?.supportPhone ?: "777644670"
    val supportEmail = settings?.supportEmail ?: "support@wam.ye"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(primaryColor),
            contentAlignment = Alignment.Center
        ) {
            Text("WAM", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = settings?.appName ?: "WAM اليمن",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "الإصدار الحالي: v2.6-Live Stable",
            fontSize = 11.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "وصف تطبيق خدمات اليمن:",
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "دليل تطبيق خدمات اليمن WAM هو منصة ريادية محلية تعمل بكفاءة تامة دون الحاجة للاتصال الدائم بالإنترنت (Offline-First) بهدف ربط الفنيين المهرة للكهرباء والسباكة والتمريض في عموم الأراضي اليمنية بأصحاب الاحتياج بشكل فوري وآمن.",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "📞 اتصل بنا والدعم الطارئ المباشر:",
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• خط الدعم والمساعدة: $supportPhone", color = Color.White, fontSize = 12.sp)
                Text("• البريد الإلكتروني المعتمد: $supportEmail", color = Color.White, fontSize = 12.sp)
                Text("• رقم واتساب المالك: ${settings?.supportWhatsApp ?: "777644670"}", color = Color.White, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("العودة للخلف", color = Color.Black)
        }
    }
}

// --- FLOATING AI CONVERSATION WINDOW WIDGET ---
@Composable
fun AIDialogWidget(
    viewModel: AppViewModel,
    primaryColor: Color,
    onClose: () -> Unit
) {
    val chatHistory by viewModel.aiChatMessages.collectAsState()
    val isAiLoading by viewModel.isAILoading.collectAsState()
    var inputQuery by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    // Scroll to bottom on updates
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            lazyListState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Dialog(onDismissRequest = onClose) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(460.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header of AI Drawer chat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Face, contentDescription = "Robot", tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مساعد دليل خدمات اليمن الذكي", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Divider(color = Color(0xFF2E2E35), modifier = Modifier.padding(vertical = 8.dp))

                // Mid: Dialogue Bubbles List
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chatHistory) { msg ->
                            val isUser = msg.isUser
                            val align = if (isUser) Alignment.End else Alignment.Start
                            val bColor = if (isUser) primaryColor else Color(0xFF2C2C35)
                            val tColor = if (isUser) Color.Black else Color.White

                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = bColor,
                                    contentColor = tColor,
                                    modifier = Modifier.widthIn(max = 210.dp)
                                ) {
                                    Text(
                                        text = msg.text,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }

                        if (isAiLoading) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = primaryColor)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("يفكر المساعد الذكي بالخدمات الطارئة...", color = Color.Gray, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom: Send bar inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputQuery,
                        onValueChange = { inputQuery = it },
                        placeholder = { Text("اطرح استفسار أو تخصص عمل...", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = primaryColor
                        ),
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputQuery.isNotBlank()) {
                                viewModel.submitAIChat(inputQuery)
                                inputQuery = ""
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(primaryColor, CircleShape)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// --- SYSTEM UTILS INTEGRATIONS (CALL, WHATSAPP SHARES) ---
fun triggerPhoneCall(context: android.content.Context, phoneNumber: String, viewModel: AppViewModel? = null) {
    try {
        viewModel?.incrementCallCount()
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "عذراً، تعذر إجراء المكالمة على هذا المحاكي.", Toast.LENGTH_SHORT).show()
    }
}

fun triggerWhatsAppChat(context: android.content.Context, phone: String, message: String) {
    var cleanPhone = phone
    if (cleanPhone.startsWith("7")) {
        cleanPhone = "967$cleanPhone" // Add Yemen country code
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "عذراً، لم نجد تطبيق واتساب مثبت على هذا الجهاز.", Toast.LENGTH_SHORT).show()
    }
}

// Category design icons mapped vector helpers
fun getCategoryVector(nameEn: String): ImageVector {
    return when {
        nameEn.contains("Electric", ignoreCase = true) -> Icons.Default.Star
        nameEn.contains("Plumb", ignoreCase = true) -> Icons.Default.Place
        nameEn.contains("Repair", ignoreCase = true) -> Icons.Default.Build
        nameEn.contains("Medical", ignoreCase = true) -> Icons.Default.Place
        nameEn.contains("Security", ignoreCase = true) -> Icons.Default.Lock
        nameEn.contains("AC", ignoreCase = true) -> Icons.Default.Refresh
        nameEn.contains("Tube", ignoreCase = true) -> Icons.Default.Settings
        else -> Icons.Default.Menu
    }
}

// Dynamic symbol mapping for custom overlay icon styles
fun mapSymbolToIcon(symbol: String): ImageVector {
    return when (symbol) {
        "Face" -> Icons.Default.Face
        "Star" -> Icons.Default.Star
        "Info" -> Icons.Default.Info
        "Build" -> Icons.Default.Build
        "Mail" -> Icons.Default.Email
        "Lock" -> Icons.Default.Lock
        "Menu" -> Icons.Default.Menu
        "Warning" -> Icons.Default.Warning
        "Search" -> Icons.Default.Search
        "Settings" -> Icons.Default.Settings
        "Person" -> Icons.Default.Person
        "Add" -> Icons.Default.Add
        else -> Icons.Default.Face
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveChatHubDialog(
    viewModel: AppViewModel,
    primaryColor: Color,
    onClose: () -> Unit
) {
    val settings by viewModel.settingsFlow.collectAsState(initial = null)
    val providers by viewModel.allServiceProviders.collectAsState(initial = emptyList())
    val allMessages by viewModel.allChatMessagesFlow.collectAsState(initial = emptyList())
    val adminSession by viewModel.adminSession.collectAsState()
    val isUserAdmin = adminSession != null

    var selectedTab by remember { mutableStateOf(if (isUserAdmin) 2 else 0) }
    var activeChatId by remember { mutableStateOf<String?>(null) }
    var activeChatReceiverName by remember { mutableStateOf("") }
    var activeChatReceiverId by remember { mutableStateOf("") }

    var draftText by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF151518),
            modifier = Modifier
                .fillMaxWidth()
                .height(580.dp)
                .padding(8.dp)
                .border(1.dp, Color(0xFF2C2C35), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Chat Logo",
                            tint = primaryColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "بوابة المحادثات الفورية WAM",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("✕", color = Color.Gray, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Global Alert Banner if disabled
                if (settings?.isChatServiceDisabled == true) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFB3261E)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = settings?.chatServiceDisabledReason ?: "من فضلكم، خدمة المراسلة معطلة مؤقتاً لأعمال التحديث.",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // If inside a specific active chat, render the chat window
                val currentChatId = activeChatId
                if (currentChatId != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                activeChatId = null // go back to tabs
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "العودة للقائمة / مراسلة: $activeChatReceiverName",
                            color = primaryColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val chatHistory = allMessages.filter { it.chatId == currentChatId }.sortedBy { it.timestamp }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF232329), RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F0F12))
                            .padding(8.dp)
                    ) {
                        if (chatHistory.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "لا توجد رسائل سابقة. ابدأ المحادثة الآن!",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(chatHistory) { msg ->
                                    val isMe = msg.senderId == "user" || (isUserAdmin && msg.senderId == "admin")
                                    val bubbleBg = if (isMe) primaryColor else Color(0xFF2C2C35)
                                    val textColor = if (isMe) Color.Black else Color.White
                                    val align = if (isMe) Alignment.End else Alignment.Start

                                    val dateFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                    val dateStr = dateFormat.format(java.util.Date(msg.timestamp))

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalAlignment = align
                                    ) {
                                        Surface(
                                            color = bubbleBg,
                                            shape = RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isMe) 12.dp else 0.dp,
                                                bottomEnd = if (isMe) 0.dp else 12.dp
                                            ),
                                            modifier = Modifier.widthIn(max = 240.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = msg.message,
                                                    color = textColor,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = dateStr,
                                                    color = if (isMe) Color(0x99000000) else Color.Gray,
                                                    fontSize = 9.sp,
                                                    textAlign = TextAlign.End,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input bottom bar
                    val isSendingBlocked = settings?.isChatServiceDisabled == true && !isUserAdmin
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = draftText,
                            onValueChange = { draftText = it },
                            placeholder = { Text(if (isSendingBlocked) "الإرسال معطل بقرار إداري" else "اكتب رسالتك وتفاصيل طلبك هنا...", color = Color.Gray, fontSize = 12.sp) },
                            enabled = !isSendingBlocked,
                            maxLines = 3,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF2C2C35),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0F0F12),
                                unfocusedContainerColor = Color(0xFF0F0F12)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (draftText.isNotBlank()) {
                                    val sender = if (isUserAdmin) "admin" else "user"
                                    viewModel.sendLiveMessage(
                                        chatId = currentChatId,
                                        senderId = sender,
                                        receiverId = activeChatReceiverId,
                                        messageText = draftText
                                    )
                                    draftText = ""
                                }
                            },
                            enabled = draftText.isNotBlank() && !isSendingBlocked,
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("إرسال", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                } else {
                    // TAB NAVIGATION
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = primaryColor,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("المطور والدعم الفني", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("مقدمو الخدمة", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        if (isUserAdmin) {
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = { Text("مراقبة المحادثات (إداري)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE57373)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeChatId = "user_admin"
                                            activeChatReceiverName = "الدعم الفني والإدارة"
                                            activeChatReceiverId = "admin"
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .background(primaryColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Admin Support",
                                                tint = Color.Black
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "مراسلة إدارة ومطور دليل WAM",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "استفسارات، شكاوى، اقتراحات أو طلبات إضافة مشرفين ودعم تكنولوجي.",
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Forward arrow",
                                            tint = primaryColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            1 -> {
                                val approvedProviders = providers.filter { it.status == "approved" && !it.isBanned }
                                if (approvedProviders.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("ليس هناك مقدمو خدمات نشطون حالياً.", color = Color.Gray, fontSize = 13.sp)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(approvedProviders) { prov ->
                                            val isSuspended = prov.chatSuspended
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .background(if (isSuspended) Color.DarkGray else primaryColor, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = prov.name.take(1),
                                                            color = Color.Black,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            prov.name,
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            prov.workAddress,
                                                            color = Color.Gray,
                                                            fontSize = 11.sp
                                                        )
                                                        if (isSuspended) {
                                                            Text(
                                                                "🚫 خدمة المراسلة معطلة إدارياً عن مقدم الخدمة",
                                                                color = Color(0xFFE57373),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                    Button(
                                                        onClick = {
                                                            activeChatId = "user_${prov.id}"
                                                            activeChatReceiverName = prov.name
                                                            activeChatReceiverId = prov.id
                                                        },
                                                        enabled = !isSuspended,
                                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("مراسلة", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> {
                                val distinctChatIds = allMessages.map { it.chatId }.distinct()
                                if (distinctChatIds.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("لا توجد محادثات جارية ومسجلة في النظام.", color = Color.Gray, fontSize = 13.sp)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(distinctChatIds) { cid ->
                                            val mList = allMessages.filter { it.chatId == cid }.sortedByDescending { it.timestamp }
                                            val lastMsg = mList.firstOrNull()?.message ?: ""
                                            val recName = when {
                                                cid == "user_admin" -> "الدعم الفني والشكاوى (المستخدم <-> الإدارة)"
                                                cid.startsWith("user_") -> {
                                                    val idPart = cid.substringAfter("user_")
                                                    providers.find { it.id == idPart }?.name ?: "مقدم الخدمة (ID: $idPart)"
                                                }
                                                else -> "محادثة مجهولة ($cid)"
                                            }

                                            val recId = when {
                                                cid == "user_admin" -> "admin"
                                                cid.startsWith("user_") -> cid.substringAfter("user_")
                                                else -> "admin"
                                            }

                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF25252B)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        activeChatId = cid
                                                        activeChatReceiverName = recName
                                                        activeChatReceiverId = recId
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = recName,
                                                            color = primaryColor,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "الرسالة الأخيرة: $lastMsg",
                                                            color = Color.LightGray,
                                                            fontSize = 11.sp,
                                                            maxLines = 1,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowBack,
                                                        contentDescription = "Read",
                                                        tint = Color.LightGray,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ModeratorsTabSection(
    viewModel: AppViewModel,
    primaryColor: Color
) {
    val moderators by viewModel.moderators.collectAsState(initial = emptyList())
    val context = androidx.compose.ui.platform.LocalContext.current

    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    // Form to add a new Moderator
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1F)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "➕ إضافة مشرف جديد مع تخصيص المزامنة والصلاحيات:",
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = newUsername,
                onValueChange = { newUsername = it },
                label = { Text("اسم المستخدم (Username)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = primaryColor
                ),
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("كلمة المرور (Password)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = primaryColor
                ),
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    if (newUsername.isNotBlank() && newPassword.isNotBlank()) {
                        viewModel.addOrUpdateModerator(
                            Moderator(
                                username = newUsername.trim(),
                                password = newPassword.trim(),
                                role = "moderator",
                                canEditCategories = true,
                                canDeleteProviders = true,
                                canManageSettings = false,
                                isActive = true
                            )
                        )
                        Toast.makeText(context, "تم إضافة حساب المشرف الجديد '${newUsername}' بنجاح ومزامنته!", Toast.LENGTH_SHORT).show()
                        newUsername = ""
                        newPassword = ""
                    } else {
                        Toast.makeText(context, "يرجى كتابة اسم المشرف وكلمة المرور أولاً.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إضافة وحفظ المشرف", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }

    // List of existing moderators
    Text(
        "👥 قائمة المشرفين وصلاحياتهم وبترخيص فردي:",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )

    if (moderators.isEmpty()) {
        Box(
            modifier = Modifier
                .height(120.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("لا يوجد مشرفين إضافيين مسجلين حالياً.", color = Color.Gray, fontSize = 11.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.height(300.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(moderators) { mod ->
                var modPassword by remember(mod.password) { mutableStateOf(mod.password) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "👤 حساب المشرف: ${mod.username}",
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                fontSize = 13.sp
                            )
                            IconButton(
                                onClick = {
                                    viewModel.deleteModerator(mod.username)
                                    Toast.makeText(context, "تم حذف حساب المشرف وتجريده من الصلاحيات.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("🗑️", fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = modPassword,
                            onValueChange = { modPassword = it },
                            label = { Text("تعديل كلمة مرور حساب المشرف") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("التحكم بصلاحيات المشرف بشكل منفصل ومستقل:", color = Color.LightGray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Switch options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تعديل وإضافة الأقسام والتخصصات", color = Color.White, fontSize = 11.sp)
                            Switch(
                                checked = mod.canEditCategories,
                                onCheckedChange = { checked ->
                                    viewModel.addOrUpdateModerator(
                                        Moderator(
                                            username = mod.username,
                                            password = modPassword,
                                            role = mod.role,
                                            canEditCategories = checked,
                                            canDeleteProviders = mod.canDeleteProviders,
                                            canManageSettings = mod.canManageSettings,
                                            isActive = mod.isActive
                                        )
                                    )
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("حظر وحذف وثائق مقدمي الخدمات", color = Color.White, fontSize = 11.sp)
                            Switch(
                                checked = mod.canDeleteProviders,
                                onCheckedChange = { checked ->
                                    viewModel.addOrUpdateModerator(
                                        Moderator(
                                            username = mod.username,
                                            password = modPassword,
                                            role = mod.role,
                                            canEditCategories = mod.canEditCategories,
                                            canDeleteProviders = checked,
                                            canManageSettings = mod.canManageSettings,
                                            isActive = mod.isActive
                                        )
                                    )
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الولوج وتعديل الإعدادات الكونية للبرنامج", color = Color.White, fontSize = 11.sp)
                            Switch(
                                checked = mod.canManageSettings,
                                onCheckedChange = { checked ->
                                    viewModel.addOrUpdateModerator(
                                        Moderator(
                                            username = mod.username,
                                            password = modPassword,
                                            role = mod.role,
                                            canEditCategories = mod.canEditCategories,
                                            canDeleteProviders = mod.canDeleteProviders,
                                            canManageSettings = checked,
                                            isActive = mod.isActive
                                        )
                                    )
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الحساب مفعل ومصرّح بالعمل", color = Color.White, fontSize = 11.sp)
                            Switch(
                                checked = mod.isActive,
                                onCheckedChange = { checked ->
                                    viewModel.addOrUpdateModerator(
                                        Moderator(
                                            username = mod.username,
                                            password = modPassword,
                                            role = mod.role,
                                            canEditCategories = mod.canEditCategories,
                                            canDeleteProviders = mod.canDeleteProviders,
                                            canManageSettings = mod.canManageSettings,
                                            isActive = checked
                                        )
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                viewModel.addOrUpdateModerator(
                                    Moderator(
                                        username = mod.username,
                                        password = modPassword,
                                        role = mod.role,
                                        canEditCategories = mod.canEditCategories,
                                        canDeleteProviders = mod.canDeleteProviders,
                                        canManageSettings = mod.canManageSettings,
                                        isActive = mod.isActive
                                    )
                                )
                                Toast.makeText(context, "تم حفظ كلمة مرور المشرف '${mod.username}' وصلاحياته الجديدة بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("اعتماد وحفظ تعديلات هذا المشرف وبثها فوراً", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdvancedChatIconSettingsTabSection(
    viewModel: AppViewModel,
    primaryColor: Color,
    settings: AppSettings?
) {
    val providers by viewModel.allServiceProviders.collectAsState(initial = emptyList())
    val context = androidx.compose.ui.platform.LocalContext.current

    var reasonText by remember(settings?.chatServiceDisabledReason) { mutableStateOf(settings?.chatServiceDisabledReason ?: "") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Section 1: Global Live Chat switches
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1F))) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "🔒 التحكم بخدمة المراسلة المباشرة (على مستوى التطبيق):",
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تعطيل خدمة المحادثات الفورية مؤقتاً للعامة", color = Color.White, fontSize = 12.sp)
                        Switch(
                            checked = settings?.isChatServiceDisabled == true,
                            onCheckedChange = { checked ->
                                if (settings != null) {
                                    viewModel.insertSettings(
                                        settings.copy(isChatServiceDisabled = checked, chatServiceDisabledReason = reasonText)
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = {
                            reasonText = it
                            if (settings != null) {
                                viewModel.insertSettings(
                                    settings.copy(chatServiceDisabledReason = it)
                                )
                            }
                        },
                        label = { Text("سبب التعطيل المعروض للمستخدمين") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Section 1.5: Footer custom transparency, height and font size
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1F))) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "📐 التحكم بشفافية وحجم تغيير تذييل التطبيق (الفوتر):",
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val currentOpacity = settings?.footerOpacity ?: 1.0f
                    Text("مستوى شفافية الـ footer والأيقونات العائمة: ${(currentOpacity * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = currentOpacity,
                        onValueChange = { newValue ->
                            if (settings != null) {
                                viewModel.insertSettings(settings.copy(footerOpacity = newValue))
                            }
                        },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = primaryColor,
                            activeTrackColor = primaryColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val currentHeight = settings?.footerHeightScale ?: 56
                    Text("حجم ارتفاع التذييل كاملاً: ${currentHeight}dp", color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = currentHeight.toFloat(),
                        onValueChange = { newValue ->
                            if (settings != null) {
                                viewModel.insertSettings(settings.copy(footerHeightScale = newValue.toInt()))
                            }
                        },
                        valueRange = 40f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = primaryColor,
                            activeTrackColor = primaryColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val currentFontSize = settings?.footerFontSize ?: 12
                    Text("حجم خط كتابة التذييل (سند الدعم): ${currentFontSize}sp", color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = currentFontSize.toFloat(),
                        onValueChange = { newValue ->
                            if (settings != null) {
                                viewModel.insertSettings(settings.copy(footerFontSize = newValue.toInt()))
                            }
                        },
                        valueRange = 8f..24f,
                        colors = SliderDefaults.colors(
                            thumbColor = primaryColor,
                            activeTrackColor = primaryColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Section 2: Custom overlay icons styling with visual effects
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1F))) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "🎨 تخصيص مظهر الأيقونات العائمة والتأثيرات البصرية البراقة:",
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("أيقونة المساعد الذكي الاصطناعي (AI):", color = Color.White, fontSize = 11.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        listOf("Face", "Star", "Info", "Build", "Search").forEach { sym ->
                            val isSelected = (settings?.assistantIconSymbol ?: "Face") == sym
                            Box(
                                modifier = Modifier
                                    .background(if (isSelected) primaryColor else Color(0xFF2C2C32), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (settings != null) {
                                            viewModel.insertSettings(
                                                settings.copy(assistantIconSymbol = sym)
                                            )
                                        }
                                    }
                                    .padding(8.dp)
                            ) {
                                Text(sym, color = if (isSelected) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تفعيل تأثير الوهج والنبض لأيقونة الذكاء الاصطناعي", color = Color.White, fontSize = 11.sp)
                        Switch(
                            checked = settings?.assistantIconGlow == true,
                            onCheckedChange = { checked ->
                                if (settings != null) {
                                    viewModel.insertSettings(
                                        settings.copy(assistantIconGlow = checked)
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("أيقونة بوابة المراسلة الفورية المباشرة:", color = Color.White, fontSize = 11.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        listOf("Mail", "Lock", "Settings", "Menu", "Add").forEach { sym ->
                            val isSelected = (settings?.liveChatIconSymbol ?: "Mail") == sym
                            Box(
                                modifier = Modifier
                                    .background(if (isSelected) primaryColor else Color(0xFF2C2C32), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (settings != null) {
                                            viewModel.insertSettings(
                                                settings.copy(liveChatIconSymbol = sym)
                                            )
                                        }
                                    }
                                    .padding(8.dp)
                            ) {
                                Text(sym, color = if (isSelected) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تفعيل تأثير الوهج والنبض لبوابة المراسلات الفورية", color = Color.White, fontSize = 11.sp)
                        Switch(
                            checked = settings?.liveChatIconGlow == true,
                            onCheckedChange = { checked ->
                                if (settings != null) {
                                    viewModel.insertSettings(
                                        settings.copy(liveChatIconGlow = checked)
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("نوع التأثير الحركي البصري الإضافي للأزرار المفردة:", color = Color.White, fontSize = 11.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        listOf("Pulse", "Rotate", "None").forEach { fx ->
                            val isSelected = (settings?.iconVisualEffectType ?: "Pulse") == fx
                            Box(
                                modifier = Modifier
                                    .background(if (isSelected) primaryColor else Color(0xFF2C2C32), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (settings != null) {
                                            viewModel.insertSettings(
                                                settings.copy(iconVisualEffectType = fx)
                                            )
                                        }
                                    }
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = if (fx == "Pulse") "نبض هائل" else if (fx == "Rotate") "دوران مستمر" else "بدون حركات",
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Safe clean and permanently erasing chats
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1F))) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "🧹 حماية الخصوصية وسحق سجلات الدردشة نهائياً:",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "للحفاظ على سرية وخصوصية مستخدمي دليل خدمات اليمن، يمكنك مسح كافة الرسائل والمحادثات المتبادلة نهائياً بلمسة زر واحدة وبشكل لا يمكن استرجاعه.",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.wipeAllChatsPermanently()
                            Toast.makeText(context, "💥 تم بنجاح سحق ومسح كافة سجلات الدردشة من جميع الأجهزة نهائياً!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("مسح وسحق كافة المحادثات والرسائل نهائياً", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 4: Individual Service Provider Suspension controls
        item {
            Text(
                "🚨 إيقاف/سحب خدمة المراسلة الفورية عن مقدمي خدمات محددين:",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val approvedProviders = providers.filter { it.status == "approved" && !it.isBanned }
        if (approvedProviders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد حسابات نشطة حالياً لتعديل إيقاف المراسلات.", color = Color.Gray, fontSize = 11.sp)
                }
            }
        } else {
            items(approvedProviders) { prov ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(prov.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("رقم الموبايل: ${prov.phone} | المهنة: ${prov.mainCategory}", color = Color.Gray, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (prov.chatSuspended) "🚫 معطّلة" else "🟢 نشطة", color = if (prov.chatSuspended) Color.Red else Color.Green, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = prov.chatSuspended,
                                onCheckedChange = { checked ->
                                    viewModel.setProviderChatSuspended(prov.id, checked)
                                    Toast.makeText(context, "تم تحديث حالة المراسلة لمقدم الخدمة بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
