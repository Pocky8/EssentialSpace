package com.essential.essspace

interface TextSummarizer {
    suspend fun summarize(text: String, apiKey: String? = null): Result<String>
}