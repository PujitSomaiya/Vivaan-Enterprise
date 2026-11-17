# Phase 21: Compose UI Common Component Refactoring & Code De-duplication - Research

## Context & User Feedback
The user requested:
"all the screen jetpack ui screen have same code nested present do not want that make small function if possible common function and use it when needed. Make proper phase and plan to fix this issues"

---

## Code Investigation & Duplication Analysis

Across `app/src/main/java/com/vivaanenterprise/app/feature/`, there are 14 screen composables totaling over 5,200 lines of code.

### 1. Document & Order Entry Duplication (`InvoiceScreen.kt` & `PurchaseOrderScreen.kt`)
Both screens implement large transactional billing forms (~550 lines each). Analysis reveals ~75% code duplication:
- **Party / Client Selection Card**: Both embed an `AppCard` displaying Seller Details snapshot and Client Details (Company Name, Address, GSTIN, State Code), with an interactive change button triggering `ClientSelectorBottomSheet`.
- **Line Items Editor**: Both render a header with an "Add Item" button and a list of line items, where each line item contains an identical card layout: product picker, item description, HSN/SAC field, quantity field, rate in rupees field, GST percentage selector, and calculated line total.
- **Financial Summary & Tax Calculation Card**: Both render identical financial breakdown cards showing Taxable Amount, CGST, SGST, IGST, Total Tax Amount, and Grand Total with Indian Rupee formatting (`₹ ...`), plus amount in words.
- **Delivery & Transport Metadata Card**: Both render identical optional collapsible/expandable sections for delivery note, supplier reference, buyer order number/date, dispatch through, destination, and terms of delivery.
- **Dialogs**: Both duplicate `DatePickerDialog`, Discard changes `AlertDialog`, and Finalize confirmation `AlertDialog`.

### 2. Detail Screens Info Row Duplication (`ClientDetailScreen.kt`, `ProductDetailScreen.kt`, `DocumentDetailScreen.kt`)
Each detail screen duplicates manual `Row` and `Column` layouts for displaying labeled business metadata (e.g., "GSTIN", "Phone", "Email", "HSN/SAC", "Default Tax Rate"):
```kotlin
Column {
    Text(text = label, style = AppTheme.typography.labelMedium, color = AppTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))
    Text(text = value, style = AppTheme.typography.bodyLarge, color = AppTheme.colorScheme.onSurface)
}
```
This pattern appears over 35 times across the codebase.

### 3. Dialog Duplication Across Screens
- Confirmation dialogs (Delete client, Delete product, Finalize invoice, Finalize PO, Discard draft) are implemented as bespoke `AlertDialog` blocks with repetitive boilerplate.
- Date picker dialogs (`rememberDatePickerState`, `DatePickerDialog`, `DatePicker`) are repeated in `InvoiceScreen`, `PurchaseOrderScreen`, and date filters.

---

## Architectural Strategy

### 1. Design System Additions (`core/designsystem/component/`)
- **`AppConfirmationDialog`**: Clean reusable confirmation dialog accepting `title`, `message`, `confirmLabel`, `isDestructive`, `onConfirm`, and `onDismiss`.
- **`AppDatePickerDialog`**: Reusable date picker modal accepting `initialDateMillis`, `onDateSelected`, and `onDismiss`.
- **`AppInfoRow`**: Standardized two-column or stacked label-value pair with optional copy/action button and emphasis typography.
- **`AppStatusBadge`**: Unified chip for `SyncStatus` (`SYNCED`, `PENDING`, `FAILED`) and `DocumentStatus` (`DRAFT`, `FINALIZED`, `CANCELLED`).

### 2. Shared Document Form Components (`feature/document/common/`)
- **`DocumentPartySection`**: Renders seller and buyer/supplier party summary with client picker trigger.
- **`DocumentLineItemCard`**: Standalone composable for editing a single line item, accepting immutable item state and event lambdas (`onDescriptionChange`, `onQuantityChange`, `onRateChange`, `onGstChange`, `onRemove`).
- **`DocumentFinancialSummary`**: Renders taxable amounts, CGST/SGST/IGST breakdown, and highlighted grand total.
- **`DocumentDeliveryMetadataSection`**: Clean, modular transport metadata fields.

### 3. Target Screen Refactoring
- Refactor `InvoiceScreen.kt` to assemble high-level modular sections instead of monolithic inline nesting (reducing lines by ~50%).
- Refactor `PurchaseOrderScreen.kt` using identical modular sections (reducing lines by ~50%).
- Refactor `ClientDetailScreen.kt` and `ProductDetailScreen.kt` using `AppInfoRow` and `AppConfirmationDialog`.
- Ensure all screens strictly maintain existing MVI contracts (`UiState`, `UiIntent`) without altering screen functionality.
