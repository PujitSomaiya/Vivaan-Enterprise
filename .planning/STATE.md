# Project State: Vivaan Enterprise

## Current Position
- **Active Milestone**: Milestone 2: Production Hardening & Sync Engine Recovery
- **Active Phase**: Phase 19 (Firestore Sync Outage Recovery & UI Hardening)
- **Active Branch**: `bug/sync-ui-issue-17-nov-2026`
- **Current Status**: Root cause diagnosed for multi-device sync breakdown following expired Firebase security rules. Phase 19 scoped with engine auto-recovery, periodic sync, and manual sync UI.

## Diagnosed Root Cause (Sync Breakdown after Rule Expiration)
1. **Permanent `FAILED` State**: When Firebase rules expired, entity push failed. Catch blocks set `syncStatus = SyncStatus.FAILED`. However, `SyncRepositoryImpl.synchronize()` strictly queries `getBySyncStatus(SyncStatus.PENDING)`. All failed entities are permanently orphaned and never retried even after rules are fixed on the backend.
2. **Conflict Engine Lockdown**: `SyncMergePolicy` protects local entities with `SyncStatus.FAILED` (`REJECT_REMOTE_PROTECT_LOCAL`), locking them against remote reconciliations.
3. **No Background Periodic Sync**: WorkManager only schedules one-time requests upon startup and write operations.
4. **No Manual Sync / Diagnostic Trigger**: The UI lacks a "Sync Now" / "Retry" action and error display to recover stuck records or give feedback.

## Key Decisions
- [DEC-001] Room database is the sole local source of truth for the UI; Firestore is never directly observed.
- [DEC-002] Financial calculations are performed strictly in integer paise (`Long`) by `DocumentCalculator`.
- [DEC-003] Finalized invoices/purchase orders freeze master entity snapshots to preserve historical immutability.
- [DEC-004] PDFs are generated dynamically on demand using native Android APIs without third-party PDF dependencies.
- [DEC-005] Entities in `SyncStatus.FAILED` must automatically be retried by the sync engine upon subsequent sync triggers or manual retries.

## Next Action
- Run `/gsd-plan-phase 19` to produce the execution plan (`PLAN.md`) and implement the fix.
