# Phase 21: Compose UI Common Component Refactoring & Code De-duplication - Execution Plan

## Phase Overview
- **Goal**: Refactor redundant, nested Jetpack Compose UI code across screens into focused, reusable components adhering to `DESIGN.md` tokens and MVI stateless composable rules.
- **Estimated Complexity**: Medium
- **Prerequisites**: Phase 20 completed. Active branch `bug/sync-inbound-issue-17-nov-2026`.

---

## Tasks

### Task 1: Reusable Core Dialogs & Info Row Components
- **Objective**: Create reusable foundation composables in `app/src/main/java/com/vivaanenterprise/app/core/designsystem/component/`.
- **Files Created**:
  - `app/src/main/java/com/vivaanenterprise/app/core/designsystem/component/AppConfirmationDialog.kt`
  - `app/src/main/java/com/vivaanenterprise/app/core/designsystem/component/AppDatePickerDialog.kt`
  - `app/src/main/java/com/vivaanenterprise/app/core/designsystem/component/AppInfoRow.kt`
- **Changes**:
  - `AppConfirmationDialog`: Standard Material 3 `AlertDialog` with configurable title, message, confirm text, cancel text, and optional destructive styling (`AppTheme.colorScheme.error`).
  - `AppDatePickerDialog`: Standard `DatePickerDialog` wrapping `DatePicker` with confirm/cancel buttons and epoch millisecond emission.
  - `AppInfoRow`: Clean stacked and horizontal label-value composables with typography from `DESIGN.md` (`labelMedium` for captions, `bodyLarge`/`titleMedium` for values).
- **Verification**: Run `./gradlew compileDebugKotlin` to verify compilation.

---

### Task 2: Shared Document & Order Form Section Components
- **Objective**: Extract common form sections used identically in Invoice and Purchase Order creation screens into `app/src/main/java/com/vivaanenterprise/app/feature/document/common/`.
- **Files Created**:
  - `app/src/main/java/com/vivaanenterprise/app/feature/document/common/DocumentPartyCard.kt`
  - `app/src/main/java/com/vivaanenterprise/app/feature/document/common/DocumentLineItemCard.kt`
  - `app/src/main/java/com/vivaanenterprise/app/feature/document/common/DocumentFinancialSummaryCard.kt`
  - `app/src/main/java/com/vivaanenterprise/app/feature/document/common/DocumentDeliveryDetailsCard.kt`
- **Changes**:
  - `DocumentPartyCard`: Renders Seller Snapshot and Client/Supplier Snapshot with interactive selection button.
  - `DocumentLineItemCard`: Renders line item card with product picker, description, HSN/SAC, quantity, rate (paise/rupees), GST rate, and delete action.
  - `DocumentFinancialSummaryCard`: Renders Taxable Value, CGST, SGST, IGST, Total Tax, and Grand Total with Indian Rupee formatting (`₹ ...`).
  - `DocumentDeliveryDetailsCard`: Collapsible or clean card for optional transport metadata (Delivery Note, Supplier Ref, Buyer Order, Dispatch Through).
- **Verification**: Compile and verify Compose previews.

---

### Task 3: Refactor `InvoiceScreen.kt` & `PurchaseOrderScreen.kt`
- **Objective**: Replace monolithic inline UI code in `InvoiceScreen.kt` and `PurchaseOrderScreen.kt` with the newly extracted shared components.
- **Files Modified**:
  - `app/src/main/java/com/vivaanenterprise/app/feature/invoice/presentation/InvoiceScreen.kt`
  - `app/src/main/java/com/vivaanenterprise/app/feature/purchaseorder/presentation/PurchaseOrderScreen.kt`
- **Changes**:
  - Replace bespoke Dialogs with `AppConfirmationDialog` and `AppDatePickerDialog`.
  - Replace inline party cards with `DocumentPartyCard`.
  - Replace inline line item cards with `DocumentLineItemCard`.
  - Replace inline financial totals with `DocumentFinancialSummaryCard`.
  - Replace inline transport fields with `DocumentDeliveryDetailsCard`.
  - Verify line count reduction from ~550 lines down to ~220 lines per screen.
- **Verification**: Run `./gradlew compileDebugKotlin` and verify UI previews.

---

### Task 4: Refactor Detail Screens with `AppInfoRow` & `AppConfirmationDialog`
- **Objective**: Clean up repetitive layout boilerplate in detail screens.
- **Files Modified**:
  - `app/src/main/java/com/vivaanenterprise/app/feature/client/presentation/detail/ClientDetailScreen.kt`
  - `app/src/main/java/com/vivaanenterprise/app/feature/product/presentation/detail/ProductDetailScreen.kt`
- **Changes**:
  - Replace manual column label-value pairs with `AppInfoRow`.
  - Replace delete `AlertDialog` with `AppConfirmationDialog`.
- **Verification**: Run `./gradlew compileDebugKotlin`.

---

### Task 5: Comprehensive Verification & Release Build
- **Objective**: Ensure that all unit tests, UI rendering, and release R8 minification pass with exit code 0.
- **Commands**:
  ```bash
  ./gradlew testDebugUnitTest
  ./gradlew lintDebug
  ./gradlew assembleDebug
  ./gradlew assembleRelease
  ```
- **Acceptance Criteria**:
  - [ ] Duplicate nested UI code is removed and centralized in reusable components.
  - [ ] `InvoiceScreen.kt` and `PurchaseOrderScreen.kt` are dramatically simplified.
  - [ ] All existing MVI user flows (creating invoices, editing POs, client/product management) function identically without regressions.
  - [ ] 43/43 unit tests pass.
  - [ ] Release R8 minification passes cleanly.
