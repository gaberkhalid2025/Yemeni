package com.wam.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class AppRepository {
    private val db = Firebase.firestore

    init {
        // Explicitly enable Offline Persistence
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            db.firestoreSettings = settings
        } catch (e: Exception) {
            // Already initialized settings (e.g. in tests/multiple calls)
        }
    }

    // --- Live Snapshot Sync Flow for AppSetting ---
    fun listenToSettings(): Flow<AppSettings> = callbackFlow {
        val listener = db.collection("app_settings").document("master")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val settings = snapshot.toObject(AppSettings::class.java)
                    if (settings != null) {
                        trySend(settings)
                    }
                } else {
                    // Initialize if empty
                    val defaultSettings = AppSettings()
                    db.collection("app_settings").document("master").set(defaultSettings)
                    trySend(defaultSettings)
                }
            }
        awaitClose { listener.remove() }
    }

    fun updateSettings(settings: AppSettings, onComplete: (Boolean) -> Unit) {
        db.collection("app_settings").document("master")
            .set(settings)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // --- Live Categories (Snapshot Listener Flow) ---
    fun listenToCategories(): Flow<List<Category>> = callbackFlow {
        val listener = db.collection("categories")
            .orderBy("order", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val categories = snapshot.toObjects(Category::class.java)
                    trySend(categories)
                }
            }
        awaitClose { listener.remove() }
    }

    fun saveCategory(category: Category, onComplete: (Boolean) -> Unit) {
        val docId = category.id.ifEmpty { UUID.randomUUID().toString() }
        val finalCategory = category.copy(id = docId)
        db.collection("categories").document(docId)
            .set(finalCategory)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun deleteCategory(id: String, onComplete: (Boolean) -> Unit) {
        db.collection("categories").document(id)
            .delete()
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // --- Live Providers (Snapshot Listener Flows) ---
    fun listenToActiveProviders(): Flow<List<ServiceProvider>> = callbackFlow {
        val listener = db.collection("service_providers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(ServiceProvider::class.java)
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun listenToPendingProviders(): Flow<List<PendingProvider>> = callbackFlow {
        val listener = db.collection("pending_providers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(PendingProvider::class.java)
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun addPendingProvider(provider: PendingProvider, onComplete: (Boolean) -> Unit) {
        val id = provider.id.ifEmpty { UUID.randomUUID().toString() }
        val finalProvider = provider.copy(id = id)
        db.collection("pending_providers").document(id)
            .set(finalProvider)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun approveProvider(pending: PendingProvider, onComplete: (Boolean) -> Unit) {
        val active = ServiceProvider(
            id = pending.id,
            fullName = pending.fullName,
            phone = pending.phone,
            mainCategoryId = pending.mainCategoryId,
            subCategoryId = pending.subCategoryId,
            address = pending.address,
            district = pending.district,
            lat = pending.lat,
            lng = pending.lng,
            profileImageUrl = pending.profileImageUrl,
            idCardImageUrl = pending.idCardImageUrl,
            isVerified = true,
            createdAt = System.currentTimeMillis()
        )
        // 1. Write active
        db.collection("service_providers").document(pending.id)
            .set(active)
            .addOnSuccessListener {
                // 2. Delete pending
                db.collection("pending_providers").document(pending.id).delete()
                onComplete(true)
            }
            .addOnFailureListener { onComplete(false) }
    }

    fun rejectProvider(id: String, reason: String, onComplete: (Boolean) -> Unit) {
        db.collection("pending_providers").document(id)
            .update("status", "rejected", "rejectReason", reason)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun saveProviderDirect(provider: ServiceProvider, onComplete: (Boolean) -> Unit) {
        val docId = provider.id.ifEmpty { UUID.randomUUID().toString() }
        val finalProvider = provider.copy(id = docId)
        db.collection("service_providers").document(docId)
            .set(finalProvider)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun updateProviderFlags(
        id: String,
        isPinned: Boolean,
        isRecommended: Boolean,
        isVerified: Boolean,
        isBlocked: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        db.collection("service_providers").document(id)
            .update(
                "isPinned", isPinned,
                "isRecommended", isRecommended,
                "isVerified", isVerified,
                "isBlocked", isBlocked
            )
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun deleteActiveProvider(id: String, onComplete: (Boolean) -> Unit) {
        db.collection("service_providers").document(id)
            .delete()
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // --- Live Banners ---
    fun listenToBanners(): Flow<List<Banner>> = callbackFlow {
        val listener = db.collection("banners")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(Banner::class.java)
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun saveBanner(banner: Banner, onComplete: (Boolean) -> Unit) {
        val docId = banner.id.ifEmpty { UUID.randomUUID().toString() }
        val finalBanner = banner.copy(id = docId)
        db.collection("banners").document(docId)
            .set(finalBanner)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun deleteBanner(id: String, onComplete: (Boolean) -> Unit) {
        db.collection("banners").document(id)
            .delete()
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // --- Live Reports ---
    fun listenToReports(): Flow<List<Report>> = callbackFlow {
        val listener = db.collection("reports")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(Report::class.java)
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun saveReport(report: Report, onComplete: (Boolean) -> Unit) {
        val docId = report.id.ifEmpty { UUID.randomUUID().toString() }
        val finalReport = report.copy(id = docId)
        db.collection("reports").document(docId)
            .set(finalReport)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // --- Live Reviews ---
    fun listenToReviews(): Flow<List<Review>> = callbackFlow {
        val listener = db.collection("reviews")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(Review::class.java)
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun saveReview(review: Review, onComplete: (Boolean) -> Unit) {
        val docId = review.id.ifEmpty { UUID.randomUUID().toString() }
        val finalReview = review.copy(id = docId)
        db.collection("reviews").document(docId)
            .set(finalReview)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // --- Live Admins ---
    fun listenToAdmins(): Flow<List<Admin>> = callbackFlow {
        val listener = db.collection("admins")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(Admin::class.java)
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun saveAdmin(admin: Admin, onComplete: (Boolean) -> Unit) {
        val docId = admin.id.ifEmpty { UUID.randomUUID().toString() }
        val finalAdmin = admin.copy(id = docId)
        db.collection("admins").document(docId)
            .set(finalAdmin)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun deleteAdmin(id: String, onComplete: (Boolean) -> Unit) {
        db.collection("admins").document(id)
            .delete()
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // --- Live Chat Messages ---
    fun listenToChats(): Flow<List<ChatMessage>> = callbackFlow {
        val listener = db.collection("chat_messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(ChatMessage::class.java)
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun sendChatMessage(msg: ChatMessage, onComplete: (Boolean) -> Unit) {
        val id = UUID.randomUUID().toString()
        val finalMsg = msg.copy(id = id, timestamp = System.currentTimeMillis())
        db.collection("chat_messages").document(id)
            .set(finalMsg)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun clearChatHistory(onComplete: (Boolean) -> Unit) {
        db.collection("chat_messages").get().addOnSuccessListener { snapshot ->
            val batch = db.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().addOnCompleteListener { onComplete(it.isSuccessful) }
        }.addOnFailureListener { onComplete(false) }
    }

    // --- Activity Logs ---
    fun listenToActivityLogs(): Flow<List<ActivityLog>> = callbackFlow {
        val listener = db.collection("activity_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(ActivityLog::class.java)
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun saveActivityLog(log: ActivityLog) {
        val id = UUID.randomUUID().toString()
        val finalLog = log.copy(id = id, timestamp = System.currentTimeMillis())
        db.collection("activity_logs").document(id).set(finalLog)
    }
}
