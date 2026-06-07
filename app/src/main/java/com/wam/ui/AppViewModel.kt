package com.wam.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wam.data.ActivityLog
import com.wam.data.AppSettings
import com.wam.data.Category
import com.wam.data.ChatMessage
import com.wam.data.FAQItem
import com.wam.data.Moderator
import com.wam.data.AppRepository
import com.wam.data.ServiceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository()
    private val prefs = application.getSharedPreferences("WAM_PREFS", Context.MODE_PRIVATE)

    // Language state (Persisted locally for user convenience)
    private val _currentLanguage = MutableStateFlow(prefs.getString("APP_LANG", "ar") ?: "ar")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // Screen navigation flow
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 2. Admin & Moderator Session Controls (Persisted locally for logging in)
    private val _adminSession = MutableStateFlow<String?>(prefs.getString("LOGGED_ADMIN", null))
    val adminSession: StateFlow<String?> = _adminSession.asStateFlow()

    private val _isBackdoorActive = MutableStateFlow(false)
    val isBackdoorActive: StateFlow<Boolean> = _isBackdoorActive.asStateFlow()

    // State Flows bound to Snapshot Listeners on Cloud Firestore
    val settings: StateFlow<AppSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val categories: StateFlow<List<Category>> = repository.categoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProviders: StateFlow<List<ServiceProvider>> = repository.providersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moderators: StateFlow<List<Moderator>> = repository.allModerators
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = repository.allChatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val faqs: StateFlow<List<FAQItem>> = repository.allFAQs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<ActivityLog>> = repository.activityLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered providers combining live streams + searches + category filters
    val filteredProviders: StateFlow<List<ServiceProvider>> = combine(
        allProviders,
        _selectedCategory,
        _searchQuery
    ) { providers, catId, query ->
        providers.filter { provider ->
            val matchesCategory = catId == null || provider.categoryId == catId
            val matchesSearch = query.isEmpty() ||
                    provider.name.contains(query, ignoreCase = true) ||
                    provider.phone.contains(query, ignoreCase = true) ||
                    provider.residenceArea.contains(query, ignoreCase = true) ||
                    provider.workAddress.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Status loading state
    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating.asStateFlow()

    init {
        // Seed and sync initial data on separate coroutine
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    // --- Localization Language Toggle ---
    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
        prefs.edit().putString("APP_LANG", lang).apply()
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategory.value = categoryId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Authentication & Session Logs ---
    fun attemptLogin(usernameInput: String, passwordInput: String, rememberMe: Boolean): Boolean {
        val trimmedUser = usernameInput.trim()
        val currentSettings = settings.value

        // 1. Check Root Owner/Backdoor Credentials (configured in Firestore)
        if (trimmedUser.equals("Owner", ignoreCase = true) || trimmedUser.equals("WAM2026", ignoreCase = true)) {
            val rootPass = currentSettings.adminPassword
            if (passwordInput == rootPass) {
                _adminSession.value = "Owner"
                if (rememberMe) {
                    prefs.edit().putString("LOGGED_ADMIN", "Owner").apply()
                }
                _isBackdoorActive.value = true
                viewModelScope.launch {
                    insertActivityLog("Owner", "Owner Sign In", "System Root Creator authenticated with backend credentials")
                }
                return true
            }
        }

        // 2. Check Database-Stored Moderators
        val matchedMod = moderators.value.find { it.username.equals(trimmedUser, ignoreCase = true) }
        if (matchedMod != null && matchedMod.password == passwordInput && matchedMod.isActive) {
            _adminSession.value = matchedMod.username
            if (rememberMe) {
                prefs.edit().putString("LOGGED_ADMIN", matchedMod.username).apply()
            }
            _isBackdoorActive.value = false
            viewModelScope.launch {
                insertActivityLog(matchedMod.username, "Moderator Sign In", "Moderator ${matchedMod.username} successfully logged into dashboard")
            }
            return true
        }

        return false
    }

    fun logout() {
        val prevAdmin = _adminSession.value ?: "Unknown"
        _adminSession.value = null
        _isBackdoorActive.value = false
        prefs.edit().remove("LOGGED_ADMIN").apply()
        viewModelScope.launch {
            insertActivityLog(prevAdmin, "Admin Sign Out", "Administrator terminated active session from device")
        }
    }

    // Retrieve full capabilities of active user
    fun getActiveUserPermissions(): Moderator {
        val current = _adminSession.value ?: return Moderator("", "", "guest", false, false, false, false)
        if (current == "Owner") {
            return Moderator("Owner", "", "owner", canEditCategories = true, canDeleteProviders = true, canManageSettings = true, isActive = true)
        }
        val modRef = moderators.value.find { it.username.equals(current, ignoreCase = true) }
        return modRef ?: Moderator(current, "", "moderator", canEditCategories = false, canDeleteProviders = false, canManageSettings = false, isActive = false)
    }

    // --- Real-time Actions via Cloud Firestore ---

    // 1. Settings mutation
    fun saveSettings(appSettings: AppSettings) {
        viewModelScope.launch {
            repository.insertSettings(appSettings)
            insertActivityLog(_adminSession.value ?: "System", "Settings Update", "Application settings modified, synced with Firestore")
        }
    }

    // 2. Categories mutations
    fun addOrUpdateCategory(id: String, nameAr: String, nameEn: String, iconHex: String) {
        viewModelScope.launch {
            val cat = Category(id = id, nameAr = nameAr, nameEn = nameEn, iconHex = iconHex)
            repository.insertCategory(cat)
            insertActivityLog(_adminSession.value ?: "Admin", "Category Mutate", "Category $nameAr added/modified in repository")
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
            insertActivityLog(_adminSession.value ?: "Admin", "Category Delete", "Category ID $categoryId removed from list")
        }
    }

    // 3. Service Providers mutations
    fun addOrUpdateProvider(
        id: String,
        name: String,
        phone: String,
        categoryId: String,
        residenceArea: String,
        workAddress: String,
        gender: String,
        isVerified: Boolean,
        isPinned: Boolean,
        isSubscribed: Boolean,
        status: String
    ) {
        viewModelScope.launch {
            val provider = ServiceProvider(
                id = id,
                name = name,
                phone = phone,
                categoryId = categoryId,
                residenceArea = residenceArea,
                workAddress = workAddress,
                gender = gender,
                isVerified = isVerified,
                isPinned = isPinned,
                isSubscribed = isSubscribed,
                status = status,
                ratingSum = 25, // default starter rating stars sum
                ratingCount = 5  // default starter 5 rating reviews of 5-star avg
            )
            repository.insertProvider(provider)
            insertActivityLog(_adminSession.value ?: "Admin", "Provider Mutate", "Service expert $name successfully added/synchronized")
        }
    }

    fun deleteProvider(providerId: String) {
        viewModelScope.launch {
            repository.deleteProvider(providerId)
            insertActivityLog(_adminSession.value ?: "Admin", "Provider Delete", "Removed Service Provider ID $providerId from database")
        }
    }

    fun toggleProviderStatus(provider: ServiceProvider, statusField: String) {
        viewModelScope.launch {
            val updated = when (statusField) {
                "verified" -> provider.copy(isVerified = !provider.isVerified)
                "pinned" -> provider.copy(isPinned = !provider.isPinned)
                "subscribed" -> provider.copy(isSubscribed = !provider.isSubscribed)
                "status_active" -> provider.copy(status = "active")
                "status_inactive" -> provider.copy(status = "inactive")
                else -> provider
            }
            repository.insertProvider(updated)
            insertActivityLog(_adminSession.value ?: "Admin", "Provider Toggle", "Toggled field $statusField for provider ${provider.name}")
        }
    }

    fun rateProvider(providerId: String, starRating: Int) {
        viewModelScope.launch {
            repository.updateProviderRating(providerId, starRating)
        }
    }

    // 4. Moderators mutations
    fun addOrUpdateModerator(moderator: Moderator) {
        viewModelScope.launch {
            repository.insertModerator(moderator)
            insertActivityLog(_adminSession.value ?: "Owner", "Moderator Mutate", "Moderator account ${moderator.username} added/updated")
        }
    }

    fun deleteModerator(username: String) {
        viewModelScope.launch {
            repository.deleteModeratorByUsername(username)
            insertActivityLog(_adminSession.value ?: "Owner", "Moderator Dismissal", "Terminated moderator account level $username")
        }
    }

    // 5. Instantly Synced Live Chat & Gemini Assistant Logic
    fun sendChatMessage(messageContent: String, isToAssistant: Boolean) {
        if (messageContent.trim().isEmpty()) return
        val senderName = _adminSession.value ?: "زائر"
        viewModelScope.launch {
            val userMsg = ChatMessage(
                sender = senderName,
                message = messageContent,
                isToAssistant = isToAssistant
            )
            repository.insertChatMessage(userMsg)

            // Trigger Real-time localized AI advice if targeted
            if (isToAssistant) {
                _isAiGenerating.value = true
                val promptPayload = "مستخدماً لهجة وصلاحيات الدليل اليمني للخدمات WAM: $messageContent"
                viewModelScope.launch(Dispatchers.IO) {
                    val aiResponse = callGeminiApiRest(promptPayload)
                    val aiMsg = ChatMessage(
                        sender = if (currentLanguage.value == "ar") "المساعد الذكي (Gemini AI)" else "Smart Gemini AI",
                        message = aiResponse,
                        isToAssistant = true
                    )
                    repository.insertChatMessage(aiMsg)
                    _isAiGenerating.value = false
                }
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatMessages()
            insertActivityLog(_adminSession.value ?: "System", "Chat Wipe", "Live chat session history purged")
        }
    }

    // Log tracking in system
    private suspend fun insertActivityLog(user: String, action: String, details: String) {
        val log = ActivityLog(username = user, actionType = action, details = details)
        repository.insertActivityLog(log)
    }

    // --- Direct, secure background REST helper for Gemini API ---
    private fun callGeminiApiRest(promptText: String): String {
        // Safe reflective capture of GEMINI_API_KEY from BuildConfig to avoid compile-time breaks
        val apiKey = try {
            val buildConfigClass = Class.forName("com.wam.BuildConfig")
            val apiKeyField = buildConfigClass.getField("GEMINI_API_KEY")
            apiKeyField.get(null) as? String ?: ""
        } catch (e: Exception) {
            ""
        }

        if (apiKey.trim().isEmpty()) {
            return "مرحباً بكم! يرجى تهيئة مفتاح الذكاء الاصطناعي (GEMINI_API_KEY) في لوحة الأسرار لاستخدام المساعد الذكي."
        }

        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            // Structured dynamic system instructions aligning with Yemen Services voice
            val systemInstr = "أنت رفيق ذكي وخبير في تلمس احتياجات دليل مقدمي الخدمات اليمني WAM. تجيب باللغة العربية بلهجة يمنية محبوبة ومحترمة وحافلة بالترحيب المهني اليماني الأصيل، وتساعد المستخدمين في العثور على المهندسين والسباكين والفنيين المناسبين. تجيب باختصار بحدود سطرين إلى ثلاثة أسطر."

            // Construct payload dynamically matching Generation REST API specs
            val requestBodyObj = JSONObject().apply {
                val contentsArray = org.json.JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = org.json.JSONArray().apply {
                            add(JSONObject().apply { put("text", promptText) })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val systemInstructionObj = JSONObject().apply {
                    val partsArray = org.json.JSONArray().apply {
                        add(JSONObject().apply { put("text", systemInstr) })
                    }
                    put("parts", partsArray)
                }
                put("systemInstruction", systemInstructionObj)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBodyObj.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    val responseStr = reader.readText()
                    val jsonObj = JSONObject(responseStr)
                    val candidate = jsonObj.getJSONArray("candidates")
                        .getJSONObject(0)
                    val contentObj = candidate.getJSONObject("content")
                    val partObj = contentObj.getJSONArray("parts").getJSONObject(0)
                    return partObj.getString("text")
                }
            } else {
                return "عذراً يا صاحبي، واجهتني مشكلة في الاتصال بالشبكة الاستشارية لـ Gemini (رمز الخطأ: $responseCode)"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "وقع خلل في الخادم أثناء معالجة استشارتك: ${e.localizedMessage}"
        }
    }
}
