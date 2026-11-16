package com.example.apkextractor.data

import androidx.compose.ui.graphics.ImageBitmap

/** One installed application shown in the app list. */
data class AppEntry(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sourceDir: String,
    val splitDirs: List<String>,
    val isSystem: Boolean,
    val icon: ImageBitmap?,
    val sizeBytes: Long
)

/** Full package metadata displayed on the App Info screen. */
data class AppDetails(
    val entry: AppEntry,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val permissions: List<String>,
    val features: List<String>
)
