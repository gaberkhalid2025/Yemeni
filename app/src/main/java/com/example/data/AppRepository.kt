package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val appDao: AppDao) {

    // --- Reactive Flows ---
    val categories: Flow<List<Category>> = callbackFlow {
        val firestore = FirebaseFirestore.getInstance()
        val listener = firestore.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            Category(
                                id = doc.getString("id") ?: doc.id,
                                nameAr = doc.getString("nameAr") ?: "",
                                nameEn = doc.getString("nameEn") ?: "",
                                imageUri = doc.getString("imageUri") ?: "",
                                displayOrder = doc.getLong("displayOrder")?.toInt() ?: 0,
                                parentCategoryId = doc.getString("parentCategoryId")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    val allServiceProviders: Flow<List<ServiceProvider>> = callbackFlow {
        val firestore = FirebaseFirestore.getInstance()
        val listener = firestore.collection("service_providers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            ServiceProvider(
                                id = doc.getString("id") ?: doc.id,
                                name = doc.getString("name") ?: "",
                                phone = doc.getString("phone") ?: "",
                                mainCategory = doc.getString("mainCategory") ?: "",
                                subCategory = doc.getString("subCategory") ?: "",
                                workAddress = doc.getString("workAddress") ?: "",
                                residenceArea = doc.getString("residenceArea") ?: "",
                                latitude = doc.getDouble("latitude") ?: 15.3694,
                                longitude = doc.getDouble("longitude") ?: 44.1910,
                                profileImage = doc.getString("profileImage") ?: "",
                                idCardImage = doc.getString("idCardImage"),
                                status = doc.getString("status") ?: "pending",
                                rejectionReason = doc.getString("rejectionReason"),
                                isPinned = doc.getBoolean("pinned") ?: doc.getBoolean("isPinned") ?: false,
                                isRecommended = doc.getBoolean("recommended") ?: doc.getBoolean("isRecommended") ?: false,
                                isVerified = doc.getBoolean("verified") ?: doc.getBoolean("isVerified") ?: false,
                                isSubscribed = doc.getBoolean("subscribed") ?: doc.getBoolean("isSubscribed") ?: false,
                                subscriptionExpiry = doc.getLong("subscriptionExpiry"),
                                ratingSum = doc.getLong("ratingSum")?.toInt() ?: 0,
                                ratingCount = doc.getLong("ratingCount")?.toInt() ?: 0,
                                isBanned = doc.getBoolean("banned") ?: doc.getBoolean("isBanned") ?: false,
                                gender = doc.getString("gender") ?: "male",
                                chatSuspended = doc.getBoolean("chatSuspended") ?: false
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    val approvedServiceProviders: Flow<List<ServiceProvider>> = allServiceProviders.map { list ->
        list.filter { it.status == "approved" && !it.isBanned }
    }

    val pendingServiceProviders: Flow<List<ServiceProvider>> = allServiceProviders.map { list ->
        list.filter { it.status == "pending" }
    }

    val activeBanners: Flow<List<Banner>> = appDao.getActiveBanners()
    val allBanners: Flow<List<Banner>> = appDao.getAllBanners()
    val reports: Flow<List<Report>> = appDao.getAllReports()
    val activityLogs: Flow<List<ActivityLog>> = appDao.getAllActivityLogs()
    val faqs: Flow<List<FAQItem>> = appDao.getAllFAQs()
    val settingsFlow: Flow<AppSettings?> = callbackFlow {
        val firestore = FirebaseFirestore.getInstance()
        val listener = firestore.collection("settings").document("global_settings")
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    try {
                        val s = AppSettings(
                            id = doc.getString("id") ?: "global_settings",
                            appName = doc.getString("appName") ?: "WAM Services",
                            primaryColor = doc.getString("primaryColor") ?: "Cosmic Silver",
                            secondaryColor = doc.getString("secondaryColor") ?: "Default",
                            footerText = doc.getString("footerText") ?: "WAM777644670",
                            welcomeMessage = doc.getString("welcomeMessage") ?: "مرحباً بكم في منصة دليل مقدمي الخدمات اليمني الأول",
                            supportPhone = doc.getString("supportPhone") ?: "777644670",
                            supportEmail = doc.getString("supportEmail") ?: "support@wam.ye",
                            supportWhatsApp = doc.getString("supportWhatsApp") ?: "777644670",
                            adminPassword = doc.getString("adminPassword") ?: "maher736462",
                            isMaintenanceMode = doc.getBoolean("isMaintenanceMode") ?: doc.getBoolean("maintenanceMode") ?: false,
                            maintenanceMessage = doc.getString("maintenanceMessage") ?: "التطبيق قيد الصيانة الطارئة حالياً. نرجو العودة لاحقاً.",
                            is2FAEnabled = doc.getBoolean("is2FAEnabled") ?: doc.getBoolean("twoFAEnabled") ?: false,
                            whitelistedDevices = doc.getString("whitelistedDevices") ?: "",
                            topBarIconsArrangement = doc.getString("topBarIconsArrangement") ?: "Home,Login,Register,Language,Refresh",
                            chatButtonHidden = doc.getBoolean("chatButtonHidden") ?: false,
                            chatButtonSize = doc.getLong("chatButtonSize")?.toInt() ?: 50,
                            chatButtonPosition = doc.getString("chatButtonPosition") ?: "bottom_right",
                            loyaltyPointsEnabled = doc.getBoolean("loyaltyPointsEnabled") ?: true,
                            pointsPerRating = doc.getLong("pointsPerRating")?.toInt() ?: 10,
                            pointsPerShare = doc.getLong("pointsPerShare")?.toInt() ?: 20,
                            userPoints = doc.getLong("userPoints")?.toInt() ?: 0,
                            allowGuestMode = doc.getBoolean("allowGuestMode") ?: true,
                            dataSaverMode = doc.getBoolean("dataSaverMode") ?: false,
                            supportIconSize = doc.getLong("supportIconSize")?.toInt() ?: 50,
                            supportIconVisible = doc.getBoolean("supportIconVisible") ?: true,
                            fontColor = doc.getString("fontColor") ?: "#FFFFFF",
                            fontType = doc.getString("fontType") ?: "Bold",
                            fontSize = doc.getLong("fontSize")?.toInt() ?: 14,
                            footerOpacity = doc.getDouble("footerOpacity")?.toFloat() ?: 1.0f,
                            footerHeightScale = doc.getLong("footerHeightScale")?.toInt() ?: 56,
                            footerFontSize = doc.getLong("footerFontSize")?.toInt() ?: 12,
                            cumulativeCallsCount = doc.getLong("cumulativeCallsCount")?.toInt() ?: 0,
                            isChatServiceDisabled = doc.getBoolean("isChatServiceDisabled") ?: doc.getBoolean("chatServiceDisabled") ?: false,
                            chatServiceDisabledReason = doc.getString("chatServiceDisabledReason") ?: "تم إيقاف خدمة المحادثات الفورية مؤقتاً لتحديث النظام بقرار من الإدارة.",
                            assistantIconSymbol = doc.getString("assistantIconSymbol") ?: "Face",
                            assistantIconGlow = doc.getBoolean("assistantIconGlow") ?: false,
                            liveChatIconSymbol = doc.getString("liveChatIconSymbol") ?: "Mail",
                            liveChatIconGlow = doc.getBoolean("liveChatIconGlow") ?: false,
                            iconVisualEffectType = doc.getString("iconVisualEffectType") ?: "Pulse"
                        )
                        trySend(s)
                    } catch (e: Exception) {
                        trySend(AppSettings())
                    }
                } else {
                    trySend(AppSettings())
                }
            }
        awaitClose { listener.remove() }
    }

    // --- Direct Suspend Database Actions ---
    suspend fun getSettingsDirect(): AppSettings = withContext(Dispatchers.IO) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val task = firestore.collection("settings").document("global_settings").get()
            val doc = Tasks.await(task)
            if (doc.exists()) {
                AppSettings(
                    id = doc.getString("id") ?: "global_settings",
                    appName = doc.getString("appName") ?: "WAM Services",
                    primaryColor = doc.getString("primaryColor") ?: "Cosmic Silver",
                    secondaryColor = doc.getString("secondaryColor") ?: "Default",
                    footerText = doc.getString("footerText") ?: "WAM777644670",
                    welcomeMessage = doc.getString("welcomeMessage") ?: "مرحباً بكم في منصة دليل مقدمي الخدمات اليمني الأول",
                    supportPhone = doc.getString("supportPhone") ?: "777644670",
                    supportEmail = doc.getString("supportEmail") ?: "support@wam.ye",
                    supportWhatsApp = doc.getString("supportWhatsApp") ?: "777644670",
                    adminPassword = doc.getString("adminPassword") ?: "maher736462",
                    isMaintenanceMode = doc.getBoolean("isMaintenanceMode") ?: doc.getBoolean("maintenanceMode") ?: false,
                    maintenanceMessage = doc.getString("maintenanceMessage") ?: "التطبيق قيد الصيانة الطارئة حالياً. نرجو العودة لاحقاً.",
                    is2FAEnabled = doc.getBoolean("is2FAEnabled") ?: doc.getBoolean("twoFAEnabled") ?: false,
                    whitelistedDevices = doc.getString("whitelistedDevices") ?: "",
                    topBarIconsArrangement = doc.getString("topBarIconsArrangement") ?: "Home,Login,Register,Language,Refresh",
                    chatButtonHidden = doc.getBoolean("chatButtonHidden") ?: false,
                    chatButtonSize = doc.getLong("chatButtonSize")?.toInt() ?: 50,
                    chatButtonPosition = doc.getString("chatButtonPosition") ?: "bottom_right",
                    loyaltyPointsEnabled = doc.getBoolean("loyaltyPointsEnabled") ?: true,
                    pointsPerRating = doc.getLong("pointsPerRating")?.toInt() ?: 10,
                    pointsPerShare = doc.getLong("pointsPerShare")?.toInt() ?: 20,
                    userPoints = doc.getLong("userPoints")?.toInt() ?: 0,
                    allowGuestMode = doc.getBoolean("allowGuestMode") ?: true,
                    dataSaverMode = doc.getBoolean("dataSaverMode") ?: false,
                    supportIconSize = doc.getLong("supportIconSize")?.toInt() ?: 50,
                    supportIconVisible = doc.getBoolean("supportIconVisible") ?: true,
                    fontColor = doc.getString("fontColor") ?: "#FFFFFF",
                    fontType = doc.getString("fontType") ?: "Bold",
                    fontSize = doc.getLong("fontSize")?.toInt() ?: 14,
                    footerOpacity = doc.getDouble("footerOpacity")?.toFloat() ?: 1.0f,
                    footerHeightScale = doc.getLong("footerHeightScale")?.toInt() ?: 56,
                    footerFontSize = doc.getLong("footerFontSize")?.toInt() ?: 12,
                    cumulativeCallsCount = doc.getLong("cumulativeCallsCount")?.toInt() ?: 0,
                    isChatServiceDisabled = doc.getBoolean("isChatServiceDisabled") ?: doc.getBoolean("chatServiceDisabled") ?: false,
                    chatServiceDisabledReason = doc.getString("chatServiceDisabledReason") ?: "تم إيقاف خدمة المحادثات الفورية مؤقتاً لتحديث النظام بقرار من الإدارة.",
                    assistantIconSymbol = doc.getString("assistantIconSymbol") ?: "Face",
                    assistantIconGlow = doc.getBoolean("assistantIconGlow") ?: false,
                    liveChatIconSymbol = doc.getString("liveChatIconSymbol") ?: "Mail",
                    liveChatIconGlow = doc.getBoolean("liveChatIconGlow") ?: false,
                    iconVisualEffectType = doc.getString("iconVisualEffectType") ?: "Pulse"
                )
            } else {
                appDao.getSettingsDirect() ?: AppSettings()
            }
        } catch (e: Exception) {
            appDao.getSettingsDirect() ?: AppSettings()
        }
    }

    suspend fun insertSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val task = firestore.collection("settings").document("global_settings").set(settings)
        Tasks.await(task)
        appDao.insertSettings(settings)
    }

    suspend fun insertCategory(category: Category) = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val task = firestore.collection("categories").document(category.id).set(category)
        Tasks.await(task)
    }

    suspend fun deleteCategory(category: Category) = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val task = firestore.collection("categories").document(category.id).delete()
        Tasks.await(task)
    }

    suspend fun deleteCategoryById(id: String) = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val task = firestore.collection("categories").document(id).delete()
        Tasks.await(task)
    }

    suspend fun insertServiceProvider(provider: ServiceProvider) = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val task = firestore.collection("service_providers").document(provider.id).set(provider)
        Tasks.await(task)
    }

    suspend fun getServiceProviderById(id: String): ServiceProvider? = withContext(Dispatchers.IO) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val task = firestore.collection("service_providers").document(id).get()
            val doc = Tasks.await(task)
            if (doc.exists()) {
                ServiceProvider(
                    id = doc.getString("id") ?: doc.id,
                    name = doc.getString("name") ?: "",
                    phone = doc.getString("phone") ?: "",
                    mainCategory = doc.getString("mainCategory") ?: "",
                    subCategory = doc.getString("subCategory") ?: "",
                    workAddress = doc.getString("workAddress") ?: "",
                    residenceArea = doc.getString("residenceArea") ?: "",
                    latitude = doc.getDouble("latitude") ?: 15.3694,
                    longitude = doc.getDouble("longitude") ?: 44.1910,
                    profileImage = doc.getString("profileImage") ?: "",
                    idCardImage = doc.getString("idCardImage"),
                    status = doc.getString("status") ?: "pending",
                    rejectionReason = doc.getString("rejectionReason"),
                    isPinned = doc.getBoolean("pinned") ?: doc.getBoolean("isPinned") ?: false,
                    isRecommended = doc.getBoolean("recommended") ?: doc.getBoolean("isRecommended") ?: false,
                    isVerified = doc.getBoolean("verified") ?: doc.getBoolean("isVerified") ?: false,
                    isSubscribed = doc.getBoolean("subscribed") ?: doc.getBoolean("isSubscribed") ?: false,
                    subscriptionExpiry = doc.getLong("subscriptionExpiry"),
                    ratingSum = doc.getLong("ratingSum")?.toInt() ?: 0,
                    ratingCount = doc.getLong("ratingCount")?.toInt() ?: 0,
                    isBanned = doc.getBoolean("banned") ?: doc.getBoolean("isBanned") ?: false,
                    gender = doc.getString("gender") ?: "male",
                    chatSuspended = doc.getBoolean("chatSuspended") ?: false
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateProviderStatus(id: String, status: String, reason: String?) = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val updates = mapOf(
            "status" to status,
            "rejectionReason" to reason
        )
        val task = firestore.collection("service_providers").document(id).update(updates)
        Tasks.await(task)
    }

    suspend fun updateProviderPinned(id: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val task = firestore.collection("service_providers").document(id).update("isPinned", isPinned)
        Tasks.await(task)
    }

    suspend fun updateProviderRecommended(id: String, isRecommended: Boolean) = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val task = firestore.collection("service_providers").document(id).update("isRecommended", isRecommended)
        Tasks.await(task)
    }

    suspend fun updateProviderVerified(id: String, isVerified: Boolean) = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val task = firestore.collection("service_providers").document(id).update("isVerified", isVerified)
        Tasks.await(task)
    }

    suspend fun updateProviderSubscription(id: String, isSubscribed: Boolean, expiry: Long?) = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val updates = mapOf(
            "isSubscribed" to isSubscribed,
            "subscriptionExpiry" to expiry
        )
        val task = firestore.collection("service_providers").document(id).update(updates)
        Tasks.await(task)
    }

    suspend fun updateProviderBanned(id: String, isBanned: Boolean) = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val task = firestore.collection("service_providers").document(id).update("isBanned", isBanned)
        Tasks.await(task)
    }

    suspend fun deleteServiceProviderById(id: String) = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val task = firestore.collection("service_providers").document(id).delete()
        Tasks.await(task)
    }

    suspend fun insertBanner(banner: Banner) {
        appDao.insertBanner(banner)
    }

    suspend fun deleteBannerById(id: String) {
        appDao.deleteBannerById(id)
    }

    suspend fun insertReport(report: Report) {
        appDao.insertReport(report)
    }

    suspend fun deleteReportById(id: String) {
        appDao.deleteReportById(id)
    }

    fun getChatMessages(chatId: String): Flow<List<ChatMessage>> {
        return appDao.getChatMessagesFlow(chatId)
    }

    suspend fun insertChatMessage(message: ChatMessage) {
        appDao.insertChatMessage(message)
    }

    suspend fun clearChatMessages(chatId: String) {
        appDao.clearChatMessages(chatId)
    }

    suspend fun insertActivityLog(adminName: String, action: String, details: String) {
        val log = ActivityLog(
            id = UUID.randomUUID().toString(),
            adminName = adminName,
            action = action,
            details = details,
            timestamp = System.currentTimeMillis()
        )
        appDao.insertActivityLog(log)
    }

    suspend fun insertFAQ(faq: FAQItem) {
        appDao.insertFAQ(faq)
    }

    suspend fun deleteFAQById(id: String) {
        appDao.deleteFAQById(id)
    }

    suspend fun updateProviderChatSuspended(id: String, chatSuspended: Boolean) {
        appDao.updateProviderChatSuspended(id, chatSuspended)
    }

    // --- Moderator Account Facades ---
    val allModerators: Flow<List<Moderator>> = callbackFlow {
        val firestore = FirebaseFirestore.getInstance()
        val listener = firestore.collection("moderators")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            Moderator(
                                username = doc.getString("username") ?: doc.id,
                                password = doc.getString("password") ?: "",
                                role = doc.getString("role") ?: "moderator",
                                canEditCategories = doc.getBoolean("canEditCategories") ?: true,
                                canDeleteProviders = doc.getBoolean("canDeleteProviders") ?: true,
                                canManageSettings = doc.getBoolean("canManageSettings") ?: true,
                                isActive = doc.getBoolean("active") ?: doc.getBoolean("isActive") ?: true
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getModeratorsDirectList(): List<Moderator> = withContext(Dispatchers.IO) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val task = firestore.collection("moderators").get()
            val snap = Tasks.await(task)
            snap.documents.mapNotNull { doc ->
                try {
                    Moderator(
                        username = doc.getString("username") ?: doc.id,
                        password = doc.getString("password") ?: "",
                        role = doc.getString("role") ?: "moderator",
                        canEditCategories = doc.getBoolean("canEditCategories") ?: true,
                        canDeleteProviders = doc.getBoolean("canDeleteProviders") ?: true,
                        canManageSettings = doc.getBoolean("canManageSettings") ?: true,
                        isActive = doc.getBoolean("active") ?: doc.getBoolean("isActive") ?: true
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            appDao.getAllModerators()
        }
    }

    suspend fun getModeratorByUsername(username: String): Moderator? = withContext(Dispatchers.IO) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val task = firestore.collection("moderators").document(username).get()
            val doc = Tasks.await(task)
            if (doc.exists()) {
                Moderator(
                    username = doc.getString("username") ?: doc.id,
                    password = doc.getString("password") ?: "",
                    role = doc.getString("role") ?: "moderator",
                    canEditCategories = doc.getBoolean("canEditCategories") ?: true,
                    canDeleteProviders = doc.getBoolean("canDeleteProviders") ?: true,
                    canManageSettings = doc.getBoolean("canManageSettings") ?: true,
                    isActive = doc.getBoolean("active") ?: doc.getBoolean("isActive") ?: true
                )
            } else {
                appDao.getModeratorByUsername(username)
            }
        } catch (e: Exception) {
            appDao.getModeratorByUsername(username)
        }
    }

    suspend fun insertModerator(moderator: Moderator) = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val task = firestore.collection("moderators").document(moderator.username).set(moderator)
        Tasks.await(task)
        appDao.insertModerator(moderator)
    }

    suspend fun deleteModeratorByUsername(username: String) = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val task = firestore.collection("moderators").document(username).delete()
        Tasks.await(task)
        appDao.deleteModeratorByUsername(username)
    }

    // --- Global Chat Audits ---
    val allChatMessages: Flow<List<ChatMessage>> = appDao.getAllChatMessagesFlow()

    suspend fun deleteAllChatMessages() {
        appDao.deleteAllChatMessages()
    }

    // --- DB Seeding ---
    suspend fun seedInitialDataIfEmpty() {
        val existingSettings = appDao.getSettingsDirect()
        if (existingSettings == null) {
            // Seed settings
            appDao.insertSettings(AppSettings())
        }

        // Seed some initial moderators if empty
        val existingMods = appDao.getAllModerators()
        if (existingMods.isEmpty()) {
            appDao.insertModerator(Moderator("admin1", "pass123", "moderator", true, true, true, isActive = true))
            appDao.insertModerator(Moderator("yemen_mod", "123456", "moderator", true, false, false, isActive = true))
        }

        // Seed Firestore Settings & Moderators if empty
        try {
            val firestore = FirebaseFirestore.getInstance()
            val settingsDocTask = firestore.collection("settings").document("global_settings").get()
            val docSnap = Tasks.await(settingsDocTask)
            if (!docSnap.exists()) {
                val defaultSettings = AppSettings()
                Tasks.await(firestore.collection("settings").document("global_settings").set(defaultSettings))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val firestore = FirebaseFirestore.getInstance()
            val modsTask = firestore.collection("moderators").limit(1).get()
            val modsSnap = Tasks.await(modsTask)
            if (modsSnap.isEmpty) {
                val initialMods = listOf(
                    Moderator("admin1", "pass123", "moderator", true, true, true, isActive = true),
                    Moderator("yemen_mod", "123456", "moderator", true, false, false, isActive = true)
                )
                for (mod in initialMods) {
                    Tasks.await(firestore.collection("moderators").document(mod.username).set(mod))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Seed Firestore categories & service providers if Firestore is empty
        try {
            val firestore = FirebaseFirestore.getInstance()
            val categoriesTask = firestore.collection("categories").limit(1).get()
            val categoriesSnap = Tasks.await(categoriesTask)
            if (categoriesSnap.isEmpty) {
                val initialCategories = listOf(
                    Category("1", "الكهرباء", "Electricity", "ic_flash", 1, null),
                    Category("1_1", "تأسيس وتمديد شبكات منازل", "Home Wiring Installation", "ic_wire", 1, "1"),
                    Category("1_2", "إصلاح وتصليح أعطال ماسات", "Fault Repairing & Troubleshooting", "ic_bolt", 2, "1"),
                    Category("1_3", "تركيب كاميرات مراقبة وأحزمة حماية", "Security Camera Installation", "ic_cam", 3, "1"),
                    Category("2", "السباكة", "Plumbing", "ic_pipe", 2, null),
                    Category("2_1", "تمديد وتأسيس حمامات ومطابخ", "Plumbing & Piping Setup", "ic_water", 1, "2"),
                    Category("2_2", "إصلاح تسريبات المياه والصرف الصحي", "Leak Repair", "ic_leak", 2, "2"),
                    Category("3", "صيانة الأجهزة الكهربائية", "Appliance Repair", "ic_fridge", 3, null),
                    Category("3_1", "صيانة مكيفات هواء وتعبئة فريون", "AC Repair & Maintenance", "ic_temp", 1, "3"),
                    Category("3_2", "صيانة شاشات وأجهزة تلفزة ذكية", "Smart TV Repair", "ic_tv", 2, "3"),
                    Category("4", "الخدمات الطبية المنزلية", "Home Medical Services", "ic_med", 4, null),
                    Category("4_1", "تمريض وتغيير جروح منزلي", "Home Nursing", "ic_nurse", 1, "4"),
                    Category("4_2", "طبيب أطفال وعلاج طبيعي منزلي", "Home Physiotherapy", "ic_doctor", 2, "4")
                )
                for (cat in initialCategories) {
                    Tasks.await(firestore.collection("categories").document(cat.id).set(cat))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val faqsCount = appDao.getAllFAQs().firstOrNull() ?: emptyList()
        if (faqsCount.isEmpty()) {
            val initialFAQs = listOf(
                FAQItem("faq_1", "ماهي الأقسام", "قائمة الأقسام تشمل الكهرباء والسباكة وصيانة الأجهزة المنزلية والخدمات الطبية وغيرها الكثير.", "What are the categories?", "The categories include Electricity, Plumbing, Appliance Repair, Home Medical Services, and more."),
                FAQItem("faq_2", "كيف أتصل بمقدم خدمة", "يمكنك الضغط على كرت مقدم الخدمة والاتصال به مباشرة عن طريق الهاتف أو مراسلته عبر تطبيق الواتساب المدمج.", "How do I contact a provider?", "You can tap on the provider card to view their profile, call them directly over phone, or message via integrated chat/WhatsApp."),
                FAQItem("faq_3", "ما هو رقم الدعم", "رقم الدعم الفني لعملاء ومزودي الخدمة في اليمن هو 777644670 متاح على مدار الساعة.", "What is the support number?", "The custom support helpline for customers and providers in Yemen is 777644670, available 24/7.")
            )
            for (faq in initialFAQs) {
                appDao.insertFAQ(faq)
            }
        }

        try {
            val firestore = FirebaseFirestore.getInstance()
            val providersTask = firestore.collection("service_providers").limit(1).get()
            val providersSnap = Tasks.await(providersTask)
            if (providersSnap.isEmpty) {
                val seedProviders = listOf(
                    ServiceProvider(
                        id = "prov_1",
                        name = "ماهر محمد طاهر",
                        phone = "777644670",
                        mainCategory = "1",
                        subCategory = "1_1",
                        workAddress = "صنعاء، شارع الستين الغربي",
                        residenceArea = "مديرية معين",
                        latitude = 15.3614,
                        longitude = 44.1802,
                        profileImage = "sim_pic_1",
                        status = "approved",
                        isVerified = true,
                        isPinned = true,
                        isRecommended = true,
                        ratingSum = 25,
                        ratingCount = 5
                    ),
                    ServiceProvider(
                        id = "prov_2",
                        name = "عبدالله صالح الحميري",
                        phone = "733456789",
                        mainCategory = "2",
                        subCategory = "2_1",
                        workAddress = "عدن، المنصورة، شارع التسعين",
                        residenceArea = "المنصورة",
                        latitude = 12.8315,
                        longitude = 44.9810,
                        profileImage = "sim_pic_2",
                        status = "approved",
                        isVerified = true,
                        isRecommended = true,
                        isSubscribed = true,
                        subscriptionExpiry = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000, // 30 days
                        ratingSum = 18,
                        ratingCount = 4
                    ),
                    ServiceProvider(
                        id = "prov_3",
                        name = "منى ياسين القدسي",
                        phone = "711987654",
                        mainCategory = "4",
                        subCategory = "4_1",
                        workAddress = "تعز، شارع جمال عبد الناصر",
                        residenceArea = "مديرية القاهرة",
                        latitude = 13.5786,
                        longitude = 44.0135,
                        profileImage = "sim_pic_3",
                        status = "approved",
                        isVerified = false,
                        isPinned = false,
                        ratingSum = 9,
                        ratingCount = 2,
                        gender = "female"
                    )
                )
                for (prov in seedProviders) {
                    Tasks.await(firestore.collection("service_providers").document(prov.id).set(prov))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val bannersCount = appDao.getAllBanners().firstOrNull() ?: emptyList()
        if (bannersCount.isEmpty()) {
            appDao.insertBanner(Banner("banner_1", "text", "حمل تطبيق WAM للحصول على أفضل خدمات محلية في اليمن", "L", 5, "", "top", true))
            appDao.insertBanner(Banner("banner_2", "text", "خصم 20% عند طلب صيانة المكيفات المنزلية هذا الأسبوع!", "M", 6, "", "top", true))
        }
    }
}
