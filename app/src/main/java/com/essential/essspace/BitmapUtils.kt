package com.essential.essspace

import android.graphics.Bitmap
import androidx.core.graphics.scale

object BitmapUtils {
    fun getResizedBitmapForOcr(sourceBitmap: Bitmap, maxSize: Int = 2048): Bitmap {
        var width = sourceBitmap.width
        var height = sourceBitmap.height

        if (width <= 0 || height <= 0) {
            // Avoid division by zero or invalid state if bitmap is somehow malformed
            return sourceBitmap
        }

        if (width <= maxSize && height <= maxSize) {
            return sourceBitmap // No resize needed, return original
        }

        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) { // Landscape or square
            width = maxSize
            height = (width / bitmapRatio).toInt().coerceAtLeast(1) // Ensure height is at least 1
        } else { // Portrait
            height = maxSize
            width = (height * bitmapRatio).toInt().coerceAtLeast(1) // Ensure width is at least 1
        }

        val resizedBitmap = sourceBitmap.scale(width, height)

        // If createScaledBitmap returned a new instance, recycle the original
        if (resizedBitmap != sourceBitmap) {
            sourceBitmap.recycle()
        }
        return resizedBitmap
    }
}