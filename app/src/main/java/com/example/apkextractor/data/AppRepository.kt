package com.example.apkextractor.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Repository for querying installed packages and inspecting package details. */
object AppRepository {

    suspend fun loadInstalledApps(context: Context): List<AppEntry> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        PmCompat.installedPackages(pm)
            .mapNotNull { pi ->
                val ai = pi.applicationInfo ?: return@mapNotNull null
                val sourceDir = ai.sourceDir ?: return@mapNotNull null
                AppEntry(
                    label = ai.loadLabel(pm).toString(),
                    packageName = pi.packageName,
                    versionName = pi.versionName ?: "",
                    versionCode = PackageInfoCompat.getLongVersionCode(pi),
                    sourceDir = sourceDir,
                    splitDirs = ai.splitSourceDirs?.toList() ?: emptyList(),
                    isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    icon = loadIconBitmap(ai.loadIcon(pm)),
                    sizeBytes = File(sourceDir).length()
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun loadAppDetails(context: Context, packageName: String): AppDetails? =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_CONFIGURATIONS
            val info = PmCompat.packageInfo(pm, packageName, flags) ?: return@withContext null
            val ai = info.applicationInfo ?: return@withContext null
            val sourceDir = ai.sourceDir ?: return@withContext null
            val entry = AppEntry(
                label = ai.loadLabel(pm).toString(),
                packageName = info.packageName,
                versionName = info.versionName ?: "",
                versionCode = PackageInfoCompat.getLongVersionCode(info),
                sourceDir = sourceDir,
                splitDirs = ai.splitSourceDirs?.toList() ?: emptyList(),
                isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                icon = loadIconBitmap(ai.loadIcon(pm), targetSizePx = 168),
                sizeBytes = File(sourceDir).length()
            )
            AppDetails(
                entry = entry,
                minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) ai.minSdkVersion else 0,
                targetSdkVersion = ai.targetSdkVersion,
                firstInstallTime = info.firstInstallTime,
                lastUpdateTime = info.lastUpdateTime,
                permissions = info.requestedPermissions?.toList().orEmpty(),
                features = info.reqFeatures?.mapNotNull { it.name }.orEmpty()
            )
        }

    private fun loadIconBitmap(drawable: Drawable?, targetSizePx: Int = 128): ImageBitmap? {
        if (drawable == null) return null
        return try {
            val width = drawable.intrinsicWidth.takeIf { it > 0 }?.coerceAtMost(targetSizePx) ?: targetSizePx
            val height = drawable.intrinsicHeight.takeIf { it > 0 }?.coerceAtMost(targetSizePx) ?: targetSizePx
            drawable.toBitmap(width = width, height = height).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
