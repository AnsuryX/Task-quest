package com.example.data.gemini

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = 0.7f,
    val topP: Float? = 0.95f
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: RequestBody
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun generateAiContent(prompt: String, systemPrompt: String? = null): String {
        var apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.startsWith("MY_")) {
            // Fallback: This will fail if the API key is not configured in .env
            // throw IllegalStateException("Gemini API Key is not configured.")
        }

        // Build the request body safely using Map to avoid Moshi printing null values (which Gemini rejects with 400)
        val requestMap = mutableMapOf<String, Any>()
        requestMap["contents"] = listOf(
            mapOf("parts" to listOf(mapOf("text" to prompt)))
        )
        if (!systemPrompt.isNullOrBlank()) {
            requestMap["systemInstruction"] = mapOf(
                "parts" to listOf(mapOf("text" to systemPrompt))
            )
        }

        val jsonAdapter = moshi.adapter(Map::class.java)
        val jsonString = jsonAdapter.toJson(requestMap)
        val mediaType = "application/json".toMediaType()
        val requestBody = jsonString.toRequestBody(mediaType)

        // Try gemini-2.5-flash as default, then fall back to gemini-1.5-flash
        val modelsToTry = listOf("gemini-2.5-flash", "gemini-1.5-flash")
        var lastException: Exception? = null

        for (model in modelsToTry) {
            try {
                val response = service.generateContent(model, apiKey, requestBody)
                val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (textResponse != null) {
                    return textResponse
                }
            } catch (e: retrofit2.HttpException) {
                lastException = e
            } catch (e: Exception) {
                lastException = e
            }
        }

        return if (lastException is retrofit2.HttpException) {
            val errBody = lastException.response()?.errorBody()?.string()
            "Connection Error (HTTP ${lastException.code()}): ${errBody ?: lastException.message()}"
        } else {
            "Connection Error: ${lastException?.localizedMessage ?: "Could not connect to AI services. Check network connection."}"
        }
    }
}
