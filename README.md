# APK Extractor (Jetpack Compose)

Modern Android utility written in Kotlin and Jetpack Compose (Material 3) for inspecting installed applications and extracting or sharing their APK files.

## Features

- **Installed Apps Browser**: Lists installed applications sorted alphabetically with icons, package names, versions, and APK sizes.
- **Search & System App Toggle**: Filter live by app label or package name, and toggle visibility of system apps.
- **Single & Batch APK Extraction**: Extract base and split APKs to `Download/ExtractedApks` (via `MediaStore.Downloads` on Android 10+ and legacy external storage on Android 7–9).
- **APK Sharing**: Share single or multiple APKs via `FileProvider` and the system share sheet.
- **Multi-Select Mode**: Long-press any app row to enter multi-select mode, select all visible apps, and batch extract or share.
- **Per-App Context Actions**: Open App Info, launch the app, or trigger system uninstall.
- **App Info Screen**: Inspect app label, package name, version name/code, min/target SDK levels, install/update timestamps, APK path, size, requested permissions, and required features.
- **Export App List**: Save the current filtered app list (`name | package | version`) to `Download/ExtractedApks/app_list.txt`.
- **Auto-Refresh**: Automatically reloads the app list when packages are installed, updated, or removed.

## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- Navigation Compose & Lifecycle ViewModel (`StateFlow`)
- Gradle Kotlin DSL (`AGP 9.1.1`, `compileSdk 36`, `minSdk 24`)
