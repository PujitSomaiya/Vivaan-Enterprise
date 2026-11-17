# Technology Stack
**Analysis Date:** 2026-11-17

## Languages

**Primary:**
- Kotlin 2.3.10 - Core application language used across the entire Android app codebase, including application entry points (`app/src/main/java/com/vivaanenterprise/app/VivaanEnterpriseApplication.kt`, `app/src/main/java/com/vivaanenterprise/app/MainActivity.kt`), UI screens and ViewModels, Room database entities, and Gradle Kotlin DSL build scripts (`build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`).

**Secondary:**
- Java 17 - JVM bytecode compilation target (`compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }` in `app/build.gradle.kts`).
- XML - Android system manifests (`app/src/main/AndroidManifest.xml`), FileProvider configuration (`app/src/main/res/xml/file_paths.xml`), theme and splash definitions (`app/src/main/res/values/themes.xml`), and vector asset drawables (`app/src/main/res/drawable/`).
- JSON - Exported Room database schema specifications (`app/schemas/com.vivaanenterprise.app.core.database.VivaanEnterpriseDatabase/1.json`, `2.json`, `3.json`).
- TOML - Centralized Gradle version catalog (`gradle/libs.versions.toml`).

## Runtime

**Environment:**
- Android Runtime (ART) - targetSdk 37, compileSdk 37, minSdk 26 (`app/build.gradle.kts`), supporting Android 8.0 (Oreo) through Android 16.
- Java Runtime Environment (JRE) - JVM 17 bytecode runtime compatible with Android Gradle Plugin 9.4.0. Daemon JVM toolchain version 25 configured in `gradle/gradle-daemon-jvm.properties`.

**Package Manager:**
- Gradle Wrapper 9.6.0 (`gradle/wrapper/gradle-wrapper.properties` via `distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.0-bin.zip`).
- Android Gradle Plugin (AGP) 9.4.0 (`com.android.application` in `gradle/libs.versions.toml` and `app/build.gradle.kts`).
- Repositories: Google Maven (`google()`), Maven Central (`mavenCentral()`), and Gradle Plugin Portal (`gradlePluginPortal()`) configured with strict `RepositoriesMode.FAIL_ON_PROJECT_REPOS` in `settings.gradle.kts`.
- Lockfile: Not present; dependency versions are strictly managed and pinned via `gradle/libs.versions.toml`.

## Frameworks

**Core:**
- Jetpack Compose (Compose BOM 2026.08.00) - Declarative UI toolkit (`app/build.gradle.kts`).
- Material 3 (`androidx.compose.material3:material3`) - Design system components, shapes, typography, and color tokens (`app/src/main/java/com/vivaanenterprise/app/core/designsystem/theme/Theme.kt`).
- AndroidX Navigation Compose 2.10.0 - In-app navigation graph and type-safe routing (`app/src/main/java/com/vivaanenterprise/app/app/VivaanEnterpriseNavHost.kt`).
- AndroidX Room 2.8.4 - Local persistence and SQLite object-relational mapping (`app/src/main/java/com/vivaanenterprise/app/core/database/VivaanEnterpriseDatabase.kt`).
- AndroidX DataStore Preferences 1.2.1 - Lightweight asynchronous key-value persistence (`app/src/main/java/com/vivaanenterprise/app/core/datastore/AppPreferencesDataStore.kt`).
- AndroidX WorkManager 2.11.2 - Guaranteed background task scheduling and synchronization (`app/src/main/java/com/vivaanenterprise/app/core/sync/FirebaseSyncWorker.kt`).
- Dagger Hilt 2.59.2 - Dependency injection framework for Android components, ViewModels, and Workers (`app/src/main/java/com/vivaanenterprise/app/VivaanEnterpriseApplication.kt`).
- Firebase Android SDK (Firebase BOM 34.18.0) - Remote cloud backend for authentication and Firestore sync (`app/src/main/java/com/vivaanenterprise/app/core/firebase/di/FirebaseModule.kt`).
- Kotlinx Coroutines 1.10.1 - Structured concurrency, background worker dispatchers, and reactive `Flow` streams (`app/src/main/java/com/vivaanenterprise/app/data/repository/`).

**Testing:**
- JUnit 4.13.2 - Unit test runner for local JVM tests (`app/src/test/java/com/vivaanenterprise/app/`).
- AndroidX Test JUnit Ext 1.3.0 - Android test runner (`androidx.test.runner.AndroidJUnitRunner`) for instrumentation testing (`app/src/androidTest/java/com/vivaanenterprise/app/`).
- AndroidX Espresso Core 3.7.0 - UI interaction and verification framework for on-device testing.
- AndroidX Room Testing 2.8.4 - Database schema migration and transaction tests (`app/src/androidTest/java/com/vivaanenterprise/app/core/database/migration/RoomDatabaseMigrationTest.kt`).
- Kotlinx Coroutines Test 1.10.1 - Test coroutine dispatchers (`StandardTestDispatcher`, `UnconfinedTestDispatcher`) and `runTest` scope for testing asynchronous flows.

