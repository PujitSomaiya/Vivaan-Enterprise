# Project State: Vivaan Enterprise

## Current Position
- **Active Milestone**: Milestone 2: Production Hardening & Sync UI Stability
- **Active Phase**: Phase 19 (Sync UI & State Indicator Hardening)
- **Active Branch**: `bug/sync-ui-issue-17-nov-2026`
- **Current Status**: Onboarding complete. Codebase fully mapped with 7 evidence-backed documents in `.planning/codebase/`. Planning infrastructure initialized.

## Codebase Map Summary
- **Architecture**: MVI / UDF pattern with Room as local source of truth, WorkManager sync to Cloud Firestore, native PDF generation.
- **Tech Stack**: Kotlin 2.3.10, Jetpack Compose, Material 3, Room, Hilt, AGP 9.4.0, targetSdk 37.
- **Quality & Conventions**: Integer minor unit `Long` paise for financial math, no `Double`/`Float` money calculations, strict coroutine cancellation handling, stateless Compose screens.
- **Concerns Identified**: Firestore DTO ProGuard reflection rules in release, WorkManager retry exponential backoff tuning, single-page PDF overflow risk on large document line counts.

## Key Decisions
- [DEC-001] Room database is the sole local source of truth for the UI; Firestore is never directly observed.
- [DEC-002] Financial calculations are performed strictly in integer paise (`Long`) by `DocumentCalculator`.
- [DEC-003] Finalized invoices/purchase orders freeze master entity snapshots to preserve historical immutability.
- [DEC-004] PDFs are generated dynamically on demand using native Android APIs without third-party PDF dependencies.

## Blockers
- None currently blocking.

## Next Action
- Inspect or address the active sync UI issue on branch `bug/sync-ui-issue-17-nov-2026` via `/gsd-plan-phase 19` or `/gsd-progress`.
