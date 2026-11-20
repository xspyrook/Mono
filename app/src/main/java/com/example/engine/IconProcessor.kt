package com.example.engine

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.stability.StabilityMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Monochrome Icon Engine:
 * Safely converts installed app drawables into high-contrast monochrome white
 * ImageBitmaps with transparent backgrounds where supported (Adaptive Icons),
 * bounded by an LruCache and Semaphore to prevent Main Thread stalls or OOM.
 * Never modifies original application files.
 */
class IconProcessor(
    private val context: Context
) {
    private val cache = LruCache<String, ImageBitmap>(MAX_CACHED_ICONS)
    private val decodeSemaphore = Semaphore(MAX_CONCURRENT_DECODES)

    fun getCached(componentKey: String): ImageBitmap? {
        val hit = cache.get(componentKey)
        if (hit != null) {
            StabilityMonitor.recordCacheHit(cache.size())
        }
        return hit
    }

    suspend fun getOrLoadMonochromeIcon(
        packageName: String,
        activityName: String
    ): ImageBitmap? {
        val key = "$packageName/$activityName"
        getCached(key)?.let { return it }

        return withContext(Dispatchers.Default) {
            // Double-check inside worker
            cache.get(key)?.let {
                StabilityMonitor.recordCacheHit(cache.size())
                return@withContext it
            }

            decodeSemaphore.withPermit {
                cache.get(key)?.let {
                    StabilityMonitor.recordCacheHit(cache.size())
                    return@withContext it
                }

                val loaded = runCatching {
                    val pm = context.packageManager
                    val drawable = loadRawDrawable(pm, packageName, activityName)
                    drawable?.let { renderMonochromeBitmap(it) }
                }.onFailure { err ->
                    StabilityMonitor.recordHandledError("IconProcessor:$packageName", err)
                }.getOrNull()

                if (loaded != null) {
                    cache.put(key, loaded)
                    StabilityMonitor.recordCacheMiss(cache.size())
                }
                loaded
            }
        }
    }

    fun clearCache() {
        cache.evictAll()
        StabilityMonitor.refreshSnapshot()
    }

    private fun loadRawDrawable(
        pm: PackageManager,
        packageName: String,
        activityName: String
    ): Drawable? {
        return try {
            val component = ComponentName(packageName, activityName)
            pm.getActivityIcon(component)
        } catch (_: Throwable) {
            try {
                pm.getApplicationIcon(packageName)
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun renderMonochromeBitmap(drawable: Drawable): ImageBitmap? {
        val size = TARGET_ICON_PX
        var rawBitmap: Bitmap? = null
        var monoBitmap: Bitmap? = null

        return try {
            rawBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val rawCanvas = Canvas(rawBitmap)

            val isAdaptiveForeground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
                val fg = drawable.foreground
                if (fg != null) {
                    val pad = (size * 0.18f).toInt()
                    fg.setBounds(-pad, -pad, size + pad, size + pad)
                    fg.draw(rawCanvas)
                    true
                } else {
                    drawable.setBounds(0, 0, size, size)
                    drawable.draw(rawCanvas)
                    false
                }
            } else {
                drawable.setBounds(0, 0, size, size)
                drawable.draw(rawCanvas)
                false
            }

            monoBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val monoCanvas = Canvas(monoBitmap)

            if (!isAdaptiveForeground) {
                // Clip legacy square icons into a clean squircle so all icons share unified geometry
                val clipPath = Path().apply {
                    val radius = size * 0.24f
                    addRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), radius, radius, Path.Direction.CW)
                }
                monoCanvas.clipPath(clipPath)
            }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(buildMonochromeWhiteMatrix())
            }
            monoCanvas.drawBitmap(rawBitmap, 0f, 0f, paint)
            monoBitmap.asImageBitmap()
        } catch (t: Throwable) {
            StabilityMonitor.recordHandledError("IconRender", t)
            null
        } finally {
            rawBitmap?.recycle()
        }
    }

    /**
     * Converts any RGB colors into pure monochrome white/silver while preserving
     * internal luminance contrast and keeping alpha transparency untouched.
     */
    private fun buildMonochromeWhiteMatrix(): ColorMatrix {
        val lumR = 0.299f * 0.52f
        val lumG = 0.587f * 0.52f
        val lumB = 0.114f * 0.52f
        val lift = 122f
        return ColorMatrix(
            floatArrayOf(
                lumR, lumG, lumB, 0f, lift,
                lumR, lumG, lumB, 0f, lift,
                lumR, lumG, lumB, 0f, lift,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    companion object {
        private const val TARGET_ICON_PX = 128
        private const val MAX_CACHED_ICONS = 140
        private const val MAX_CONCURRENT_DECODES = 3
    }
}
