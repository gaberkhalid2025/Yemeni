package com.wam.data

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class AppRepository {

    private val firestore = FirebaseFirestore.getInstance()

    // 1. Settings Flow (Real-time Snapshot Listener)
    val settingsFlow: Flow<AppSettings?> = callbackFlow {
        val docRef = firestore.collection("settings").document("global_settings")
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val s = snapshot.toObject(AppSettings::class.java)
                trySend(s)
            } else {
                trySend(AppSettings())
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun getSettingsDirect(): AppSettings = withContext(Dispatchers.IO) {
        try {
            val task = firestore.collection("settings").document("global_settings").get()
            val doc = Tasks.await(task)
            if (doc.exists()) {
                doc.toObject(AppSettings::class.java) ?: AppSettings()
            } else {
                AppSettings()
            }
        } catch (e: Exception) {
            AppSettings()
        }
    }

    suspend fun insertSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        val task = firestore.collection("settings").document("global_settings").set(settings)
        Tasks.await(task)
    }

    // 2. Categories Flow (Real-time Snapshot Listener)
    val categoriesFlow: Flow<List<Category>> = callbackFlow {
        val listener = firestore.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Category::class.java)?.copy(id = doc.id)
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getCategoriesList(): List<Category> = withContext(Dispatchers.IO) {
        try {
            val task = firestore.collection("categories").get()
            val snap = Tasks.await(task)
            snap.documents.mapNotNull { doc ->
                doc.toObject(Category::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun insertCategory(category: Category) = withContext(Dispatchers.IO) {
        val docId = if (category.id.isEmpty()) firestore.collection("categories").document().id else category.id
        val finalCategory = category.copy(id = docId)
        val task = firestore.collection("categories").document(docId).set(finalCategory)
        Tasks.await(task)
    }

    suspend fun deleteCategory(categoryId: String) = withContext(Dispatchers.IO) {
        val task = firestore.collection("categories").document(categoryId).delete()
        Tasks.await(task)
    }

    // 3. Service Providers Flow (Real-time Snapshot Listener)
    val providersFlow: Flow<List<ServiceProvider>> = callbackFlow {
        val listener = firestore.collection("providers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ServiceProvider::class.java)?.copy(id = doc.id)
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getProviderById(providerId: String): ServiceProvider? = withContext(Dispatchers.IO) {
        try {
            val task = firestore.collection("providers").document(providerId).get()
            val doc = Tasks.await(task)
            if (doc.exists()) {
                doc.toObject(ServiceProvider::class.java)?.copy(id = doc.id)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun insertProvider(provider: ServiceProvider) = withContext(Dispatchers.IO) {
        val docId = if (provider.id.isEmpty()) firestore.collection("providers").document().id else provider.id
        val finalProvider = provider.copy(id = docId)
        val task = firestore.collection("providers").document(docId).set(finalProvider)
        Tasks.await(task)
    }

    suspend fun updateProviderRating(providerId: String, starRating: Int) = withContext(Dispatchers.IO) {
        try {
            val p = getProviderById(providerId)
            if (p != null) {
                val updated = p.copy(
                    ratingSum = p.ratingSum + starRating,
                    ratingCount = p.ratingCount + 1
                )
                insertProvider(updated)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteProvider(providerId: String) = withContext(Dispatchers.IO) {
        val task = firestore.collection("providers").document(providerId).delete()
        Tasks.await(task)
    }

    // 4. Moderators Flow (Real-time Snapshot Listener)
    val allModerators: Flow<List<Moderator>> = callbackFlow {
        val listener = firestore.collection("moderators")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Moderator::class.java)?.copy(username = doc.id)
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getModeratorByUsername(username: String): Moderator? = withContext(Dispatchers.IO) {
        try {
            val task = firestore.collection("moderators").document(username).get()
            val doc = Tasks.await(task)
            if (doc.exists()) {
                doc.toObject(Moderator::class.java)?.copy(username = doc.id)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun insertModerator(moderator: Moderator) = withContext(Dispatchers.IO) {
        val task = firestore.collection("moderators").document(moderator.username).set(moderator)
        Tasks.await(task)
    }

    suspend fun deleteModeratorByUsername(username: String) = withContext(Dispatchers.IO) {
        val task = firestore.collection("moderators").document(username).delete()
        Tasks.await(task)
    }

    // 5. Chat Messages Flow (Real-time Snapshot Listener)
    val allChatMessages: Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection("chat_messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun insertChatMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        val docId = if (message.id.isEmpty()) firestore.collection("chat_messages").document().id else message.id
        val finalMsg = message.copy(id = docId)
        val task = firestore.collection("chat_messages").document(docId).set(finalMsg)
        Tasks.await(task)
    }

    suspend fun clearChatMessages() = withContext(Dispatchers.IO) {
        try {
            val task = firestore.collection("chat_messages").get()
            val snap = Tasks.await(task)
            for (doc in snap.documents) {
                firestore.collection("chat_messages").document(doc.id).delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 6. FAQs Flow
    val allFAQs: Flow<List<FAQItem>> = callbackFlow {
        val listener = firestore.collection("faqs")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(FAQItem::class.java)?.copy(id = doc.id)
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun insertFAQItem(faq: FAQItem) = withContext(Dispatchers.IO) {
        val docId = if (faq.id.isEmpty()) firestore.collection("faqs").document().id else faq.id
        val finalFaq = faq.copy(id = docId)
        val task = firestore.collection("faqs").document(docId).set(finalFaq)
        Tasks.await(task)
    }

    // 7. Activity Logs Flow
    val activityLogs: Flow<List<ActivityLog>> = callbackFlow {
        val listener = firestore.collection("activity_logs")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ActivityLog::class.java)?.copy(id = doc.id)
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun insertActivityLog(log: ActivityLog) = withContext(Dispatchers.IO) {
        val docId = firestore.collection("activity_logs").document().id
        val finalLog = log.copy(id = docId)
        val task = firestore.collection("activity_logs").document(docId).set(finalLog)
        Tasks.await(task)
    }

    // Seeding Firestore database if empty
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        try {
            // Seed Settings
            val settingsDoc = Tasks.await(firestore.collection("settings").document("global_settings").get())
            if (!settingsDoc.exists()) {
                val defSettings = AppSettings()
                Tasks.await(firestore.collection("settings").document("global_settings").set(defSettings))
            }

            // Seed Moderators
            val modsCountDoc = Tasks.await(firestore.collection("moderators").limit(1).get())
            if (modsCountDoc.isEmpty) {
                val initialMods = listOf(
                    Moderator("admin1", "pass123", "moderator", true, true, true, true),
                    Moderator("yemen_mod", "123456", "moderator", true, false, false, true)
                )
                for (mod in initialMods) {
                    Tasks.await(firestore.collection("moderators").document(mod.username).set(mod))
                }
            }

            // Seed Categories
            val catsCountDoc = Tasks.await(firestore.collection("categories").limit(1).get())
            if (catsCountDoc.isEmpty) {
                val initialCats = listOf(
                    Category("cat_elec", "صيانة كهربائية والتمديدات", "Electrical & Wiring", "⚡"),
                    Category("cat_plumb", "سباكة تركيب وأعطال مياه", "Plumbing & Repairs", "🔧"),
                    Category("cat_ac", "تكييف وتبريد وصيانة غسالات", "AC & Refrigeration", "❄️"),
                    Category("cat_comp", "برمجة حاسوب وصيانة هواتف", "IT & Phone Support", "💻"),
                    Category("cat_house", "بناء ومقاولات وديكور صبغ", "Construction & Design", "🏠"),
                    Category("cat_trans", "نقل كتل وأغراض وتوصيل", "Logistics & Transport", "📦"),
                    Category("cat_teach", "تدريس منزلي وتطوير لغات", "Home Tutoring", "🎓")
                )
                for (cat in initialCats) {
                    Tasks.await(firestore.collection("categories").document(cat.id).set(cat))
                }
            }

            // Seed Providers
            val prodsCountDoc = Tasks.await(firestore.collection("providers").limit(1).get())
            if (prodsCountDoc.isEmpty) {
                val initialProviders = listOf(
                    ServiceProvider("p1", "المهندس عادل ناصر الكهربائي", "773456789", "cat_elec", "صنعاء القديمة", "شارع القصر", "male", true, true, true, 20, 4, "active"),
                    ServiceProvider("p2", "الأستاذ مراد لخدمات السباكة", "733475920", "cat_plumb", "حدة", "جولة كحلان", "male", true, false, true, 19, 4, "active"),
                    ServiceProvider("p3", "صيانة الأندلس للتكييف والتبريد", "771122334", "cat_ac", "تعز - الحوبان", "جوار مستشفى اليمن الدولي", "male", false, true, false, 5, 1, "active"),
                    ServiceProvider("p4", "مؤسسة يمن تك لبرمجة الهواتف", "775566332", "cat_comp", "عدن - كريتر", "الشارع الطويل", "male", true, false, false, 0, 0, "active"),
                    ServiceProvider("p5", "أم علي لخدمات التدريس واللغة", "772299881", "cat_teach", "صنعاء - التحرير", "حارة النصر", "female", true, true, true, 48, 10, "active")
                )
                for (provider in initialProviders) {
                    Tasks.await(firestore.collection("providers").document(provider.id).set(provider))
                }
            }

            // Seed FAQs
            val faqsCountDoc = Tasks.await(firestore.collection("faqs").limit(1).get())
            if (faqsCountDoc.isEmpty) {
                val initialFAQs = listOf(
                    FAQItem("faq1", "كيف أثق بمقدم الخدمة؟", "How can I trust a service provider?", "جميع مقدمي الخدمات الموثقين يحملون علامة النجمة الذهبية (⭐) بعد التحقق من هوياتهم وسجلاتهم المهنية.", "All verified providers bear a golden star badge (⭐) indicating their identity and professional reviews have been authenticated."),
                    FAQItem("faq2", "هل يوفر التطبيق تواصل مباشر؟", "Does the app provide direct contact?", "نعم، يمكنك الضغط على زر اتصل الآن للاتصال وتنسيق العمل مع المهندس مباشرة وبلا وسطاء.", "Yes, simply tap 'Call Now' to contact the engineer and arrange everything directly without intermediaries."),
                    FAQItem("faq3", "ما هي تكلفة طلب الخدمات؟", "What is the service cost?", "التطبيق مجاني تماماً للدليل، ويتم الاتفاق على السعر مباشرة بينك وبين المهندس المسؤول.", "The directory tool is 100% free; the price is settled completely between you and the service expert.")
                )
                for (faq in initialFAQs) {
                    Tasks.await(firestore.collection("faqs").document(faq.id).set(faq))
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
