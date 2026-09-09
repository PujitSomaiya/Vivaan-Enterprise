# Vivaan Enterprise Conceptual Data Model Specification

## 1. Domain Entities Overview
This document specifies the business data entities, relationships, historical snapshot structures, and financial precision requirements for Vivaan Enterprise.

```text
[BusinessProfile]  ---> (Provides Seller Snapshot for Documents)
        │
        ├─── 1 : N ───> [BusinessDocument] <─── N : 1 ─── [Client]
        │                     │
        │                     ├─── 1 : N ───> [DocumentLineItem] <─── N : 1 ─── [Product]
        │                     │
        │                     └─── 1 : 1 ───> [ClientAccountEntry]
        │
        └───────────────> [DocumentSequence]
```

---

## 2. Business Models Specification

### 2.1 Business Profile (`BusinessProfile`)
Represents the fixed seller identity for Vivaan Enterprise. Provides the seller snapshot for all newly generated Tax Invoices and Purchase Orders.

| Field Name | Type | Description / Default Value |
| :--- | :--- | :--- |
| `businessName` | `String` | `"VIVAAN ENTERPRISE"` |
| `addressLine1` | `String` | `"NEAR SHALIBHADRANIVAS, OPP. SIDDHIVINAYAK HOUSE"` |
| `addressLine2` | `String` | `"STREET NO. 4, MAHATMA GANDHI ROAD, JORAWAR NAGAR"` |
| `cityStatePincode` | `String` | `"SURENDRANAGAR, GUJARAT - 363020"` |
| `gstin` | `String` | `"24CHWPG0910J1ZB"` |
| `mobile` | `String` | `"+91 97371 78061"` |
| `pan` | `String` | `"CHWPG0910J"` |
| `bankAccountName` | `String` | `"SHETH JANVI"` |
| `bankName` | `String` | `"HDFC BANK"` |
| `bankAccountNumber`| `String` | `"50100419622062"` |
| `bankIfsc` | `String` | `"HDFC0000299"` |
| `bankBranch` | `String` | `"SHAIVAL COMPLEX, OPP. CHANDANBALA TOWERS, PALDI, AHMEDABAD - 380 007"` |
| `declaration` | `String` | Standard invoice declaration text |
| `authorisedSignatory`| `String` | `"For VIVAAN ENTERPRISE"` |

---

### 2.2 Client (`Client`)
Master catalog of buyer/client information.

| Field Name | Type | Optionality | Description |
| :--- | :--- | :--- | :--- |
| `id` | `String` | Required | UUID primary key |
| `companyName` | `String` | Required | Registered company name (e.g., "Green Eco Industries") |
| `address` | `String` | Optional | Street address & city |
| `gstin` | `String` | Optional | 15-character GSTIN (empty if unregistered) |
| `state` | `String` | Optional | State name (e.g., "Gujarat", "Maharashtra") |
| `stateCode` | `String` | Optional | 2-digit GST state code (e.g., "24", "27") |
| `email` | `String` | Optional | Contact email address |
| `phone` | `String` | Optional | Contact phone number |
| `pan` | `String` | Optional | Permanent Account Number |
| `iec` | `String` | Optional | Import Export Code |
| `otherDetails` | `String` | Optional | Miscellaneous notes |
| `createdAt` | `Long` | Required | Unix timestamp ms |
| `updatedAt` | `Long` | Required | Unix timestamp ms |
| `isDeleted` | `Boolean` | Required | Soft deletion flag for sync |
| `syncStatus` | `Enum` | Required | `PENDING`, `SYNCED`, `FAILED` |

*Note: `state` and `stateCode` are optional on `Client` master records so saving incomplete client entries is not blocked. However, when finalizing a Tax Invoice, all information required for GST determination (seller state code, place of supply, buyer state code where applicable) must be validated before finalization.*

---

### 2.3 Product (`Product`)
Master catalog of products and services.

| Field Name | Type | Optionality | Description |
| :--- | :--- | :--- | :--- |
| `id` | `String` | Required | UUID primary key |
| `name` | `String` | Required | Product description (e.g., "3M anti-slip 15mm Scotch Tape") |
| `hsnSac` | `String` | Optional | HSN or SAC code (e.g., "3919") |
| `defaultGstRate` | `BigDecimal` | Optional | Default GST percentage (e.g., 18.00) |
| `isActive` | `Boolean` | Required | Active status in catalog |
| `createdAt` | `Long` | Required | Unix timestamp ms |
| `updatedAt` | `Long` | Required | Unix timestamp ms |
| `isDeleted` | `Boolean` | Required | Soft deletion flag |
| `syncStatus` | `Enum` | Required | `PENDING`, `SYNCED`, `FAILED` |

*Note: Rate/price is NOT fixed in Product master. Unit rate is entered/adjusted per document line item.*


---

### 2.4 Business Document (`BusinessDocument`)
Core entity for Tax Invoices and Purchase Orders.

