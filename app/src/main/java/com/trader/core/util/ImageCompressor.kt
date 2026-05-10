package com.trader.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

object ImageCompressor {

    private const val TARGET_MAX_BYTES  = 150 * 1024
    private const val MAX_DIMENSION     = 1280
    private const val INITIAL_QUALITY   = 88
    private const val QUALITY_STEP      = 8
    private const val MIN_QUALITY       = 10

    fun compress(context: Context, uri: Uri): ByteArray {
        val bounds  = decodeBounds(context, uri)
        val sample  = computeSampleSize(bounds.first, bounds.second)
        val bitmap  = decodeSampled(context, uri, sample)

        val scaled  = scaleBitmap(bitmap)
        if (scaled !== bitmap) bitmap.recycle()

        val out     = ByteArrayOutputStream()
        var quality = INITIAL_QUALITY

        do {
            out.reset()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            quality -= QUALITY_STEP
        } while (out.size() > TARGET_MAX_BYTES && quality >= MIN_QUALITY)

        scaled.recycle()
        return out.toByteArray()
    }

    private fun decodeBounds(context: Context, uri: Uri): Pair<Int, Int> {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
        return Pair(opts.outWidth, opts.outHeight)
    }

    private fun computeSampleSize(width: Int, height: Int): Int {
        var size = 1
        while ((width / size) > MAX_DIMENSION * 2 || (height / size) > MAX_DIMENSION * 2) {
            size *= 2
        }
        return size
    }

    private fun decodeSampled(context: Context, uri: Uri, sampleSize: Int): Bitmap {
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        } ?: throw IllegalStateException("Failed to decode bitmap from URI: $uri")
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= MAX_DIMENSION && h <= MAX_DIMENSION) return bitmap
        val ratio = minOf(MAX_DIMENSION.toFloat() / w, MAX_DIMENSION.toFloat() / h)
        return Bitmap.createScaledBitmap(
            bitmap,
            (w * ratio).toInt(),
            (h * ratio).toInt(),
            true
        )
    }
}
