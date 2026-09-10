# Vivaan Enterprise

[![Android AGP](https://img.shields.io/badge/AGP-9.4.0-brightgreen.svg)](file:///Users/macm90/AndroidStudioProjects/VivaanEnterprise/gradle/libs.versions.toml)
[![Gradle](https://img.shields.io/badge/Gradle-9.6.0-blue.svg)](file:///Users/macm90/AndroidStudioProjects/VivaanEnterprise/gradle/wrapper/gradle-wrapper.properties)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-37-orange.svg)](file:///Users/macm90/AndroidStudioProjects/VivaanEnterprise/app/build.gradle.kts)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-purple.svg)](file:///Users/macm90/AndroidStudioProjects/VivaanEnterprise/gradle/libs.versions.toml)

Vivaan Enterprise is a specialized, internal Android business application built for small-team billing, document generation, and client account tracking.

## Overview
The application enables offline-first creation, management, and dynamic PDF output of key business documents:
- **Tax Invoices** (`TAX_INVOICE`)
- **Purchase Orders** (`PURCHASE_ORDER`)
- **Client & Product Masters**
- **Client Billing Accounts & Payment Ledgers**

It is designed for rapid, reliable local operation with background remote synchronization to Cloud Firestore.

---

## Project Status
> **Current Phase**: Final Production Release Verified (Step 18)
> 
> Vivaan Enterprise is fully implemented, verified, and audited for production release. The application features complete Client Master, Product Master, Document Domain foundation, Tax Calculation Engine, Tax Invoice & Purchase Order Creation UI, Native A4 PDF Generation Engine, Native PDF Viewer with Share & MediaStore Export, Document History, Client Billing Account ledgers, Dashboard summaries, and hardened WorkManager offline synchronization with robust background sync, retry, parent-child ordering, and snapshot immutability.

## Implemented V1 Features
- **Authentication**: Firebase Auth login and session state management for authorized internal business users.
- **Dashboard**: High-level overview of finalized tax invoices and aggregate total billed metrics.
- **Client Master**: Local CRUD for buyer/client details (Name, Address, GSTIN, State Code, Contact).
- **Product Master**: Catalog management for items (Item Name, HSN/SAC code, default GST rates).
- **Document Generator**: Form-based creation of Tax Invoices and Purchase Orders supporting:
  - Separate, independent numbering sequences
  - Auto-suggested document numbers with full manual override
  - Multi-line item entries
  - Dynamic GST calculations (IGST or CGST + SGST)
  - Frozen historical document snapshots upon finalization
- **In-App PDF Viewer**: On-demand native rendering of finalized documents into A4 PDF pages with zoom, scroll, export to device storage, and system share capabilities.
- **Client Billing History**: Automatic recording of finalized invoices into client account ledgers.
- **Offline Sync**: Local-first Room database operation with background WorkManager synchronization to Cloud Firestore.
- **Theme**: Light and Dark mode UI based on Material 3.

---

## Architecture Summary
The application follows Unidirectional Data Flow (UDF) with strict MVI presentation:
```
Compose UI  --->  MVI ViewModel  --->  Repository  --->  Room Database (Local Source of Truth)
                                                              ▲
                                                         Sync Manager (WorkManager)
                                                              ▼
                                                       Cloud Firestore
```
- **Room Database is the Local Source of Truth**: The UI observes Room data via `Flow`. Network connectivity is not required for local business operations.
- **Dynamic PDF Rendering**: PDFs are rendered on-demand using native Android `PdfDocument` from immutable, frozen document snapshots.
- **Deterministic Money Calculations**: All financial totals are calculated using exact arithmetic (integer minor units / `BigDecimal`). `Double` and `Float` are strictly prohibited for monetary figures.

For complete technical specifications, see [`docs/ARCHITECTURE.md`](file:///Users/macm90/AndroidStudioProjects/VivaanEnterprise/docs/ARCHITECTURE.md).

---

## Technology Stack
The exact library versions are maintained in the version catalog at [`gradle/libs.versions.toml`](file:///Users/macm90/AndroidStudioProjects/VivaanEnterprise/gradle/libs.versions.toml):
- **Language**: Kotlin 2.3.10 (AGP 9 built-in Kotlin)
- **UI Framework**: Jetpack Compose & Material 3
- **Toolchain**: AGP 9.4.0, Gradle 9.6.0, KSP 2.3.10
- **Android Target**: `compileSdk = 37`, `targetSdk = 37`, `minSdk = 26`, JVM 17
- **Dependency Injection**: Dagger Hilt 2.59.2
- **Persistence**: Room 2.8.4 & DataStore Preferences 1.2.1
- **Background Tasks**: WorkManager 2.11.2
- **Remote Backend**: Firebase BOM 34.18.0 (Auth & Firestore)

---

## Project Setup & Environment Requirements

### Prerequisites
1. **JDK 17 or higher** (JDK 21 recommended).
2. **Android Studio** (Ladybug / 2024.2.1 or newer supporting AGP 9.4.0 and API 37).
3. **Android SDK Platform 37** installed via SDK Manager.

### Setup Instructions
1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd VivaanEnterprise
   ```
2. Open the project in Android Studio.
3. Sync Project with Gradle Files.
4. *Note on Firebase*: During foundational steps, `google-services.json` is not required; the app compiles without external Firebase configuration.

---

## Build & Verification Commands
Execute the following commands from the project root to compile, run static analysis, execute tests, and build release APKs:

```bash
# Check Gradle environment version
./gradlew --version

# Verify Gradle task execution
./gradlew help

# Dry run full build task graph
./gradlew build --dry-run

# Assemble Debug APK
./gradlew assembleDebug

# Run Android Lint static code analysis
./gradlew lintDebug

# Run unit tests
./gradlew testDebugUnitTest

# Assemble Release APK (R8 minified & resource shrunk)
./gradlew assembleRelease
```

---

## Documentation Index
- [`AGENTS.md`](file:///Users/macm90/AndroidStudioProjects/VivaanEnterprise/AGENTS.md) — Mandatory engineering rules and AI coding agent guidelines.
- [`DESIGN.md`](file:///Users/macm90/AndroidStudioProjects/VivaanEnterprise/DESIGN.md) — Design system specification, color tokens, typography, and visual rules.
- [`docs/ARCHITECTURE.md`](file:///Users/macm90/AndroidStudioProjects/VivaanEnterprise/docs/ARCHITECTURE.md) — Detailed architecture, layer boundaries, and offline sync philosophy.
- [`docs/DATA_MODEL.md`](file:///Users/macm90/AndroidStudioProjects/VivaanEnterprise/docs/DATA_MODEL.md) — Business entities, schema relations, document snapshot specification, and money precision rules.
- [`docs/PDF_SPEC.md`](file:///Users/macm90/AndroidStudioProjects/VivaanEnterprise/docs/PDF_SPEC.md) — PDF layout structure, Tax Invoice / Purchase Order master templates, and viewer requirements.

---

## Development Guidelines
All contributors and AI agents must strictly adhere to the engineering rules defined in [`AGENTS.md`](file:///Users/macm90/AndroidStudioProjects/VivaanEnterprise/AGENTS.md).