**Build/Dev:**
- Google KSP (Kotlin Symbol Processing) 2.3.10 - Annotation processor for Room DAOs/entities and Hilt dependency graphs (`app/build.gradle.kts`).
- Kotlin Compose Compiler Plugin 2.3.10 (`org.jetbrains.kotlin.plugin.compose`) - Compose AST transformation and code generation (`build.gradle.kts`).
- Google Services Gradle Plugin 4.5.0 (`com.google.gms.google-services`) - Firebase JSON parsing and build integration (`build.gradle.kts`).
- R8 Optimizer & Code Shrinker - Minification and dead code/resource stripping enabled for both debug and release configurations (`isMinifyEnabled = true`, `isShrinkResources = true` in `app/build.gradle.kts`) guided by `app/proguard-rules.pro`.

## Key Dependencies

**Critical:**
- `androidx.room:room-runtime` & `androidx.room:room-ktx` 2.8.4 - Local persistence engine representing the application's single source of truth (`app/src/main/java/com/vivaanenterprise/app/core/database/`).
- `com.google.dagger:hilt-android` 2.59.2, `androidx.hilt:hilt-navigation-compose` 1.3.0, and `androidx.hilt:hilt-work` 1.3.0 - Component lifecycle and dependency injection (`app/src/main/java/com/vivaanenterprise/app/data/di/RepositoryModule.kt`).
- `com.google.firebase:firebase-firestore` (BOM 34.18.0) - Cloud document store for bidirectional synchronization (`app/src/main/java/com/vivaanenterprise/app/data/remote/datasource/FirestoreSyncDataSource.kt`).
- `com.google.firebase:firebase-auth` (BOM 34.18.0) - User identity provider for sync authentication (`app/src/main/java/com/vivaanenterprise/app/data/repository/AuthRepositoryImpl.kt`).
- `androidx.work:work-runtime-ktx` 2.11.2 - Offline-to-online sync queue orchestration (`app/src/main/java/com/vivaanenterprise/app/core/sync/WorkManagerSyncScheduler.kt`).
- `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` 1.10.1 - Integration bridge between Kotlin coroutines (`await()`) and Google Play Services Task API (`app/build.gradle.kts`).
- Android Platform PDF APIs (`android.graphics.pdf.PdfDocument`, `android.graphics.pdf.PdfRenderer`) - Standard framework classes for zero-dependency PDF rendering and on-device viewing (`app/src/main/java/com/vivaanenterprise/app/core/pdf/AndroidBusinessDocumentPdfGenerator.kt`, `app/src/main/java/com/vivaanenterprise/app/feature/pdfviewer/service/PdfRenderSession.kt`).

**Infrastructure:**
- `androidx.core:core-ktx` 1.17.0 - Android framework Kotlin idiomatic extensions.
- `androidx.core:core-splashscreen` 1.0.1 - Android 12+ backward-compatible splash screen support.
- `androidx.lifecycle:lifecycle-runtime-compose` 2.11.0 - Lifecycle-aware `collectAsStateWithLifecycle()` UI state binding (`app/src/main/java/com/vivaanenterprise/app/feature/invoice/presentation/InvoiceScreen.kt`).
- `androidx.compose.material:material-icons-core` - Standard material icon vector definitions.
- `androidx.datastore:datastore-preferences` 1.2.1 - Persistent settings storage for non-business state (`app/src/main/java/com/vivaanenterprise/app/core/datastore/`).

## Configuration

**Environment:**
- Local SDK path defined via `local.properties` (`sdk.dir`).
- Gradle build JVM arguments configured in `gradle.properties` (`-Xmx2048m -Dfile.encoding=UTF-8`, `android.useAndroidX=true`, `android.nonTransitiveRClass=true`).
- Toolchain configuration in `gradle/gradle-daemon-jvm.properties` (JDK 25 toolchain targets).
- Room schema export directory configured via KSP in `app/build.gradle.kts` (`room.schemaLocation = "$projectDir/schemas"`).
- Firebase client configuration parsed from `google-services.json` (gitignored, loaded by `com.google.gms.google-services`).

**Build:**
- Root build script: `build.gradle.kts`
- Application build configuration: `app/build.gradle.kts`
- Project settings and repository rules: `settings.gradle.kts`
- Dependency catalog: `gradle/libs.versions.toml`
- R8/ProGuard rules: `app/proguard-rules.pro`

## Platform Requirements

**Development:**
- JDK 17 (or newer with toolchain support).
- Android SDK with API 37 platform SDK (`compileSdk = 37`).
- Android Build Tools 35+ / 37 preview.
- Memory: Minimum 2GB heap allocated for Gradle daemon (`-Xmx2048m`).

**Production:**
- Minimum Android OS: Android 8.0 Oreo (API level 26).
- Target Android OS: Android 16 (API level 37).
- Hardware ABIs: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` (Standard Android ART support).
- Minification: Production release APKs and AABs are minified and obfuscated with R8.

---

*Stack analysis: 2026-11-17*
