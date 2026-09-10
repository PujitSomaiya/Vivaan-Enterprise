# Vivaan Enterprise PDF Output & Viewer Specification

## 1. General PDF Specifications
Dynamic PDF generation for Tax Invoices and Purchase Orders must satisfy these physical rendering constraints:

- **Format**: Standard A4 (`210mm x 297mm` / `595 x 842 points` at 72 DPI).
- **Orientation**: Portrait.
- **Margins**: Left `30pt`, Right `565pt` (Width: `595pt`), Top `30pt`, Bottom `812pt` (Height: `842pt`), Content Width `535pt`.
- **Page Budget**: Designed to fit cleanly on **1 single A4 page** for standard business documents (1–5 line items).
- **Rendering Engine**: Native Android `android.graphics.pdf.PdfDocument`.
- **Viewing Engine**: Native Android `android.graphics.pdf.PdfRenderer`.
- **Third-Party Libraries**: Prohibited.
- **Layout Quality**: Professional modern business document styling, sharp information hierarchy, subtle brand identity, crisp slate borders (`#486581`), muted headers (`#F0F4F8`), deterministic grid bounds, clean text wrapping without clipping, right-aligned monetary values, and high legibility.
- **Formatting**: All numbers formatted using Indian currency system (e.g., `₹ 1,50,000.00`) and amounts in words (e.g., "RUPEES ONE LAKH FIFTY THOUSAND ONLY").

---

## 2. Master Tax Invoice Visual Layout Structure

The generated Tax Invoice PDF strictly reproduces the following structural block order based on Vivaan Enterprise master templates:

```text
+-----------------------------------------------------------------------------------+
| [VE Logo]  VIVAAN ENTERPRISE                                     TAX INVOICE      |
|            Near Shalibhadranivas, Opp. Siddhivinayak...                           |
|            GSTIN/UIN: 24CHWPG0910J1ZB | Mobile: +91 97371 78061                       |
+-----------------------------------------------------------------------------------+
| DOCUMENT METADATA GRID                                                            |
| Invoice No.    : VE/06/2026-27                | Dated          : 09 Sep 2026      |
| Delivery Note  : ...                          | Payment Terms  : ...              |
| Buyer Order No.: ...                          | Place of Supply: 33               |
+-----------------------------------------------------------------------------------+
| BILL TO (BUYER DETAILS)                       | DELIVERY / FACTORY ADDRESS        |
| Client Name, Address, State Code, GSTIN       | Delivery Address (if present)     |
+-----------------------------------------------------------------------------------+
| PRODUCT & SERVICE TABLE                                                           |
| Sr.| Description of Goods/Services | HSN/SAC | Qty | Rate (₹) | Taxable Amt (₹) |
|----+-------------------------------+---------+-----+----------+------------------|
| 1  | 3M anti-slip 15mm Scotch Tape | 3919    | 5   | 3,500.00 | 17,500.00        |
+-----------------------------------------------------------------------------------+
| TOTALS & TAX BREAKDOWN                                                            |
|                                                Taxable Amount : ₹ 17,500.00       |
|                                                CGST @ 9%      : ₹  1,575.00       |
|                                                SGST @ 9%      : ₹  1,575.00       |
|                                                Total Tax      : ₹  3,150.00       |
|                                                GRAND TOTAL    : ₹ 20,650.00       |
| Amount Chargeable (in words): RUPEES TWENTY THOUSAND SIX HUNDRED FIFTY ONLY       |
+-----------------------------------------------------------------------------------+
| TAX SUMMARY TABLE                                                                 |
| HSN/SAC | Taxable Value | CGST Rate | CGST Amt | SGST Rate | SGST Amt | Total Tax  |
| 3919    | 17,500.00     | 9%        | 1,575.00 | 9%        | 1,575.00 | 3,150.00  |
| Tax Amount (in words): RUPEES THREE THOUSAND ONE HUNDRED FIFTY ONLY               |
+-----------------------------------------------------------------------------------+
| BANK DETAILS & DECLARATION                      | AUTHORISED SIGNATORY            |
| Bank: HDFC BANK | A/C: 50100419622062           | For VIVAAN ENTERPRISE           |
| IFSC: HDFC0000299 | Branch: Paldi, Ahmedabad    |                                 |
| Declaration: We declare that this invoice...    | Authorised Signatory            |
+-----------------------------------------------------------------------------------+
|                       This is a Computer Generated Invoice                        |
+-----------------------------------------------------------------------------------+
```

---

## 3. Master Purchase Order Visual Layout Structure

The Purchase Order layout shares the common visual structure with minor header/label adaptations:

