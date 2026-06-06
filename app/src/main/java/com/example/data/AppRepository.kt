package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class AppRepository(private val appDao: AppDao) {

    // --- Reactive Flows ---
    val categories: Flow<List<Category>> = appDao.getAllCategories()
    val allServiceProviders: Flow<List<ServiceProvider>> = appDao.getAllServiceProvidersFlow()
    val approvedServiceProviders: Flow<List<ServiceProvider>> = appDao.getApprovedServiceProvidersFlow()
    val pendingServiceProviders: Flow<List<ServiceProvider>> = appDao.getPendingServiceProvidersFlow()
    val activeBanners: Flow<List<Banner>> = appDao.getActiveBanners()
    val allBanners: Flow<List<Banner>> = appDao.getAllBanners()
    val reports: Flow<List<Report>> = appDao.getAllReports()
    val activityLogs: Flow<List<ActivityLog>> = appDao.getAllActivityLogs()
    val faqs: Flow<List<FAQItem>> = appDao.getAllFAQs()
    val settingsFlow: Flow<AppSettings?> = appDao.getSettingsFlow()

    // --- Direct Suspend Database Actions ---
    suspend fun getSettingsDirect(): AppSettings {
        return appDao.getSettingsDirect() ?: AppSettings().also {
            appDao.insertSettings(it)
        }
    }

    suspend fun insertSettings(settings: AppSettings) {
        appDao.insertSettings(settings)
    }

    suspend fun insertCategory(category: Category) {
        appDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        appDao.deleteCategory(category)
    }

    suspend fun deleteCategoryById(id: String) {
        appDao.deleteCategoryById(id)
    }

    suspend fun insertServiceProvider(provider: ServiceProvider) {
        appDao.insertServiceProvider(provider)
    }

    suspend fun getServiceProviderById(id: String): ServiceProvider? {
        return appDao.getServiceProviderById(id)
    }

    suspend fun updateProviderStatus(id: String, status: String, reason: String?) {
        appDao.updateProviderStatus(id, status, reason)
    }

    suspend fun updateProviderPinned(id: String, isPinned: Boolean) {
        appDao.updateProviderPinned(id, isPinned)
    }

    suspend fun updateProviderRecommended(id: String, isRecommended: Boolean) {
        appDao.updateProviderRecommended(id, isRecommended)
    }

    suspend fun updateProviderVerified(id: String, isVerified: Boolean) {
        appDao.updateProviderVerified(id, isVerified)
    }

    suspend fun updateProviderSubscription(id: String, isSubscribed: Boolean, expiry: Long?) {
        appDao.updateProviderSubscription(id, isSubscribed, expiry)
    }

    suspend fun updateProviderBanned(id: String, isBanned: Boolean) {
        appDao.updateProviderBanned(id, isBanned)
    }

    suspend fun deleteServiceProviderById(id: String) {
        appDao.deleteServiceProviderById(id)
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

    // --- DB Seeding ---
    suspend fun seedInitialDataIfEmpty() {
        val existingSettings = appDao.getSettingsDirect()
        if (existingSettings == null) {
            // Seed settings
            appDao.insertSettings(AppSettings())
        }

        val categoriesCount = appDao.getAllCategories().firstOrNull() ?: emptyList()
        if (categoriesCount.isEmpty()) {
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
                appDao.insertCategory(cat)
            }
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

        val providersCount = appDao.getAllServiceProviders()
        if (providersCount.isEmpty()) {
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
                appDao.insertServiceProvider(prov)
            }
        }

        val bannersCount = appDao.getAllBanners().firstOrNull() ?: emptyList()
        if (bannersCount.isEmpty()) {
            appDao.insertBanner(Banner("banner_1", "text", "حمل تطبيق WAM للحصول على أفضل خدمات محلية في اليمن", "L", 5, "", "top", true))
            appDao.insertBanner(Banner("banner_2", "text", "خصم 20% عند طلب صيانة المكيفات المنزلية هذا الأسبوع!", "M", 6, "", "top", true))
        }
    }
}
