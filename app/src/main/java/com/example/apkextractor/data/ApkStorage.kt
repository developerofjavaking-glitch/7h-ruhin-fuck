package com.example.apkextractor.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/** Copies APK files out of the system and into Download/ExtractedApks (or the share cache). */
object ApkStorage {
    const val FOLDER = "ExtractedApks"
    const val MIME_APK = "application/vnd.android.package-archive"
    const val APP_LIST_FILE = "app_list.txt"

    private fun safeName(text: String): String =
        text.replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_')

    fun baseName(app: AppEntry): String {
        val name = safeName(app.label).ifEmpty { app.packageName }
        val version = safeName(app.versionName)
        return if (version.isEmpty()) name else "${name}_$version"
    }

    /**
     * Writes a file to Download/ExtractedApks.
     * Android 10+ goes through MediaStore; older versions write directly
     * (requires WRITE_EXTERNAL_STORAGE).
     */
    fun saveToDownloads(
        context: Context,
        fileName: String,
        mime: String,
        write: (OutputStream) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Could not create $fileName")
            try {
                val out = resolver.openOutputStream(uri)
                    ?: throw IOException("Could not open $fileName")
                out.use(write)
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                FOLDER
            )
            if (!dir.exists() && !dir.mkdirs()) throw IOException("Could not create ${dir.path}")
            FileOutputStream(File(dir, fileName)).use(write)
        }
    }

    /** Extracts the base APK (and any split APKs) of [app]. */
    fun extract(context: Context, app: AppEntry) {
        val base = baseName(app)
        saveToDownloads(context, "$base.apk", MIME_APK) { out ->
            File(app.sourceDir).inputStream().use { it.copyTo(out) }
        }
        app.splitDirs.forEach { split ->
            val splitFile = File(split)
            saveToDownloads(context, "${base}_${splitFile.name}", MIME_APK) { out ->
                splitFile.inputStream().use { it.copyTo(out) }
            }
        }
    }

    /** Exports "name  |  package  |  version" for every listed app to a text file. */
    fun exportAppList(context: Context, apps: List<AppEntry>) {
        val text = apps.joinToString("\n") {
            "${it.label}  |  ${it.packageName}  |  ${it.versionName}"
        }
        saveToDownloads(context, APP_LIST_FILE, "text/plain") { out ->
            out.write(text.toByteArray())
        }
    }

    /** Copies base APKs into the cache and returns content URIs that other apps can read. */
    fun prepareShare(context: Context, apps: List<AppEntry>): ArrayList<Uri> {
        val dir = File(context.cacheDir, "shared")
        dir.deleteRecursively()
        dir.mkdirs()
        val uris = ArrayList<Uri>()
        for (app in apps) {
            val target = File(dir, "${baseName(app)}.apk")
            File(app.sourceDir).inputStream().use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
            uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target))
        }
        return uris
    }
}
