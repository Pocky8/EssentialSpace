package com.essential.essspace.util

object OcrTextUtil {
    fun prepareOcrTextForLinkExtraction(text: String?): String {
        if (text.isNullOrBlank()) {
            return ""
        }
        var processedText = text.trim()

        // Regex to find http(s):// followed by non-space characters, then one or more spaces,
        // then more non-space characters that could be part of a URL.
        // This simplified pattern aims to be more robust for path concatenation.
        val urlPattern = Regex("(https?://[^\\s]+)(\\s+)([^\\s]+)")

        var previousText: String
        do {
            previousText = processedText
            processedText = urlPattern.replace(processedText) { matchResult ->
                // Reconstruct the URL by removing the space(s) between the matched groups
                matchResult.groupValues[1] + matchResult.groupValues[3]
            }
        } while (previousText != processedText) // Continue if changes were made

        // Second pass: Specifically look for spaces before a query parameter '?' or other URL parts.
        // This regex remains the same.
        val spaceBeforeQueryPattern = Regex("([^\\s/]+)(\\s+)(\\??[^\\s]+)")
        do {
            previousText = processedText
            processedText = spaceBeforeQueryPattern.replace(processedText) { matchResult ->
                matchResult.groupValues[1] + matchResult.groupValues[3]
            }
        } while (previousText != processedText)

        return processedText
    }
}
