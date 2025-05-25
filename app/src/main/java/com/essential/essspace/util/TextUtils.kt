package com.essential.essspace.util

fun cleanOcrText(text: String?): String {
    if (text.isNullOrBlank()) return ""

    // Step 1: Trim leading/trailing whitespace from the whole text block
    var processedText = text.trim()

    // Step 2: Normalize 3 or more newlines to exactly 2 (for paragraph separation)
    processedText = processedText.replace(Regex("\n{3,}"), "\n\n")

    // Step 3: Convert single newlines (that are not part of a double newline) to a single space.
    // This helps join lines that were broken mid-sentence by OCR.
    // To do this safely without affecting intended paragraph breaks (\n\n):
    // 1. Temporarily replace "\n\n" with a unique placeholder.
    // 2. Replace all remaining single "\n" with a space.
    // 3. Restore the placeholder to "\n\n".
    val paragraphSeparatorPlaceholder = "[[PARAGRAPH_SEPARATOR_PLACEHOLDER_TEXT_UTIL]]"
    processedText = processedText.replace("\n\n", paragraphSeparatorPlaceholder)
    processedText = processedText.replace("\n", " ") // Convert all remaining single newlines to spaces
    processedText = processedText.replace(paragraphSeparatorPlaceholder, "\n\n") // Restore paragraph breaks

    // Step 4: Clean up multiple spaces that might have been introduced by the previous steps
    processedText = processedText.replace(Regex("\\s{2,}"), " ")

    // Step 5: Trim whitespace from each line individually (if there are still multiple lines)
    // This can be useful if OCR added spaces at the start/end of lines within paragraphs.
    processedText = processedText.lines().joinToString("\n") { it.trim() }

    return processedText.trim() // Final trim of the whole block
}