| Field Name | Type | Description |
| :--- | :--- | :--- |
| `id` | `String` | UUID primary key |
| `documentType` | `Enum` | `TAX_INVOICE` or `PURCHASE_ORDER` |
| `documentNumber` | `String` | User-visible document number (e.g., `"VE/06/2026-27"`) |
| `documentDate` | `Long` | Document issuance date timestamp |
| `status` | `Enum` | `DRAFT`, `FINALIZED`, `CANCELLED` |
| `clientId` | `String` | Reference to Client master |
| **`sellerSnapshot`** | `Embedded` | Frozen copy of `BusinessProfile` details at finalization |
| **`clientSnapshot`** | `Embedded` | Frozen copy of `Client` details at finalization |
| `taxType` | `Enum` | `INTRA_STATE` (CGST+SGST) or `INTER_STATE` (IGST) |
| `taxableAmount` | `Long` | Total taxable value in paise |
| `cgstAmount` | `Long` | Central GST in paise |
| `sgstAmount` | `Long` | State GST in paise |
| `igstAmount` | `Long` | Integrated GST in paise |
| `totalTaxAmount` | `Long` | Total tax in paise |
| `grandTotal` | `Long` | Final total amount in paise |
| `amountInWords` | `String` | Generated grand total in words |
| `taxInWords` | `String` | Generated total tax in words |
| `paymentTerms` | `String` | Optional payment terms text |
| `dispatchDetails` | `Embedded` | Delivery note, dispatch doc no, vehicle no, terms |
| `createdAt` | `Long` | Creation timestamp |
| `updatedAt` | `Long` | Modification timestamp |
| `finalizedAt` | `Long?` | Timestamp when document was finalized |
| `syncStatus` | `Enum` | `PENDING`, `SYNCED`, `FAILED` |

---

### 2.5 Document Line Item (`DocumentLineItem`)
Line items attached to a `BusinessDocument`.

| Field Name | Type | Description |
| :--- | :--- | :--- |
| `id` | `String` | UUID primary key |
| `documentId` | `String` | Foreign key to `BusinessDocument` |
| `productId` | `String?` | Optional reference to Product master |
| `itemIndex` | `Int` | Display order index (1, 2, 3...) |
| `description` | `String` | Product/item description snapshot |
| `hsnSac` | `String` | HSN/SAC code snapshot |
| `quantity` | `Int` | Unit quantity |
| `unitRate` | `Long` | Price per unit in paise |
| `gstRate` | `BigDecimal` | Applicable GST percentage (e.g., 18.00) |
| `taxableAmount` | `Long` | `quantity * unitRate` in paise |
| `cgstAmount` | `Long` | CGST in paise |
| `sgstAmount` | `Long` | SGST in paise |
| `igstAmount` | `Long` | IGST in paise |
| `lineTotal` | `Long` | `taxableAmount + totalLineTax` in paise |

---

### 2.6 Document Sequence (`DocumentSequence`)
Tracks sequential counter states for document auto-numbering.

| Field Name | Type | Description |
| :--- | :--- | :--- |
| `documentType` | `Enum` | `TAX_INVOICE` or `PURCHASE_ORDER` (Primary Key) |
| `financialYear` | `String` | Current financial year string (e.g., `"2026-27"`) |
| `lastSequenceNumber` | `Int` | Counter integer (e.g., `6` for `"VE/06/2026-27"`) |
| `prefix` | `String` | Format prefix (e.g., `"VE/"`) |

*Rules:*
- Invoice and Purchase Order numbering sequences are separate and independent.
- Automatic numbering provides a suggested value (e.g., `VE/06/2026-27`), but automatic numbering is convenience, not authority.
- The user may manually override the entire suggested document number.
- Database index, duplicate handling, and repository validation strategies are deferred to the actual document implementation steps.

---


### 2.7 Client Account Entry (`ClientAccountEntry`)
Records billing history and accounts receivable for clients.

| Field Name | Type | Description |
| :--- | :--- | :--- |
| `id` | `String` | UUID primary key |
| `clientId` | `String` | Foreign key to `Client` |
| `documentId` | `String?` | Foreign key to `BusinessDocument` |
| `entryDate` | `Long` | Transaction date timestamp |
| `entryType` | `Enum` | `INVOICE` (debit/receivable), `PAYMENT` (credit), `ADJUSTMENT` |
| `amount` | `Long` | Transaction value in paise |
| `narration` | `String` | Entry description (e.g., "Tax Invoice #VE/06/2026-27") |
| `createdAt` | `Long` | Creation timestamp |

*Note: Finalizing a `TAX_INVOICE` creates a `ClientAccountEntry` (`INVOICE`). Finalizing a `PURCHASE_ORDER` does NOT create a revenue entry.*

---

## 3. Financial Precision & Money Representation

### 3.1 Strict Integer Minor Units (Paise) Rule
- Floating-point types (`Double`, `Float`) are **STRICTLY PROHIBITED** for storing or calculating monetary values.
- All persisted monetary figures use a single exact representation based on integer minor units: `Long` paise, where ₹ 1.00 = 100 paise (e.g., ₹ 2,500.00 = `250000L`, ₹ 48,852.00 = `4885200L`).

### 3.2 Tax Calculation Rules & Determinism
- Percentage/tax calculations must be deterministic and avoid binary floating-point arithmetic.
- All tax calculations (UI display, database persistence, and PDF rendering) use the exact same output from `DocumentCalculator`.
- Detailed arithmetic and rounding rules belong to Step 9/10 implementation.


---

## 4. Immutable Historical Document Snapshots
1. When a document status transitions to `FINALIZED`:
   - `sellerSnapshot` copies current `BusinessProfile` values.
   - `clientSnapshot` copies current `Client` record values.
   - Line items copy product descriptions, HSN/SAC, rates, and tax percentages.
2. Future edits or soft-deletions of `Client` or `Product` records in master catalogs will have **ZERO effect** on existing finalized `BusinessDocument` records.
3. PDF generation always reads from the embedded snapshot.