1. **Document Header Banner**: Displays **`PURCHASE ORDER`** in place of `TAX INVOICE`.
2. **Metadata Grid**: Displays **`PO No.`** in place of `Invoice No.`
3. **Supplier Details Block**: Displays **`SUPPLIER / VENDOR`** block.
4. **Delivery Address Block**: Displays **`DELIVERY / FACTORY ADDRESS`**.
5. **Footer Notice**: Displays `This is a Computer Generated Purchase Order`.

---

## 4. Branding & VE Monogram Vector Logo Rules

- **Native Vector Rendering**: To ensure crisp print quality without pixelation or clipping, the "VE" monogram is drawn natively onto the PDF `Canvas` using geometric paths (matching `VeLogo.kt` design).
- **Proportional Bounds**: The logo is constrained to a `$36 \times 36\text{ pt}$` vector badge inside the header box without squashing or stretching.
- **Separation**: The logo badge sits cleanly on the left margin, aligned with seller title text, never overlapping seller details.

---

## 5. Dynamic GST Layout Requirements

The PDF renderer MUST dynamically adjust its tax breakdown section based on the transaction tax type:

### 5.1 Intra-State Transaction (CGST + SGST)
Used when Seller State Code matches Buyer State Code (e.g., Gujarat state code `24`):
- Displays separate lines and columns for **CGST Amount** and **SGST Amount**.
- Tax Summary table split into CGST Rate/Amount and SGST Rate/Amount columns.

### 5.2 Inter-State Transaction (IGST)
Used when Seller State Code differs from Buyer State Code:
- Displays a single consolidated line and column for **IGST Amount**.
- Tax Summary table displays IGST Rate and IGST Amount columns.

---

## 6. Single Source of Financial Data
- The PDF rendering module (`core/pdf/`) **MUST NEVER** recalculate financial amounts using duplicate formula logic.
- The PDF renderer consumes persisted frozen snapshot amounts from `BusinessDocument`.

---

## 7. Dynamic Measurement & Multi-Page Pagination Rules
- **Pre-Drawing Measurement Contract**: All variable-length textual sections (seller header, party address blocks, line item descriptions, bank details, declaration) **MUST** be measured dynamically using `StaticLayout` before drawing surrounding section frames or borders.
- **Font Metrics & Line Height**: Line spacing and text heights are derived from `StaticLayout` line height metrics with `1.05x` multiplier and `2pt` line spacing so text lines never collide or overlap vertically.
- **Dynamic Header & Section Heights**: Section boundary heights (e.g. `headerHeight`, `blockHeight`, `partySectionHeight`) are derived from `maxOf(measuredContentHeight + padding)` rather than hardcoded fixed line counts.
- **Page Break Rule**: Line-item rows are never split across page boundaries. If a row height exceeds remaining page budget, a new page is started and table headers are repeated.
- **Header Continuation**: Multi-page documents repeat a compact header (Document Number, Title, Page Number) at the top of continuation pages.
- **Summary Section Protection**: The Grand Total, Bank Details, Declaration, and Authorised Signatory block are kept together; if space is insufficient, the summary block moves cleanly to the next page.

---

## 8. PDF Storage, Naming & Lifecycle Rules
- **No Persistence in Database**: Raw PDF byte arrays (`ByteArray`) are NOT saved in Room or Firestore.
- **On-Demand Generation**: PDFs are dynamically compiled on-demand from historical document snapshots.
- **Meaningful Filenames**:
  - Tax Invoice: `Tax_Invoice_<DocumentNumber>_<ClientName>.pdf`
  - Purchase Order: `Purchase_Order_<DocumentNumber>_<ClientOrSupplierName>.pdf`
- **Temporary Cache**: Generated PDFs for previewing are written to `context.cacheDir` (`/cache/pdf/temp_doc.pdf`).
- **Exporting**: Users can save PDFs to public downloads via `MediaStore`.
- **Sharing**: Sharing via external apps (WhatsApp, Mail) utilizes `FileProvider` with temporary read permission `content://` URIs (`content://com.vivaanenterprise.app.fileprovider/cache/pdf/...`). `file://` URIs are strictly forbidden.

---

## 9. In-App PDF Viewer Specifications

### Presentation Architecture (`feature/pdfviewer/`)
- `PdfViewerRoute`: Scopes ViewModel and wires back/share/download callbacks.
- `PdfViewerViewModel`: Loads document snapshot, triggers native `PdfDocument` generation to temporary cache file, and initializes `PdfRenderer`.
- `PdfViewerScreen`: Displays full-screen preview with top app bar (Title, Share icon, Export icon, Print icon) and scrollable bitmap pages rendered by `PdfRenderer`.

### Viewer UX Requirements
- Dark/neutral canvas backdrop (`#202B36`) with white document page centered.
- Smooth pinch-to-zoom and panning without breaking vertical list scrolling at 1.0x scale.
- Clear error state with retry button if PDF rendering fails.

