package com.essential.essspace


import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.essential.essspace.OnDeviceSummarizer

// Assume you have a way to get user preference (e.g., from SharedPreferences)
enum class SummarizerPreference {
    AUTO, ON_DEVICE, CLOUD
}

class SummarizationService(
    private val context: Context, // For checking network state
    private val onDeviceSummarizer: OnDeviceSummarizer, // Assuming you have this
    private val cloudSummarizer: CloudSummarizer
) {
    private fun isOnline(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    suspend fun getSummary(
        text: String,
        preference: SummarizerPreference = SummarizerPreference.AUTO, // Get from settings later
        apiKey: String? // Get from settings later
    ): Result<String> {
        return when (preference) {
            SummarizerPreference.ON_DEVICE -> onDeviceSummarizer.summarize(text, apiKey) // apiKey not used by onDevice
            SummarizerPreference.CLOUD -> {
                if (isOnline()) {
                    if (!apiKey.isNullOrBlank()) {
                        cloudSummarizer.summarize(text, apiKey)
                    } else {
                        Result.failure(Exception("API key for cloud summarizer is missing."))
                    }
                } else {
                    Result.failure(Exception("No internet connection for cloud summarizer."))
                }
            }
            SummarizerPreference.AUTO -> {
                if (isOnline() && !apiKey.isNullOrBlank()) {
                    cloudSummarizer.summarize(text, apiKey).recoverCatching {
                        // Fallback to on-device if cloud fails
                        Log.w("SummarizationService", "Cloud summarization failed, falling back to on-device. Error: ${it.message}")
                        onDeviceSummarizer.summarize(text, null).getOrThrow()
                    }
                } else {
                    // If not online or no API key, use on-device directly
                    Log.d("SummarizationService", "Using on-device summarizer (offline or no API key).")
                    onDeviceSummarizer.summarize(text, null)
                }
            }
        }
    }
}