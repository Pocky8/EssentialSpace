package com.essential.essspace.util

object SummarizationUtils {
    fun summarizeAndBulletText(text: String?): String {
        if (text.isNullOrBlank()) {
            return ""
        }
        // Simple summarization: split by sentences (periods or newlines), trim, filter blanks, and bullet each.
        val sentences = text.split(Regex("[.\n]+"))
                            .map { it.trim() }
                            .filter { it.isNotBlank() }

        if (sentences.isEmpty()) {
            // If no clear sentences based on delimiters (e.g., a single phrase), bullet the whole text.
            return "* ${text.trim()}"
        }
        return sentences.joinToString("\n") { "* $it" }
    }
}
