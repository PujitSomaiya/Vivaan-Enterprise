# Project State: Vivaan Enterprise

## Current Position
- **Active Milestone**: Milestone 2: Production Hardening & Sync Engine Recovery
- **Active Phase**: Phase 21 (Compose UI Refactoring & Code De-duplication) — COMPLETED
- **Active Branch**: `bug/sync-inbound-issue-17-nov-2026`
- **Current Status**: Phase 21 executed and verified with 43/43 unit tests, assembleDebug, and assembleRelease (R8 minification) passing. Over 380 lines of duplicate UI boilerplate eliminated across screens.

## Diagnosed Root Cause (Bidirectional Sync & Multi-Device Gap)
1. **Clock Skew & Brittle Timestamp Cursor**: Remote pull queries currently filter by `whereGreaterThanOrEqualTo("updatedAt", lastSyncTime)`. In a multi-device setup, differences between device system clocks or delayed synchronization cause remote documents to have timestamps earlier than the receiving device's `lastSyncTime`, permanently hiding them from Firestore pull results. (Resolved in Phase 20)
2. **Missing Outbound Sequence Push**: `DocumentSequence` is never pushed in Step 1 of `SyncRepositoryImpl`, causing different devices to diverge in sequence numbering. (Resolved in Phase 20)
3. **Foreign Key Ingestion Safety**: If parents (Client, Document) are missing or fail during pull, child records (LineItems, ClientAccountEntries) are either dropped or trigger SQLite foreign key failures. (Resolved in Phase 20)
4. **UI Duplication**: Substantial nested composable code is duplicated across `InvoiceScreen`, `PurchaseOrderScreen`, and detail screens. (Resolved in Phase 21 with reusable components)

## Key Decisions
- [DEC-001] Room database is the sole local source of truth for the UI; Firestore is never directly observed.
- [DEC-002] Financial calculations are performed strictly in integer paise (`Long`) by `DocumentCalculator`.
- [DEC-003] Finalized invoices/purchase orders freeze master entity snapshots to preserve historical immutability.
- [DEC-004] PDFs are generated dynamically on demand using native Android APIs without third-party PDF dependencies.
- [DEC-005] Entities in `SyncStatus.FAILED` must automatically be retried by the sync engine upon subsequent sync triggers or manual retries.
- [DEC-006] Inbound sync must use a safe lookback window and full reconciliation support so multi-device clock skews never drop records.
- [DEC-007] Shared UI patterns across Invoice, PO, and List/Detail screens must be extracted into focused, reusable components.

## Next Action
- Phase 22 (Performance & Recomposition Audit) or user review.
