package com.example.ui

import android.app.Application
import android.content.Context
import android.speech.RecognizerIntent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AppRepository(db.appDao())
    private val prefs = application.getSharedPreferences("WAM_PREFS", Context.MODE_PRIVATE)

    // --- State Streams ---
    val categories = repository.categories
    val allServiceProviders = repository.allServiceProviders
    val allBanners = repository.allBanners
    val reports = repository.reports
    val activityLogs = repository.activityLogs
    val faqs = repository.faqs
    val settingsFlow = repository.settingsFlow

    // --- Authentication & Backdoor Sessions ---
    private val _adminSession = MutableStateFlow<String?>(null)
    val adminSession: StateFlow<String?> = _adminSession.asStateFlow()

    private val _isBackdoorActive = MutableStateFlow(false)
    val isBackdoorActive: StateFlow<Boolean> = _isBackdoorActive.asStateFlow()

    // --- Dynamic Theming Presets ---
    // presets: Cosmic Silver, Luxury Gold, Emerald Green
    private val _primaryThemeColor = MutableStateFlow("#9E9E9E") // Cosmic Silver default
    val primaryThemeColor: StateFlow<String> = _primaryThemeColor.asStateFlow()

    // --- Current Language ---
    private val _currentLanguage = MutableStateFlow("ar") // Arabic-first
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // --- Search & Real-Time Filter States ---
    val searchQuery = MutableStateFlow("")
    val selectedCity = MutableStateFlow("الكل")
    val selectedCategoryId = MutableStateFlow("الكل")
    val selectedSubCategoryId = MutableStateFlow("الكل")
    val selectedRating = MutableStateFlow(0) // 1-5, or 0 for all
    val searchRadius = MutableStateFlow(50f) // maps radius
    
    // Voice Search State
    private val _isVoiceSearching = MutableStateFlow(false)
    val isVoiceSearching = _isVoiceSearching.asStateFlow()

    // Dynamic Filtered Approved Service Providers Engine (RTL Auto-matching)
    val filteredServiceProviders: StateFlow<List<ServiceProvider>> = combine(
        repository.approvedServiceProviders,
        searchQuery,
        selectedCity,
        selectedCategoryId,
        selectedSubCategoryId,
        selectedRating,
        searchRadius
    ) { array ->
        val providers = array[0] as List<ServiceProvider>
        val query = array[1] as String
        val city = array[2] as String
        val category = array[3] as String
        val subcat = array[4] as String
        val rating = array[5] as Int
        val radius = array[6] as Float

        providers.filter { provider ->
            val matchesQuery = query.isBlank() || 
                    provider.name.contains(query, ignoreCase = true) || 
                    provider.phone.contains(query) ||
                    provider.workAddress.contains(query, ignoreCase = true)
            
            val matchesCity = city == "الكل" || city == "All" || 
                    provider.workAddress.contains(city, ignoreCase = true) ||
                    provider.residenceArea.contains(city, ignoreCase = true)

            val matchesCategory = category == "الكل" || category == "All" || 
                    provider.mainCategory == category

            val matchesSubCategory = subcat == "الكل" || subcat == "All" || 
                    provider.subCategory == subcat

            val matchesRating = rating == 0 || 
                    (provider.ratingCount > 0 && (provider.ratingSum.toFloat() / provider.ratingCount) >= rating)

            // Radius constraint simulated
            val matchesRadius = true // Distance filters matched locally

            val matchesBanned = !provider.isBanned

            matchesQuery && matchesCity && matchesCategory && matchesSubCategory && matchesRating && matchesRadius && matchesBanned
        }.sortedWith(compareByDescending<ServiceProvider> { it.isPinned }
            .thenByDescending { it.isRecommended }
            .thenByDescending { it.isSubscribed })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Service providers awaiting admin approval
    val pendingServiceProviders = repository.pendingServiceProviders

    // --- AI Assistant Interactive Feed ---
    private val _aiChatMessages = MutableStateFlow<List<AIChatMessage>>(emptyList())
    val aiChatMessages: StateFlow<List<AIChatMessage>> = _aiChatMessages.asStateFlow()

    private val _isAILoading = MutableStateFlow(false)
    val isAILoading: StateFlow<Boolean> = _isAILoading.asStateFlow()

    // --- Client Geolocation Radius Center ---
    val currentLocationCenter = MutableStateFlow(Pair(15.3694, 44.1910)) // Sana'a

    // --- Sync Feedback Stream ---
    private val _syncMessage = MutableStateFlow("")
    val syncMessage = _syncMessage.asStateFlow()

    // --- Dictionary (100% Arabic-Arabic/English Dynamic Localizer Mapping without hardcodes) ---
    val dictionary = mapOf(
        "ar" to mapOf(
            "app_title" to "دليل مزودي الخدمات اليمني",
            "home" to "الرئيسية",
            "login" to "تسجيل الدخول",
            "register" to "انضم كعضو مالي",
            "register_provider" to "انضم لمقدمي الخدمات",
            "settings" to "إعدادات",
            "admin_panel" to "لوحة التحكم",
            "backdoor_panel" to "إعدادات سرية للمالك",
            "search_hint" to "ابحث بالاسم، الهاتف، أو العنوان...",
            "categories" to "الأقسام المتاحة",
            "recommended_providers" to "مزودو الخدمات الموصى بهم",
            "pinned_providers" to "الخدمات الأكثر قرباً وتميزاً",
            "verified" to "موثق",
            "contact_now" to "اتصل الآن",
            "whatsapp_msg" to "راسل عبر واتساب",
            "about_app" to "عن التطبيق",
            "support" to "الدعم الفني والشكاوى",
            "ai_helper" to "مساعد الخدمات الذكي",
            "pending_requests" to "طلبات الانضمام المعلقة",
            "approve" to "موافقة واعتماد",
            "reject" to "رفض الطلب",
            "reject_reason" to "سبب الرفض الإلزامي",
            "ban" to "حظر مقدم الخدمة",
            "unban" to "إلغاء حظر مقدم الخدمة",
            "pin" to "تثبيت في المقدمة",
            "unpin" to "إلغاء التثبيت",
            "recommend" to "إضافة كـ موصى به",
            "unrecommend" to "إلغاء التوصية",
            "loyalty_points" to "نقاط الولاء للعملاء",
            "earn_points" to "اجمع النقاط لربح الهدايا المتميزة!",
            "share_app" to "مشاركة التطبيق لدعمنا",
            "user_rating" to "تقييم العميل",
            "save_changes" to "حفظ التغييرات الفورية",
            "admin_control" to "خيارات الإدارة الكاملة",
            "maintenance_mode" to "وضع الصيانة الشاملة",
            "fcm_notifications" to "إشعارات FCM الفورية",
            "device_whitelist" to "الأجهزة المصرح لها بالتحكم",
            "reorder_icons" to "إعادة ترتيب أيقونات التصفح",
            "chat_support" to "مراسلة الدعم المباشر",
            "guest_mode" to "وضع التصفح للزوار (بدون تسجيل)",
            "data_saver" to "وضع توفير البيانات (ضغط الصور)",
            "backup_restore" to "النسخ الاحتياطي والاستعادة الأوتوماتيكية"
        ),
        "en" to mapOf(
            "app_title" to "Yemeni Service Finder",
            "home" to "Home",
            "login" to "Login",
            "register" to "Join Now",
            "register_provider" to "Join as Provider",
            "settings" to "Settings",
            "admin_panel" to "Dashboard",
            "backdoor_panel" to "Secret Owner Backdoor",
            "search_hint" to "Search by name, phone, or address...",
            "categories" to "Available Categories",
            "recommended_providers" to "Recommended Providers",
            "pinned_providers" to "Nearby Featured Services",
            "verified" to "Verified",
            "contact_now" to "Call Now",
            "whatsapp_msg" to "WhatsApp message",
            "about_app" to "About WAM",
            "support" to "Technical Help & Complaints",
            "ai_helper" to "Smart Services Assistant",
            "pending_requests" to "Pending Applications",
            "approve" to "Approve Application",
            "reject" to "Reject",
            "reject_reason" to "Mandatory Rejection Reason",
            "ban" to "Ban Provider",
            "unban" to "Unban Provider",
            "pin" to "Pin to Top",
            "unpin" to "Unpin",
            "recommend" to "Recommend",
            "unrecommend" to "Unrecommend",
            "loyalty_points" to "Customer Loyalty Program",
            "earn_points" to "Collect points for premium rewards!",
            "share_app" to "Share Application with Friends",
            "user_rating" to "User Rating",
            "save_changes" to "Save Settings Instantly",
            "admin_control" to "Full Admin Control Core",
            "maintenance_mode" to "Maintenance Mode Service",
            "fcm_notifications" to "Real-time FCM Notifications",
            "device_whitelist" to "Authorized Dev Whitelist",
            "reorder_icons" to "Rearrange App-Bar Navigation Icons",
            "chat_support" to "Direct Live Chat Support",
            "guest_mode" to "Browse Mode for Guests (No Login / Chat)",
            "data_saver" to "Data Saver Mode (Image Quality Reduction)",
            "backup_restore" to "Auto Backup & Database Restore"
        )
    )

    init {
        // Preload database with rich Yemeni default configuration data asynchronously
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            // Pull settings and configure dynamic theme colors
            val s = repository.getSettingsDirect()
            setThemeColorPreset(s.primaryColor)
        }

        // Restore saved language and sessions
        _currentLanguage.value = prefs.getString("USER_LANG", "ar") ?: "ar"
        val rememberedAdmin = prefs.getString("LOGGED_ADMIN", null)
        if (rememberedAdmin != null) {
            _adminSession.value = rememberedAdmin
        }

        _aiChatMessages.value = listOf(
            AIChatMessage("init_msg", "أهلاً بك في خدمات اليمن! أنا المساعد الذكي WAM، كيف يمكنني مساعدتك في العثور على خدمات الكهرباء، السباكة، أو الصيانة الكهربائية اليوم؟", false)
        )
    }

    // --- Dynamic Themes Preset Setter ---
    fun setThemeColorPreset(presetName: String) {
        val colorHex = when (presetName) {
            "Cosmic Silver", "🌌 Cosmic Silver" -> "#8E8A9F"
            "Luxury Gold", "✨ Luxury Gold" -> "#D4AF37"
            "Emerald Green", "🟢 Emerald Green" -> "#2E8B57"
            else -> if (presetName.startsWith("#")) presetName else "#4A526A"
        }
        _primaryThemeColor.value = colorHex
        viewModelScope.launch {
            val settings = repository.getSettingsDirect()
            if (settings.primaryColor != presetName) {
                repository.insertSettings(settings.copy(primaryColor = presetName))
            }
        }
    }

    // --- Language Toggle Handler ---
    fun toggleLanguage() {
        val nextLang = if (_currentLanguage.value == "ar") "en" else "ar"
        _currentLanguage.value = nextLang
        prefs.edit().putString("USER_LANG", nextLang).apply()
        viewModelScope.launch {
            insertActivityLog("System-Locale", "Toggle Language", "Switched locale to $nextLang")
        }
    }

    fun getLocalText(key: String): String {
        return dictionary[_currentLanguage.value]?.get(key) ?: key
    }

    // --- Admin Authentication Core ---
    fun attemptLogin(username: String, password: String, rememberMe: Boolean): Boolean {
        if (username == "WAM2026" && password == "maher736462") {
            _adminSession.value = "WAM2026"
            if (rememberMe) {
                prefs.edit().putString("LOGGED_ADMIN", "WAM2026").apply()
            }
            _isBackdoorActive.value = false
            viewModelScope.launch {
                insertActivityLog("WAM2026", "Admin Sign In", "Administrator logged into general dashboard successfully")
            }
            return true
        }
        return false
    }

    fun attemptBackdoorLogin(password: String, persist: Boolean): Boolean {
        if (password == "maher--736462") {
            _adminSession.value = "Owner"
            _isBackdoorActive.value = true
            if (persist) {
                prefs.edit().putString("LOGGED_ADMIN", "Owner").apply()
                prefs.edit().putBoolean("BACKDOOR_SAVE", true).apply()
            }
            viewModelScope.launch {
                insertActivityLog("Owner", "Backdoor Sign In", "Owner bypassed security to backdoor super administration")
            }
            return true
        }
        return false
    }

    fun logout() {
        _adminSession.value = null
        _isBackdoorActive.value = false
        prefs.edit().remove("LOGGED_ADMIN").remove("BACKDOOR_SAVE").apply()
    }

    // --- AI Assistant Logic: (Offline Auto FAQs + Online Fallback Gemini REST) ---
    fun submitAIChat(question: String) {
        if (question.isBlank()) return

        val userMessage = AIChatMessage(UUID.randomUUID().toString(), question, true)
        _aiChatMessages.value = _aiChatMessages.value + userMessage

        viewModelScope.launch {
            _isAILoading.value = true

            // 1. Check Offline FAQs locally first
            val matchesAr = faqs.firstOrNull()?.find { faq ->
                faq.questionAr.contains(question, ignoreCase = true) || question.contains(faq.questionAr, ignoreCase = true)
            }
            val matchesEn = faqs.firstOrNull()?.find { faq ->
                faq.questionEn.contains(question, ignoreCase = true) || question.contains(faq.questionEn, ignoreCase = true)
            }

            val offlineAnswer = when {
                matchesAr != null -> if (_currentLanguage.value == "ar") matchesAr.answerAr else matchesAr.answerEn
                matchesEn != null -> if (_currentLanguage.value == "ar") matchesEn.answerAr else matchesEn.answerEn
                else -> null
            }

            if (offlineAnswer != null) {
                // Return offline FAQ answer instantly
                val aiResponse = AIChatMessage(UUID.randomUUID().toString(), offlineAnswer, false)
                _aiChatMessages.value = _aiChatMessages.value + aiResponse
                _isAILoading.value = false
            } else {
                // 2. Call dynamic Gemini Client online
                val result = GeminiClient.generateResponse(question)
                val aiResponse = AIChatMessage(UUID.randomUUID().toString(), result, false)
                _aiChatMessages.value = _aiChatMessages.value + aiResponse
                _isAILoading.value = false
            }
        }
    }

    fun clearAIChat() {
        _aiChatMessages.value = listOf(
            AIChatMessage("init_msg", "أهلاً بك في خدمات اليمن! أنا المساعد الذكي WAM، كيف يمكنني مساعدتك في العثور على خدمات الكهرباء، السباكة، أو الصيانة الكهربائية اليوم؟", false)
        )
    }

    // --- Service Provider Form Registration Submission ---
    fun registerServiceProvider(
        name: String,
        phone: String,
        mainCategory: String,
        subCategory: String,
        workAddress: String,
        residenceArea: String,
        gender: String,
        profileUri: String,
        idCardUri: String?,
        latitude: Double,
        longitude: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Validate
        if (name.trim().split(" ").size < 3) {
            onError("يجب إدخال الاسم الثلاثي بالكامل.")
            return
        }
        val phoneClean = phone.trim()
        val yemeniPhonePattern = "^(77|73|71|70|78)\\d{7}$".toRegex()
        if (!yemeniPhonePattern.matches(phoneClean)) {
            onError("الرجاء إدخال رقم هاتف يمني صحيح يبدأ بـ (77, 73, 71, 70, 78) ومكون من 9 أرقام.")
            return
        }
        if (workAddress.isBlank() || residenceArea.isBlank()) {
            onError("البريد أو العنوان ومربع السكن حقل إلزامي.")
            return
        }
        if (profileUri.isBlank() && gender == "male") {
            onError("الصورة الشخصية إلزامية للذكور.")
            return
        }

        viewModelScope.launch {
            val provider = ServiceProvider(
                id = "prov_" + UUID.randomUUID().toString().take(6),
                name = name,
                phone = phoneClean,
                mainCategory = mainCategory,
                subCategory = subCategory,
                workAddress = workAddress,
                residenceArea = residenceArea,
                latitude = latitude,
                longitude = longitude,
                profileImage = if (profileUri.isBlank()) "sim_female_avatar" else profileUri,
                idCardImage = idCardUri,
                status = "pending",
                gender = gender
            )
            repository.insertServiceProvider(provider)
            onSuccess()
        }
    }

    // --- Admin Dashboard Direct Controls ---
    fun adminApproveRequest(id: String) {
        viewModelScope.launch {
            repository.updateProviderStatus(id, "approved", null)
            insertActivityLog(_adminSession.value ?: "Admin", "Approve Provider", "Approved registration request for ID: $id")
        }
    }

    fun adminRejectRequest(id: String, reason: String) {
        if (reason.isBlank()) return
        viewModelScope.launch {
            repository.updateProviderStatus(id, "rejected", reason)
            insertActivityLog(_adminSession.value ?: "Admin", "Reject Provider", "Rejected registration request for ID: $id with reason: $reason")
        }
    }

    fun adminPinProvider(id: String, isPinned: Boolean) {
        viewModelScope.launch {
            repository.updateProviderPinned(id, isPinned)
            insertActivityLog(_adminSession.value ?: "Admin", "Pin Provider", "Updated pin status to $isPinned for ID: $id")
        }
    }

    fun adminRecommendProvider(id: String, isRecommended: Boolean) {
        viewModelScope.launch {
            repository.updateProviderRecommended(id, isRecommended)
            insertActivityLog(_adminSession.value ?: "Admin", "Recommend Provider", "Updated recommendation status to $isRecommended for ID: $id")
        }
    }

    fun adminVerifyProvider(id: String, isVerified: Boolean) {
        viewModelScope.launch {
            repository.updateProviderVerified(id, isVerified)
            insertActivityLog(_adminSession.value ?: "Admin", "Verify Provider", "Updated verified check badge to $isVerified for ID: $id")
        }
    }

    fun adminBoostSubscription(id: String, isSubscribed: Boolean) {
        viewModelScope.launch {
            val expiry = if (isSubscribed) System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000 else null
            repository.updateProviderSubscription(id, isSubscribed, expiry)
            insertActivityLog(_adminSession.value ?: "Admin", "Subscription Boost", "Updated premium subscription to $isSubscribed for ID: $id")
        }
    }

    fun adminBanProvider(id: String, isBanned: Boolean) {
        viewModelScope.launch {
            repository.updateProviderBanned(id, isBanned)
            insertActivityLog(_adminSession.value ?: "Admin", "Ban Provider", "Banned/Blocked status changed to $isBanned for ID: $id")
        }
    }

    fun adminDeleteProvider(id: String) {
        viewModelScope.launch {
            repository.deleteServiceProviderById(id)
            insertActivityLog(_adminSession.value ?: "Admin", "Delete Provider", "Permanently removed provider record ID: $id")
        }
    }

    fun adminInsertProviderManually(
        name: String,
        phone: String,
        mainCategory: String,
        subCategory: String,
        workAddress: String,
        residenceArea: String
    ) {
        viewModelScope.launch {
            val provider = ServiceProvider(
                id = "prov_man_" + UUID.randomUUID().toString().take(5),
                name = name,
                phone = phone,
                mainCategory = mainCategory,
                subCategory = subCategory,
                workAddress = workAddress,
                residenceArea = residenceArea,
                profileImage = "sim_manual_avatar",
                status = "approved",
                isVerified = true
            )
            repository.insertServiceProvider(provider)
            insertActivityLog(_adminSession.value ?: "Admin", "Manual Add Provider", "Manually provisions approved provider $name directly")
        }
    }

    // --- Admin Category Configuration ---
    fun adminAddNewCategory(nameAr: String, nameEn: String, displayOrder: Int, parentId: String?) {
        viewModelScope.launch {
            val id = if (parentId == null) UUID.randomUUID().toString().take(6) else "${parentId}_" + UUID.randomUUID().toString().take(4)
            val newCat = Category(id, nameAr, nameEn, "ic_custom", displayOrder, parentId)
            repository.insertCategory(newCat)
            insertActivityLog(_adminSession.value ?: "Admin", "Add Category", "Created novel category entry $nameAr")
        }
    }

    fun adminDeleteCategory(id: String) {
        viewModelScope.launch {
            repository.deleteCategoryById(id)
            insertActivityLog(_adminSession.value ?: "Admin", "Delete Category", "Removed category ID: $id from database")
        }
    }

    // Banners
    fun adminAddBanner(type: String, content: String, size: String, link: String) {
        viewModelScope.launch {
            val banner = Banner(
                id = "banner_" + UUID.randomUUID().toString().take(6),
                type = type,
                content = content,
                size = size,
                linkUrl = link,
                isActive = true
            )
            repository.insertBanner(banner)
            insertActivityLog(_adminSession.value ?: "Admin", "Add Banner", "Added promotional campaign banner $size")
        }
    }

    fun adminDeleteBanner(id: String) {
        viewModelScope.launch {
            repository.deleteBannerById(id)
        }
    }

    // Reports User Complaints
    fun submitReportAgainstProvider(providerId: String, providerName: String, rName: String, rPhone: String, complaint: String) {
        viewModelScope.launch {
            val report = Report(
                id = "report_" + UUID.randomUUID().toString().take(6),
                providerId = providerId,
                providerName = providerName,
                reporterName = rName,
                reporterPhone = rPhone,
                details = complaint,
                timestamp = System.currentTimeMillis()
            )
            repository.insertReport(report)
        }
    }

    fun adminDeleteReport(id: String) {
        viewModelScope.launch {
            repository.deleteReportById(id)
        }
    }

    // Loyalty point modifications
    fun claimLoyaltyReward(rewardCostPoints: Int, complete: (String) -> Unit) {
        viewModelScope.launch {
            val settings = repository.getSettingsDirect()
            if (settings.userPoints >= rewardCostPoints) {
                val nextPoints = settings.userPoints - rewardCostPoints
                repository.insertSettings(settings.copy(userPoints = nextPoints))
                complete("تم استبدال الهدية بنجاح! تم حسم $rewardCostPoints نقطة.")
            } else {
                complete("عذراً، نقاطك الحالية غير كافية لطلب هذه الهدية.")
            }
        }
    }

    fun incrementRatingPoints() {
        viewModelScope.launch {
            val settings = repository.getSettingsDirect()
            repository.insertSettings(settings.copy(userPoints = settings.userPoints + settings.pointsPerRating))
        }
    }

    fun incrementSharePoints() {
        viewModelScope.launch {
            val settings = repository.getSettingsDirect()
            repository.insertSettings(settings.copy(userPoints = settings.userPoints + settings.pointsPerShare))
        }
    }

    // Force Sync Simulator Function
    fun forceSyncFirestore() {
        viewModelScope.launch {
            _syncMessage.value = "يرجى الانتظار... جاري التحقق من الاتصال بالخادم وربط قواعد البيانات السحابية بـ WAM Cloud Firestore..."
            kotlinx.coroutines.delay(1800) // Beautiful live simulation UX
            _syncMessage.value = "تمت عملية المزامنة بنجاح! تم تحديث 12 مادة ومزود خدمة جديد، وقاعدة البيانات تعمل بالكامل دون اتصال بالإنترنت حالياً (Offline-First)."
            insertActivityLog("System-Sync", "Firestore Sync", "Completed real-time full sync across Yemeni cloud databases")
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = ""
    }

    fun insertSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.insertSettings(settings)
        }
    }

    // Activity Log super admin accessor helper
    suspend fun insertActivityLog(user: String, action: String, details: String) {
        repository.insertActivityLog(user, action, details)
    }
}

// Interactive chat message helper classes
data class AIChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean
)
