package com.wam.data

import androidx.annotation.Keep

@Keep
data class AppSettings(
    val id: String = "global_settings",
    val appName: String = "Yemen Services",
    val primaryColor: String = "Cosmic Silver",
    val secondaryColor: String = "Default",
    val footerText: String = "MAW 777644670",
    val welcomeMessage: String = "مرحباً بكم في منصة دليل مقدمي الخدمات اليمني الأول",
    val supportPhone: String = "777644670",
    val supportEmail: String = "support@wam.ye",
    val supportWhatsApp: String = "777644670",
    val adminPassword: String = "maher736462",
    val isMaintenanceMode: Boolean = false,
    val maintenanceMessage: String = "التطبيق قيد الصيانة الطارئة حالياً. نرجو العودة لاحقاً.",
    val topBarIconsArrangement: String = "Home,Login,Register,Language,Refresh",
    val chatButtonHidden: Boolean = false,
    val chatButtonSize: Int = 50,
    val chatButtonPosition: String = "bottom_right",
    val allowGuestMode: Boolean = true,
    val footerOpacity: Float = 1.0f,
    val footerHeightScale: Int = 56,
    val footerFontSize: Int = 12,
    val isChatServiceDisabled: Boolean = false,
    val chatServiceDisabledReason: String = "تم إيقاف خدمة المحادثات الفورية مؤقتاً لتحديث النظام بقرار من الإدارة.",
    val assistantIconSymbol: String = "Face",
    val assistantIconGlow: Boolean = false,
    val liveChatIconSymbol: String = "Mail",
    val liveChatIconGlow: Boolean = false,
    val iconVisualEffectType: String = "Pulse"
)

@Keep
data class Moderator(
    val username: String = "",
    val password: String = "",
    val role: String = "moderator",
    val canEditCategories: Boolean = true,
    val canDeleteProviders: Boolean = true,
    val canManageSettings: Boolean = true,
    val isActive: Boolean = true
)

@Keep
data class ServiceProvider(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val categoryId: String = "",
    val residenceArea: String = "",
    val workAddress: String = "",
    val gender: String = "male", // male or female
    val isVerified: Boolean = false,
    val isPinned: Boolean = false,
    val isSubscribed: Boolean = false,
    val ratingSum: Int = 0,
    val ratingCount: Int = 0,
    val status: String = "active" // active, inactive
)

@Keep
data class Category(
    val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val iconHex: String = "🔧"
)

@Keep
data class ChatMessage(
    val id: String = "",
    val sender: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isToAssistant: Boolean = false
)

@Keep
data class FAQItem(
    val id: String = "",
    val questionAr: String = "",
    val questionEn: String = "",
    val answerAr: String = "",
    val answerEn: String = ""
)

@Keep
data class ActivityLog(
    val id: String = "",
    val username: String = "",
    val actionType: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
