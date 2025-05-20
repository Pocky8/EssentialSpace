package com.essential.essspace

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class CloudSummarizer : TextSummarizer {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Using Hugging Face Inference API as an example
    private val summarizationModel = "facebook/bart-large-cnn"
    private val apiUrl = "https://api-inference.huggingface.co/models/$summarizationModel"

    override suspend fun summarize(text: String, apiKey: String?): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API key is required for cloud summarizer."))
        }
        if (text.length < 100) { // Avoid sending very short texts for summarization
            return@withContext Result.failure(IllegalArgumentException("Text is too short to summarize effectively."))
        }

        val requestBodyJson = JSONObject().apply {
            put("inputs", text)
            // Optional: Add parameters like min_length, max_length if supported by the model/API
            // val parameters = JSONObject().apply {
            //     put("min_length", 30)
            //     put("max_length", 150)
            // }
            // put("parameters", parameters)
        }.toString()

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            Log.d("CloudSummarizer", "Requesting summary for text: ${text.take(50)}...")
            val response = client.newCall(request).execute()
            val responseBodyString = response.body?.string()

            if (!response.isSuccessful) {
                Log.e("CloudSummarizer", "API Error: ${response.code} - ${response.message}. Body: $responseBodyString")
                return@withContext Result.failure(IOException("API Error: ${response.code} ${response.message}"))
            }

            if (responseBodyString.isNullOrBlank()) {
                Log.e("CloudSummarizer", "API Error: Empty response body.")
                return@withContext Result.failure(IOException("Empty response body from API"))
            }

            Log.d("CloudSummarizer", "API Response: $responseBodyString")

            // Hugging Face Inference API typically returns a JSON array with a single object
            // containing "summary_text".
            val jsonResponseArray = JSONArray(responseBodyString)
            if (jsonResponseArray.length() > 0) {
                val summaryObject = jsonResponseArray.getJSONObject(0)
                if (summaryObject.has("summary_text")) {
                    val summary = summaryObject.getString("summary_text")
                    Log.i("CloudSummarizer", "Summary extracted: ${summary.take(100)}...")
                    Result.success(summary)
                } else if (summaryObject.has("error")) {
                    val error = summaryObject.getString("error")
                    Log.e("CloudSummarizer", "API returned an error: $error")
                    Result.failure(IOException("API returned an error: $error"))
                }
                else {
                    Log.e("CloudSummarizer", "Unexpected JSON structure: 'summary_text' not found.")
                    Result.failure(IOException("Unexpected JSON structure from API."))
                }
            } else {
                Log.e("CloudSummarizer", "Unexpected JSON structure: Empty array.")
                Result.failure(IOException("Empty array in API response."))
            }
        } catch (e: IOException) {
            Log.e("CloudSummarizer", "Network IOException: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) { // Catch other exceptions like JSONException
            Log.e("CloudSummarizer", "General Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}