package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "عذراً، لم يتم ضبط مفتاح Gemini API في لوحة التحكم بشكل صحيح. الرجاء التحقق من الرموز السرية."
        }

        try {
            // Build request json structure purely using android SDK libraries
            val requestJson = JSONObject()
            
            val contentArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentArray.put(contentObj)
            requestJson.put("contents", contentArray)

            // System instructions
            val systemInstruction = JSONObject()
            val systemParts = JSONArray()
            val systemPart = JSONObject()
            systemPart.put("text", "أنت مساعد ذكي للمنصة اليمنية للخدمات WAM. أجب باللغة العربية باختصار وبطريقة ودودة ومحترفة.")
            systemParts.put(systemPart)
            systemInstruction.put("parts", systemParts)
            requestJson.put("systemInstruction", systemInstruction)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "عذراً، حدث خطأ في الاتصال بخوادم المساعد الذكي الكلية: ${response.code}"
                }
                val rawBody = response.body?.string() ?: return@withContext "استجابة فارغة تماماً من الخادم."
                val responseJson = JSONObject(rawBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "عذراً، لم يورد المساعد رداً دلالياً.")
                    }
                }
                "عذراً، تعذر صياغة رد مناسب من الذكاء الاصطناعي."
            }
        } catch (e: Exception) {
            "عذراً، واجهنا خطأ أثناء معالجة رد الذكاء الاصطناعي: ${e.localizedMessage}"
        }
    }
}
