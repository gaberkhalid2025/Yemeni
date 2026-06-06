package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val imageUri: String,
    val displayOrder: Int = 0,
    val parentCategoryId: String? = null // if null, it is a main category. If not null, it's a subcategory
) : Serializable

@Entity(tableName = "service_providers")
data class ServiceProvider(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val mainCategory: String,
    val subCategory: String,
    val workAddress: String,
    val residenceArea: String,
    val latitude: Double = 15.3694, // Default to Sana'a capital latitude
    val longitude: Double = 44.1910, // Sana'a longitude
    val profileImage: String,
    val idCardImage: String? = null,
    val status: String = "pending", // pending, approved, rejected
    val rejectionReason: String? = null,
    val isPinned: Boolean = false,
    val isRecommended: Boolean = false,
    val isVerified: Boolean = false,
    val isSubscribed: Boolean = false,
    val subscriptionExpiry: Long? = null,
    val ratingSum: Int = 0,
    val ratingCount: Int = 0,
    val isBanned: Boolean = false,
    val gender: String = "male", // male or female
    val chatSuspended: Boolean = false // If moderator/admin suspends direct messaging
) : Serializable

@Entity(tableName = "banners")
data class Banner(
    @PrimaryKey val id: String,
    val type: String, // image, video, text
    val content: String, // image path or text message
    val size: String = "M", // S, M, L
    val duration: Int = 5, // auto-hide seconds
    val linkUrl: String = "",
    val position: String = "top",
    val isActive: Boolean = true
) : Serializable

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey val id: String,
    val providerId: String,
    val providerName: String,
    val reporterName: String,
    val reporterPhone: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String,
    val chatId: String, // format: "user_provider" or similar
    val senderId: String,
    val receiverId: String,
    val message: String,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
) : Serializable

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey val id: String,
    val adminName: String,
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "faq_items")
data class FAQItem(
    @PrimaryKey val id: String,
    val questionAr: String,
    val answerAr: String,
    val questionEn: String,
    val answerEn: String
) : Serializable

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: String = "global_settings",
    val appName: String = "WAM Services",
    val primaryColor: String = "Cosmic Silver", // Cosmic Silver, Luxury Gold, Emerald Green, or custom hex
    val secondaryColor: String = "Default",
    val footerText: String = "WAM777644670",
    val welcomeMessage: String = "مرحباً بكم في منصة دليل مقدمي الخدمات اليمني الأول",
    val supportPhone: String = "777644670",
    val supportEmail: String = "support@wam.ye",
    val supportWhatsApp: String = "777644670",
    val adminPassword: String = "maher736462", // Master admin settings
    val isMaintenanceMode: Boolean = false,
    val maintenanceMessage: String = "التطبيق قيد الصيانة الطارئة حالياً. نرجو العودة لاحقاً.",
    val is2FAEnabled: Boolean = false,
    val whitelistedDevices: String = "", // Comma-separated ids
    val topBarIconsArrangement: String = "Home,Login,Register,Language,Refresh", // Rearrage layout csv
    val chatButtonHidden: Boolean = false,
    val chatButtonSize: Int = 50, // width in dp
    val chatButtonPosition: String = "bottom_right",
    val loyaltyPointsEnabled: Boolean = true,
    val pointsPerRating: Int = 10,
    val pointsPerShare: Int = 20,
    val userPoints: Int = 0,
    val allowGuestMode: Boolean = true,
    val dataSaverMode: Boolean = false,
    val supportIconSize: Int = 50,
    val supportIconVisible: Boolean = true,
    val fontColor: String = "#FFFFFF",
    val fontType: String = "Bold",
    val fontSize: Int = 14,
    val footerOpacity: Float = 1.0f,
    val footerHeightScale: Int = 56,
    val footerFontSize: Int = 12,
    val cumulativeCallsCount: Int = 0,
    
    // NEW REAL-TIME CHAT & DYNAMIC OVERLAYS CONFIGS
    val isChatServiceDisabled: Boolean = false,
    val chatServiceDisabledReason: String = "تم إيقاف خدمة المحادثات الفورية مؤقتاً لتحديث النظام بقرار من الإدارة.",
    val assistantIconSymbol: String = "Face", // Face, Star, Build, Info, Lock
    val assistantIconGlow: Boolean = false,
    val liveChatIconSymbol: String = "Mail", // Mail, Chat, Build, Lock, Menu
    val liveChatIconGlow: Boolean = false,
    val iconVisualEffectType: String = "Pulse" // Pulse, Rotate, Shake, None
) : Serializable

@Entity(tableName = "moderators")
data class Moderator(
    @PrimaryKey val username: String,
    val password: String,
    val role: String = "moderator", // owner, admin, moderator
    val canEditCategories: Boolean = true,
    val canDeleteProviders: Boolean = true,
    val canManageSettings: Boolean = true,
    val isActive: Boolean = true
) : Serializable
