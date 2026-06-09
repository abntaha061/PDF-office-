package com.example.data.network

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini Request / Response Models ---

data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiThinkingConfig(
    val thinkingLevel: String
)

data class GeminiImageConfig(
    val aspectRatio: String,
    val imageSize: String
)

data class GeminiResponseFormat(
    val text: GeminiResponseFormatText? = null
)

data class GeminiResponseFormatText(
    val mimeType: String
)

data class GeminiGenerationConfig(
    val responseFormat: GeminiResponseFormat? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val thinkingConfig: GeminiThinkingConfig? = null,
    val imageConfig: GeminiImageConfig? = null,
    val responseModalities: List<String>? = null
)

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

data class GeminiPartResponse(
    val text: String? = null,
    val inlineData: GeminiInlineDataResponse? = null
)

data class GeminiInlineDataResponse(
    val mimeType: String,
    val data: String
)

data class GeminiContentResponse(
    val parts: List<GeminiPartResponse>
)

data class GeminiCandidate(
    val content: GeminiContentResponse? = null
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

// --- Retrofit API Service Interface ---

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- Singleton Retrofit Client ---

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    // --- Helper Functions to call Gemini ---

    suspend fun generateWithModel(prompt: String, model: String = "gemini-3.5-flash", systemPrompt: String? = null): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "برجاء ضبط مفتاح Gemini API Key في لوحة الإعدادات لتفعيل ميزات الذكاء الاصطناعي."
        }

        val contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
        val systemInstruction = systemPrompt?.let {
            GeminiContent(parts = listOf(GeminiPart(text = it)))
        }

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = systemInstruction
        )

        return try {
            val response = service.generateContent(model, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "لم يستطع محرك الذكاء الاصطناعي إنتاج استجابة واضحة حالياً."
        } catch (e: Exception) {
            "حدث خطأ أثناء الاتصال بخوادم الذكاء الاصطناعي: ${e.localizedMessage ?: e.message}"
        }
    }

    suspend fun generateImage(prompt: String): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return null
        }

        val contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
        val request = GeminiRequest(
            contents = contents,
            generationConfig = GeminiGenerationConfig(
                imageConfig = GeminiImageConfig(aspectRatio = "1:1", imageSize = "1K"),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )

        return try {
            val response = service.generateContent("gemini-2.5-flash-image", apiKey, request)
            val inlineData = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }?.inlineData
                ?: response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.inlineData
            inlineData?.data
        } catch (e: Exception) {
            null
        }
    }
}
