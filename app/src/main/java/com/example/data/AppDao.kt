package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY displayOrder ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    // --- Service Providers ---
    @Query("SELECT * FROM service_providers")
    fun getAllServiceProvidersFlow(): Flow<List<ServiceProvider>>

    @Query("SELECT * FROM service_providers")
    suspend fun getAllServiceProviders(): List<ServiceProvider>

    @Query("SELECT * FROM service_providers WHERE id = :id")
    suspend fun getServiceProviderById(id: String): ServiceProvider?

    @Query("SELECT * FROM service_providers WHERE status = 'approved'")
    fun getApprovedServiceProvidersFlow(): Flow<List<ServiceProvider>>

    @Query("SELECT * FROM service_providers WHERE status = 'pending'")
    fun getPendingServiceProvidersFlow(): Flow<List<ServiceProvider>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceProvider(provider: ServiceProvider)

    @Query("UPDATE service_providers SET status = :status, rejectionReason = :reason WHERE id = :id")
    suspend fun updateProviderStatus(id: String, status: String, reason: String?)

    @Query("UPDATE service_providers SET isPinned = :isPinned WHERE id = :id")
    suspend fun updateProviderPinned(id: String, isPinned: Boolean)

    @Query("UPDATE service_providers SET isRecommended = :isRecommended WHERE id = :id")
    suspend fun updateProviderRecommended(id: String, isRecommended: Boolean)

    @Query("UPDATE service_providers SET isVerified = :isVerified WHERE id = :id")
    suspend fun updateProviderVerified(id: String, isVerified: Boolean)

    @Query("UPDATE service_providers SET isSubscribed = :isSubscribed, subscriptionExpiry = :expiry WHERE id = :id")
    suspend fun updateProviderSubscription(id: String, isSubscribed: Boolean, expiry: Long?)

    @Query("UPDATE service_providers SET isBanned = :isBanned WHERE id = :id")
    suspend fun updateProviderBanned(id: String, isBanned: Boolean)

    @Query("DELETE FROM service_providers WHERE id = :id")
    suspend fun deleteServiceProviderById(id: String)

    // --- Banners ---
    @Query("SELECT * FROM banners WHERE isActive = 1")
    fun getActiveBanners(): Flow<List<Banner>>

    @Query("SELECT * FROM banners")
    fun getAllBanners(): Flow<List<Banner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanner(banner: Banner)

    @Query("DELETE FROM banners WHERE id = :id")
    suspend fun deleteBannerById(id: String)

    // --- Reports & Complaints ---
    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<Report>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: Report)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteReportById(id: String)

    // --- Real-Time Chat ---
    @Query("SELECT * FROM chat_messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getChatMessagesFlow(chatId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE chatId = :chatId")
    suspend fun clearChatMessages(chatId: String)

    // --- Activity Logs ---
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllActivityLogs(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLog)

    // --- FAQ Items ---
    @Query("SELECT * FROM faq_items")
    fun getAllFAQs(): Flow<List<FAQItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFAQ(faq: FAQItem)

    @Query("DELETE FROM faq_items WHERE id = :id")
    suspend fun deleteFAQById(id: String)

    // --- Application Settings ---
    @Query("SELECT * FROM app_settings WHERE id = 'global_settings'")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 'global_settings'")
    suspend fun getSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettings)
}
