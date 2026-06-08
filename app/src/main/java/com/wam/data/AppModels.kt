package com.wam.data

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class Category(
    val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val imageUrl: String = "",
    val parentId: String? = null,
    val order: Int = 0,
    val isActive: Boolean = true
)

data class ServiceProvider(
    val id: String = "",
    val fullName: String = "",
    val phone: String = "",
    val mainCategoryId: String = "",
    val subCategoryId: String = "",
    val address: String = "",
    val district: String = "",
    val lat: Double = 15.3694, // Standard Sana'a Yemen fallback coordinate
    val lng: Double = 44.1910,
    val profileImageUrl: String = "",
    val idCardImageUrl: String = "",
    val isPinned: Boolean = false,
    val isRecommended: Boolean = false,
    val isVerified: Boolean = false,
    val isBlocked: Boolean = false,
    val averageRating: Double = 4.5,
    val totalReviews: Int = 1,
    val isSubscribed: Boolean = false,
    val subscriptionExpiry: Long = 0,
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class PendingProvider(
    val id: String = "",
    val fullName: String = "",
    val phone: String = "",
    val mainCategoryId: String = "",
    val subCategoryId: String = "",
    val address: String = "",
    val district: String = "",
    val lat: Double = 15.3694,
    val lng: Double = 44.1910,
    val profileImageUrl: String = "",
    val idCardImageUrl: String = "",
    val status: String = "pending", // pending / rejected
    val rejectReason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class AppSettings(
    val id: String = "master",
    val appName: String = "WAM Services",
    val footerText: String = "MAW 777644670",
    val welcomeMessage: String = "أهلاً ومرحباً بكم مع تطبيق دليل خدمات اليمن المعتمد",
    val supportPhone: String = "777644670",
    val supportEmail: String = "support@wam.services",
    val supportWhatsApp: String = "777644670",
    val primaryColor: String = "#004D40", // Emerald hex color default
    val secondaryColor: String = "#FFD700", // Luxurious Gold
    val fontFamily: String = "Tajawal",
    val fontSize: Int = 14,
    val chatEnabled: Boolean = true,
    val assistantEnabled: Boolean = true,
    val radiusSearchLimits: List<Int> = listOf(5, 10, 25, 50),
    val dataSaverMode: Boolean = false,
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "المنصة في صيانة طارئة مبرمجة حالياً... نعود لخدمتكم قريباً"
)

data class Banner(
    val id: String = "",
    val title: String = "",
    val type: String = "image", // image / text
    val mediaUrl: String = "",
    val redirectLink: String = "",
    val size: String = "M", // S/M/L
    val durationSeconds: Int = 5,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class Report(
    val id: String = "",
    val providerId: String = "",
    val userId: String = "Guest",
    val reason: String = "",
    val details: String = "",
    val status: String = "pending", // pending / action_taken
    val createdAt: Long = System.currentTimeMillis()
)

data class Review(
    val id: String = "",
    val providerId: String = "",
    val userId: String = "Guest",
    val rating: Int = 5,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class Admin(
    val id: String = "",
    val username: String = "",
    val password: String = "",
    val permissions: List<String> = emptyList(), // registrar, categories, banners, active, reports
    val isActive: Boolean = true
)

data class ActivityLog(
    val id: String = "",
    val adminId: String = "",
    val action: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
