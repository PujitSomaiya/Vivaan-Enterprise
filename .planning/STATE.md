# Project State: Vivaan Enterprise

## Current Position
- **Active Milestone**: Milestone 2: Production Hardening & Sync Engine Recovery
- **Active Phase**: Phase 20 (Bidirectional Multi-Device Sync Engine) — READY TO PLAN & EXECUTE
- **Active Branch**: `bug/sync-ui-issue-17-nov-2026`
- **Current Status**: Phase 19 completed. Phase 20 (Bidirectional Multi-Device Sync) and Phase 21 (Compose UI Refactoring & Code De-duplication) added to roadmap.

## Diagnosed Root Cause (Bidirectional Sync & Multi-Device Gap)
1. **Clock Skew & Brittle Timestamp Cursor**: Remote pull queries currently filter by `whereGreaterThanOrEqualTo("updatedAt", lastSyncTime)`. In a multi-device setup, differences between device system clocks or delayed synchronization cause remote documents to have timestamps earlier than the receiving device's `lastSyncTime`, permanently hiding them from Firestore pull results.
2. **Missing Outbound Sequence Push**: `DocumentSequence` is never pushed in Step 1 of `SyncRepositoryImpl`, causing different devices to diverge in sequence numbering.
3. **Foreign Key Ingestion Safety**: If parents (Client, Document) are missing or fail during pull, child records (LineItems, ClientAccountEntries) are either dropped or trigger SQLite foreign key failures.
4. **UI Duplication**: Substantial nested composable code is duplicated across `InvoiceScreen`, `PurchaseOrderScreen`, `ClientListScreen`, `DocumentsScreen`, and detail screens.

## Key Decisions
- [DEC-001] Room database is the sole local source of truth for the UI; Firestore is never directly observed.
- [DEC-002] Financial calculations are performed strictly in integer paise (`Long`) by `DocumentCalculator`.
- [DEC-003] Finalized invoices/purchase orders freeze master entity snapshots to preserve historical immutability.
- [DEC-004] PDFs are generated dynamically on demand using native Android APIs without third-party PDF dependencies.
- [DEC-005] Entities in `SyncStatus.FAILED` must automatically be retried by the sync engine upon subsequent sync triggers or manual retries.
- [DEC-006] Inbound sync must use a safe lookback window and full reconciliation support so multi-device clock skews never drop records.
- [DEC-007] Shared UI patterns across Invoice, PO, and List/Detail screens must be extracted into focused, reusable components.

## Next Action
- Execute planning for Phase 20 via `/gsd-plan-phase 20` to fix inbound/outbound sync across devices.
