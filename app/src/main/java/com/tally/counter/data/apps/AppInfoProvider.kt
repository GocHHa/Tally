package com.tally.counter.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.util.LruCache
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.tally.counter.data.settings.TallySettings
import java.util.concurrent.ConcurrentHashMap

data class AppMeta(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val hasLauncher: Boolean,
    val installed: Boolean
) {
    /** System components without a launcher icon (Android System, Play services…) are off by default. */
    val defaultExcluded: Boolean get() = !installed || (isSystem && !hasLauncher)
}

/**
 * Loads app labels, icons (as bitmaps, for Compose and Glance) and accent colours (Palette).
 * Everything is cached; call from a background thread.
 */
class AppInfoProvider(context: Context) {

    private val appContext = context.applicationContext
    private val pm: PackageManager = appContext.packageManager

    private val metaCache = ConcurrentHashMap<String, AppMeta>()
    private val colorCache = ConcurrentHashMap<String, Int>()
    private val iconCache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun meta(packageName: String): AppMeta = metaCache.getOrPut(packageName) { loadMeta(packageName) }

    fun isExcluded(packageName: String, settings: TallySettings): Boolean = when {
        packageName in ALWAYS_EXCLUDED -> true
        packageName in settings.excludedPackages -> true
        packageName in settings.includedPackages -> false
        else -> meta(packageName).defaultExcluded
    }

    fun launchIntent(packageName: String): Intent? = pm.getLaunchIntentForPackage(packageName)

    /** Every app with a launcher icon (visible thanks to the <queries> manifest entry). */
    fun launcherApps(): List<AppMeta> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return resolved
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != appContext.packageName }
            .map { meta(it) }
    }

    /** Square ARGB bitmap of the app icon, or null if the app isn't visible/installed. */
    fun iconBitmap(packageName: String, sizePx: Int): Bitmap? {
        val cacheKey = "$packageName@$sizePx"
        iconCache.get(cacheKey)?.let { return it }
        val drawable = try {
            pm.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val oldBounds = Rect(drawable.bounds)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        drawable.bounds = oldBounds
        iconCache.put(cacheKey, bitmap)
        return bitmap
    }

    /** Dominant colour of the app icon, nudged so it reads as a glow on dark and light cards. */
    fun accentColor(packageName: String): Int = colorCache.getOrPut(packageName) {
        val bitmap = iconBitmap(packageName, 96) ?: return@getOrPut DEFAULT_ACCENT
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()
        val dominant = palette.getDominantColor(DEFAULT_ACCENT)
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(dominant, hsl)
        // Near-white, near-black or grey dominant colours make a poor glow: prefer a vibrant swatch.
        val washedOut = hsl[1] < 0.2f || hsl[2] > 0.9f || hsl[2] < 0.12f
        val chosen = if (washedOut) {
            palette.getVibrantColor(palette.getLightVibrantColor(palette.getMutedColor(dominant)))
        } else {
            dominant
        }
        ColorUtils.colorToHSL(chosen, hsl)
        if (hsl[1] < 0.12f) {
            DEFAULT_ACCENT
        } else {
            hsl[2] = hsl[2].coerceIn(0.38f, 0.65f)
            ColorUtils.HSLToColor(hsl)
        }
    }

    private fun loadMeta(packageName: String): AppMeta {
        val info: ApplicationInfo? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        val label = info?.let { pm.getApplicationLabel(it).toString() } ?: prettifyPackage(packageName)
        val isSystem = info != null && (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val hasLauncher = pm.getLaunchIntentForPackage(packageName) != null
        return AppMeta(
            packageName = packageName,
            label = label,
            isSystem = isSystem,
            hasLauncher = hasLauncher,
            installed = info != null
        )
    }

    private fun prettifyPackage(packageName: String): String =
        packageName.substringAfterLast('.').replaceFirstChar { it.uppercaseChar() }

    companion object {
        val DEFAULT_ACCENT: Int = Color.rgb(255, 69, 58)

        /** Never counted, regardless of settings. */
        val ALWAYS_EXCLUDED = setOf(
            "android",
            "com.android.systemui",
            "com.android.providers.downloads",
            "com.samsung.android.app.aodservice"
        )
    }
}
