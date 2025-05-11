package com.essential.essspace

object TextCleanupUtils {

    fun cleanOcrText(rawText: String?): String? {
        if (rawText.isNullOrBlank()) {
            return null
        }

        val lines = rawText.split('\n')
        val cleanedLines = mutableListOf<String>()

        val patternsToIgnore = listOf(
            Regex("^Replacing \\d+ out of \\d+ node\\(s\\) with delegate.*", RegexOption.IGNORE_CASE), // ML Kit delegate logs
            Regex("^< Search for '[^']+'$", RegexOption.IGNORE_CASE), // Search bar prompts
            Regex("^YOUR PAST SEARCHES$", RegexOption.IGNORE_CASE),
            Regex("^Q [A-Za-z0-9 ]+$"), // Lines starting with "Q " (likely past searches)
            Regex("^\\d{1,2}:\\d{2} [APMapm.]{0,2} .*"), // Timestamps like 3:22 O 40D • (adjust if needed)
            Regex("^[\\W_]+$") // Lines with only symbols or underscores
        )

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) {
                continue // Skip empty lines
            }

            var ignoreLine = false
            for (pattern in patternsToIgnore) {
                if (pattern.matches(trimmedLine)) {
                    ignoreLine = true
                    break
                }
            }

            if (ignoreLine) {
                continue
            }

            // Optional: Remove lines that are too short (e.g., less than 3 characters, adjust as needed)
            // if (trimmedLine.length < 3) {
            //     continue
            // }

            cleanedLines.add(trimmedLine)
        }

        val result = cleanedLines.joinToString("\n").trim()
        return if (result.isBlank()) null else result
    }
}