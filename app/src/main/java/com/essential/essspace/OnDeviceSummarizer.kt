package com.essential.essspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OnDeviceSummarizer : TextSummarizer {
    override suspend fun summarize(text: String, apiKey: String?): Result<String> = withContext(Dispatchers.Default) {
        // Simple placeholder: return first 100 characters or full text if shorter
        val summary = if (text.length <= 100) text else text.take(100) + "..."
        Result.success(summary)
    }
}