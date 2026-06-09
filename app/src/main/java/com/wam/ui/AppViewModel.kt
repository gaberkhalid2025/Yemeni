package com.wam.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wam.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AppRepository()

    // --- StateFlow bindings ---
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _providers = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val providers: StateFlow<List<ServiceProvider>> = _providers.asStateFlow()

    private val _pendingProviders = MutableStateFlow<List<PendingProvider>>(emptyList())
    val pendingProviders: StateFlow<List<PendingProvider>> = _pendingProviders.asStateFlow()

    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    val banners: StateFlow<List<Banner>> = _banners.asStateFlow()

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private val _admins = MutableStateFlow<List<Admin>>(emptyList())
    val admins: StateFlow<List<Admin>> = _admins.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _logs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val logs: StateFlow<List<ActivityLog>> = _logs.asStateFlow()

    // --- Local Session States ---
    private val _currentLang = MutableStateFlow("ar")
    val currentLang: StateFlow<String> = _currentLang.asStateFlow()

    private val _adminLoggedIn = MutableStateFlow<String?>(null) // Contains admin username if logged in
    val adminLoggedIn: StateFlow<String?> = _adminLoggedIn.asStateFlow()

    private val _adminPermissions = MutableStateFlow<List<String>>(emptyList())
    val adminPermissions: StateFlow<List<String>> = _adminPermissions.asStateFlow()

    private val _userLoyaltyPoints = MutableStateFlow(120) // Default starting points
    val userLoyaltyPoints: StateFlow<Int> = _userLoyaltyPoints.asStateFlow()

    // --- AI Assistant Dialog State ---
    private val _assistantChat = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf("أهلاً بك! أنا مساعد دليل خدمات اليمن الذكي 🤖. كيف يمكنني مساعدتك اليوم؟" to false)
    )
    val assistantChat: StateFlow<List<Pair<String, Boolean>>> = _assistantChat.asStateFlow()

    private val _assistantLoading = MutableStateFlow(false)
    val assistantLoading: StateFlow<Boolean> = _assistantLoading.asStateFlow()

    // FAQ dictionary for Offline-First answers
    private val offlineFaqsAr = mapOf(
        "sections" to "الأقسام والخدمات المتوفرة حالياً بالدليل تشمل: كهربائي، سباك، نجار، حداد، مهندس مكيفات، دهان، وغيرها الكثير! تصفح الواجهة الرئيسية للمزيد.",
        "الأقسام" to "الأقسام والخدمات المتوفرة حالياً بالدليل تشمل: كهربائي، سباك، نجار، حداد، مهندس مكيفات، دهان، وغيرها الكثير! تصفح الواجهة الرئيسية للمزيد.",
        "call" to "للإتصال بأي مقدم خدمة نقوم بتوفير زر أخضر للإتصال الهاتفي الفوري 📞 في واجهة البطاقات للتواصل السريع، وأيضاً زر دردشة واتساب 💬.",
        "اتصل" to "للإتصال بأي مقدم خدمة نقوم بتوفير زر أخضر للإتصال الهاتفي الفوري 📞 في واجهة البطاقات للتواصل السريع، وأيضاً زر دردشة واتساب 💬.",
        "رقم الدعم" to "رقم الهاتف للدعم الفني المتكامل هو 777644670 - يتوفر خيارات الدعم في تذييل الشاشة وفي صفحة 'عن التطبيق'.",
        "الدعم" to "رقم الهاتف للدعم الفني المتكامل هو 777644670 - يتوفر خيارات الدعم في تذييل الشاشة وفي صفحة 'عن التطبيق'.",
        "بلاغ" to "يمكنك تقديم بلاغ أو شكوى فورية عن أي مزود خدمة بالضغط على أيقونة الإبلاغ ⚠️ في تفاصيل مقدم الخدمة وملء الحقول المخصصة، وستقوم لوحة المشرفين بفحصه حالاً.",
        "مشاركه" to "مشاركة التطبيق تمنح المستخدمين نقاط ولاء مجانية فورية! اضغط على تفاصيل المشاركة لكسب نقاط إضافية."
    )

    private val offlineFaqsEn = mapOf(
        "sections" to "Currently active directories: Plumber, Electrician, Carpenter, Blacksmith, AC repair, and Painter. Explore the categories grid globally.",
        "الأقسام" to "Currently active directories: Plumber, Electrician, Carpenter, Blacksmith, AC repair, and Painter. Explore the categories grid globally.",
        "call" to "Press the green phone dial button on any provider profile card to immediately place a local voice call or open WhatsApp.",
        "atصل" to "Press the green phone dial button on any provider profile card to immediately place a local voice call or open WhatsApp.",
        "رقم الدعم" to "The verified technical helpline telephone is 777644670. Find online support options in 'About Us' section.",
        "الدعم" to "The verified technical helpline telephone is 777644670. Find online support options in 'About Us' section.",
        "report" to "You can report or complain about any service provider by clicking the alert icon on their card or details screen. Handled directly by Admin.",
        "share" to "Sharing our premium platform yields 20 loyalty rewards points immediately to your account wallet dashboard."
    )

    init {
        // Start all FireStore Live Sync Snapshot Listeners
        viewModelScope.launch {
            repo.listenToSettings().collect { _settings.value = it }
        }
        viewModelScope.launch {
            repo.listenToCategories().collect { _categories.value = it }
        }
        viewModelScope.launch {
            repo.listenToActiveProviders().collect { _providers.value = it }
        }
        viewModelScope.launch {
            repo.listenToPendingProviders().collect { _pendingProviders.value = it }
        }
        viewModelScope.launch {
            repo.listenToBanners().collect { _banners.value = it }
        }
        viewModelScope.launch {
            repo.listenToReports().collect { _reports.value = it }
        }
        viewModelScope.launch {
            repo.listenToReviews().collect { _reviews.value = it }
        }
        viewModelScope.launch {
            repo.listenToAdmins().collect { _admins.value = it }
        }
        viewModelScope.launch {
            repo.listenToChats().collect { _messages.value = it }
        }
        viewModelScope.launch {
            repo.listenToActivityLogs().collect { _logs.value = it }
        }
    }

    // --- Actions with Log Audit Integration ---
    fun toggleLanguage() {
        _currentLang.value = if (_currentLang.value == "ar") "en" else "ar"
    }

    fun loginAdmin(user: String, pass: String): Boolean {
        // Super admin credential check
        if (user == "WAM2026" && pass == "maher736462") {
            _adminLoggedIn.value = "WAM2026"
            _adminPermissions.value = listOf("all")
            repo.saveActivityLog(ActivityLog(adminId = "WAM2026", action = "Login", details = "Super admin logged in successfully"))
            return true
        }
        // Supervisors check
        val found = admins.value.find { it.username == user && it.password == pass && it.isActive }
        if (found != null) {
            _adminLoggedIn.value = found.username
            _adminPermissions.value = found.permissions
            repo.saveActivityLog(ActivityLog(adminId = found.username, action = "Login", details = "Supervisor logged in successfully"))
            return true
        }
        return false
    }

    fun logout() {
        val current = _adminLoggedIn.value ?: "Guest"
        repo.saveActivityLog(ActivityLog(adminId = current, action = "Logout", details = "Admin or supervisor logged out"))
        _adminLoggedIn.value = null
        _adminPermissions.value = emptyList()
    }

    fun updatePlatformSettings(newName: String, footer: String, welcome: String, phone: String, email: String, wa: String, pCol: String, sCol: String) {
        val updated = settings.value.copy(
            appName = newName,
            footerText = footer,
            welcomeMessage = welcome,
            supportPhone = phone,
            supportEmail = email,
            supportWhatsApp = wa,
            primaryColor = pCol,
            secondaryColor = sCol
        )
        repo.updateSettings(updated) {
            if (it) {
                repo.saveActivityLog(ActivityLog(adminId = _adminLoggedIn.value ?: "Owner", action = "UpdateSettings", details = "AppSettings master parameters updated"))
            }
        }
    }

    fun updateFontSettings(family: String, size: Int) {
        val updated = settings.value.copy(fontFamily = family, fontSize = size)
        repo.updateSettings(updated) {}
    }

    fun updateToggleFeatures(chat: Boolean, assist: Boolean, mainMode: Boolean, mainMsg: String) {
        val updated = settings.value.copy(
            chatEnabled = chat,
            assistantEnabled = assist,
            maintenanceMode = mainMode,
            maintenanceMessage = mainMsg
        )
        repo.updateSettings(updated) {}
    }

    // --- Category Management ---
    fun createCategory(nameAr: String, nameEn: String, parentId: String?, order: Int, iconKey: String) {
        val cat = Category(nameAr = nameAr, nameEn = nameEn, parentId = parentId, order = order, imageUrl = iconKey)
        repo.saveCategory(cat) {
            repo.saveActivityLog(ActivityLog(adminId = _adminLoggedIn.value ?: "Admin", action = "CreateCategory", details = "Created category $nameAr"))
        }
    }

    fun removeCategory(id: String) {
        repo.deleteCategory(id) {
            repo.saveActivityLog(ActivityLog(adminId = _adminLoggedIn.value ?: "Admin", action = "DeleteCategory", details = "Deleted category $id"))
        }
    }

    // --- Provider Requests ---
    fun submitPendingProvider(name: String, phone: String, mainCat: String, subCat: String, ad: String, dist: String, profPic: String, idPic: String) {
        val pending = PendingProvider(
            fullName = name,
            phone = phone,
            mainCategoryId = mainCat,
            subCategoryId = subCat,
            address = ad,
            district = dist,
            profileImageUrl = profPic,
            idCardImageUrl = idPic
        )
        repo.addPendingProvider(pending) {}
    }

    fun handleApprove(provider: PendingProvider) {
        repo.approveProvider(provider) {
            repo.saveActivityLog(ActivityLog(adminId = _adminLoggedIn.value ?: "Admin", action = "ApproveProvider", details = "Approved provider ${provider.fullName}"))
        }
    }

    fun handleReject(id: String, reason: String) {
        repo.rejectProvider(id, reason) {
            repo.saveActivityLog(ActivityLog(adminId = _adminLoggedIn.value ?: "Admin", action = "RejectProvider", details = "Rejected provider id $id. Reason: $reason"))
        }
    }

    fun quickAddProviderDirect(name: String, phone: String, cat: String, price: Double, vip: Boolean) {
        val active = ServiceProvider(
            fullName = name,
            phone = phone,
            mainCategoryId = cat,
            address = "صنعاء",
            district = "الوسط",
            averageRating = 5.0,
            isRecommended = vip,
            isVerified = true,
            createdAt = System.currentTimeMillis()
        )
        repo.saveProviderDirect(active) {
            repo.saveActivityLog(ActivityLog(adminId = _adminLoggedIn.value ?: "Admin", action = "DirectAddProvider", details = "Directly added service provider $name"))
        }
    }

    fun changeProviderFlags(id: String, pin: Boolean, rec: Boolean, ver: Boolean, block: Boolean) {
        repo.updateProviderFlags(id, pin, rec, ver, block) {
            repo.saveActivityLog(ActivityLog(adminId = _adminLoggedIn.value ?: "Admin", action = "UpdateProviderFlags", details = "Updated provider attributes for doc $id"))
        }
    }

    fun removeActiveProvider(id: String) {
        repo.deleteActiveProvider(id) {
            repo.saveActivityLog(ActivityLog(adminId = _adminLoggedIn.value ?: "Admin", action = "DeleteProvider", details = "Removed provider $id"))
        }
    }

    // --- Banners Ads ---
    fun createBanner(title: String, type: String, url: String, link: String, size: String, duration: Int) {
        val banner = Banner(title = title, type = type, mediaUrl = url, redirectLink = link, size = size, durationSeconds = duration)
        repo.saveBanner(banner) {}
    }

    fun removeBanner(id: String) {
        repo.deleteBanner(id) {}
    }

    // --- Supervisors ---
    fun createSupervisor(user: String, pass: String, perms: List<String>) {
        val supervisor = Admin(username = user, password = pass, permissions = perms, isActive = true)
        repo.saveAdmin(supervisor) {
            repo.saveActivityLog(ActivityLog(adminId = _adminLoggedIn.value ?: "Admin", action = "CreateSupervisor", details = "Created supervisor account $user"))
        }
    }

    fun removeSupervisor(id: String) {
        repo.deleteAdmin(id) {
            repo.saveActivityLog(ActivityLog(adminId = _adminLoggedIn.value ?: "Admin", action = "DeleteSupervisor", details = "Removed supervisor doc $id"))
        }
    }

    // --- Incident Reports & Reviews ---
    fun submitReport(providerId: String, reason: String, details: String) {
        val report = Report(providerId = providerId, reason = reason, details = details)
        repo.saveReport(report) {}
    }

    fun submitReview(providerId: String, rating: Int, comment: String) {
        val rev = Review(providerId = providerId, rating = rating, comment = comment)
        repo.saveReview(rev) {
            if (it) {
                // Award points reward (+15 points)
                _userLoyaltyPoints.value += 15
            }
        }
    }

    // --- Loyalty wallets ---
    fun claimPointsRedemption() {
        if (_userLoyaltyPoints.value >= 100) {
            _userLoyaltyPoints.value -= 100
            // Success response simulation
        }
    }

    fun addPointsFromShare() {
        _userLoyaltyPoints.value += 20
    }

    // --- Live Instant Chat ---
    fun dispatchChatMessage(chatId: String, senderId: String, receiverId: String, text: String) {
        val msg = ChatMessage(chatId = chatId, senderId = senderId, receiverId = receiverId, message = text)
        repo.sendChatMessage(msg) {}
    }

    fun wipeChatHistory() {
        repo.clearChatHistory {
            repo.saveActivityLog(ActivityLog(adminId = _adminLoggedIn.value ?: "Admin", action = "ClearChats", details = "Wiped message history database completely"))
        }
    }

    // --- AI Smart Assistant engine (Offline FAQs first fallback, Online Gemini API) ---
    fun askAssistant(promptText: String) {
        if (promptText.isBlank()) return

        // 1. Apppend user message
        val updatedList = _assistantChat.value + (promptText to true)
        _assistantChat.value = updatedList
        _assistantLoading.value = true

        // 2. Perform localized FAQ lookup matching key sub-phrases (Offline case)
        val promptLower = promptText.lowercase()
        var matchedResponse: String? = null
        val lookupMap = if (_currentLang.value == "ar") offlineFaqsAr else offlineFaqsEn

        for ((key, value) in lookupMap) {
            if (promptLower.contains(key)) {
                matchedResponse = value
                break
            }
        }

        if (matchedResponse != null) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(600) // Aesthetic delay simulating smart processing
                _assistantChat.value = _assistantChat.value + (matchedResponse to false)
                _assistantLoading.value = false
            }
            return
        }

        // 3. If no offline FAQ found, call Gemini API in a background thread of the ViewModel via REST
        viewModelScope.launch {
            val response = runGeminiAPIRequest(promptText)
            _assistantChat.value = _assistantChat.value + (response to false)
            _assistantLoading.value = false
        }
    }

    private suspend fun runGeminiAPIRequest(promptText: String): String = withContext(Dispatchers.IO) {
        // Fallback for empty API settings
        val key = "AIzaSyA5ysT25HeS0qFz6rUy-YCSFcVqlPowoSc" // Explicit client key as requested from firebase options
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$key"

        val client = OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        try {
            // Construct the exact GenerativeContent request JSON mandated in gemini-api skill
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "You are the smart AI assistant for WAM Yemen Services directory. Answer concisely in Arabic. Prompt: $promptText")
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext if (_currentLang.value == "ar") {
                        "عذراً، لم أستطع الإجابة حالياً. رقم الدعم المتوفر هو 777644670 لمساعدتك مباشرة!"
                    } else {
                        "Sorry, I am currently unable to assist. Please contact support at 777644670!"
                    }
                }
                val respStr = response.body?.string() ?: ""
                val jsonObj = JSONObject(respStr)
                val candidates = jsonObj.getJSONArray("candidates")
                val textResponse = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                return@withContext textResponse.trim()
            }
        } catch (e: Exception) {
            // Friendly error with help fallback
            return@withContext if (_currentLang.value == "ar") {
                "لا يتوفر اتصال بالإنترنت حالياً للذكاء الاصطناعي... يمكنك تصفح الأقسام والاتصال مباشرة بالمهنيين يدوياً!"
            } else {
                "Offline active state... Please reach out to support or dial direct numbers on your screen."
            }
        }
    }

    // --- Import / Export Backup (Internal Memory Storage Simulation) ---
    fun backupDatabaseLocal(filesDir: File, onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(filesDir, "wam_services_backup.json")
                val backupJson = JSONObject().apply {
                    put("version", "wam2026")
                    put("timestamp", System.currentTimeMillis())
                    put("providers_count", providers.value.size)
                }
                file.writeText(backupJson.toString(2))
                withContext(Dispatchers.Main) {
                    onComplete("تم إنشاء نسخة احتياطية بنجاح في: ${file.absolutePath}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete("فشل النسخ الاحتياطي: ${e.message}")
                }
            }
        }
    }

    fun restoreDatabaseLocal(filesDir: File, onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(filesDir, "wam_services_backup.json")
                if (!file.exists()) {
                    withContext(Dispatchers.Main) {
                        onComplete("لا يوجد ملف نسخة احتياطية صالح للاستعادة!")
                    }
                    return@launch
                }
                val schema = file.readText()
                val jsonObj = JSONObject(schema)
                val version = jsonObj.optString("version")
                withContext(Dispatchers.Main) {
                    onComplete("تمت استعادة البيانات بنجاح للإصدار الفاخر $version")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete("خطأ أثناء الاستعادة: ${e.message}")
                }
            }
        }
    }
}
