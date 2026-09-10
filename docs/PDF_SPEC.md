# Vivaan Enterprise PDF Output & Viewer Specification

## 1. General PDF Specifications
Dynamic PDF generation for Tax Invoices and Purchase Orders must satisfy these physical rendering constraints:

- **Format**: Standard A4 (`210mm x 297mm` / `595 x 842 points` at 72 DPI).
- **Orientation**: Portrait.
- **Page Budget**: Designed to fit cleanly on **1 single A4 page** for standard business documents (1–5 line items).
- **Rendering Engine**: Native Android `android.graphics.pdf.PdfDocument`.
- **Viewing Engine**: Native Android `android.graphics.pdf.PdfRenderer`.
- **Third-Party Libraries**: Prohibited.
- **Layout Quality**: Deterministic grid bounds, explicit text wrapping, crisp 1dp slate borders (`#486581`), no clipped text, and high legibility.
- **Formatting**: All numbers formatted using Indian numbering system (e.g., `₹ 1,50,000.00`) and amounts in words (e.g., "RUPEES ONE LAKH FIFTY THOUSAND ONLY").

---

## 2. Master Tax Invoice Layout Structure

The generated Tax Invoice PDF must strictly reproduce the following structural block order based on Vivaan Enterprise master templates:

```text
+-----------------------------------------------------------------------------------+
|                                   TAX INVOICE                                     |
+-----------------------------------------------------------------------------------+
| SUPPLIER INVOICE DETAILS                                                          |
| +-----------------------------------------------+-------------------------------+ |
| | VIVAAN ENTERPRISE                             | Tax Invoice No.  : VE/06/...  | |
| | Near Shalibhadranivas, Opp. Siddhivinayak...  | Dated            : 09-Sep-2026| |
| | GSTIN/UIN: 24CHWPG0910J1ZB                    | Delivery Note    : ...        | |
| | Mobile: +91 97371 78061                       | Mode/Terms Paymt : ...        | |
| +-----------------------------------------------+-------------------------------+ |
| BILL TO (BUYER DETAILS)                                                           |
| Client Name, Address, State Code, GSTIN, PAN, Phone                               |
| DELIVERY / FACTORY ADDRESS (If specified)                                         |
+-----------------------------------------------------------------------------------+
| PRODUCT & SERVICE TABLE                                                           |
| Sl. | Description of Goods | HSN/SAC | Quantity | Rate (₹) | Per | Amount (₹)   |
|-----+----------------------+---------+----------+----------+-----+------------|
| 1   | 3M anti-slip 15mm... | 3919    | 100      | 150.00   | Pcs | 15,000.00  |
+-----------------------------------------------------------------------------------+
| TOTALS & TAX BREAKDOWN                                                            |
| Taxable Value : ₹ 15,000.00                                                       |
| CGST @ 9%     : ₹  1,350.00                                                       |
| SGST @ 9%     : ₹  1,350.00  (or IGST @ 18% : ₹ 2,700.00)                        |
| TOTAL AMOUNT  : ₹ 17,700.00                                                       |
| Amount Chargeable (in words): RUPEES SEVENTEEN THOUSAND SEVEN HUNDRED ONLY        |
+-----------------------------------------------------------------------------------+
| TAX SUMMARY TABLE                                                                 |
| HSN/SAC | Taxable Value | CGST Rate | CGST Amt | SGST Rate | SGST Amt | Total Tax  |
| 3919    | 15,000.00     | 9%        | 1,350.00 | 9%        | 1,350.00 | 2,700.00  |
| Tax Amount (in words): RUPEES TWO THOUSAND SEVEN HUNDRED ONLY                     |
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

## 3. Master Purchase Order Layout Structure

The Purchase Order layout shares the common visual structure with minor header/label adaptations:

1. **Document Header**: Displays **`PURCHASE ORDER`** in place of `TAX INVOICE`.
2. **Metadata Block**: Displays **`PO No.`** in place of `Tax Invoice No.`
3. **Supplier Details**: Displays Supplier/Vendor block.
4. **Delivery Address Block**: Displays **`DELIVERY / FACTORY ADDRESS`**.
5. **Footer Notice**: Displays `This is a Computer Generated Purchase Order`.

---

## 4. Dynamic GST Layout Requirements

The PDF renderer MUST dynamically adjust its tax breakdown section based on the transaction tax type:

### 4.1 Intra-State Transaction (CGST + SGST)
Used when Seller State Code matches Buyer State Code (e.g., Gujarat state code `24`):
- Displays separate lines and columns for **CGST Amount** and **SGST Amount**.
- Tax Summary table split into CGST Rate/Amount and SGST Rate/Amount columns.

### 4.2 Inter-State Transaction (IGST)
Used when Seller State Code differs from Buyer State Code:
- Displays a single consolidated line and column for **IGST Amount**.
- Tax Summary table displays IGST Rate and IGST Amount columns.

---

## 5. Single Source of Financial Data
- The PDF rendering module (`core/pdf/`) **MUST NEVER** recalculate financial amounts using duplicate formula logic.
- The PDF renderer consumes `DocumentCalculationResult` generated by the core calculation engine (`DocumentCalculator`).

---

## 6. PDF Storage & Lifecycle Rules
- **No Persistence in Database**: Raw PDF byte arrays (`ByteArray`) are NOT saved in Room or Firestore.
- **On-Demand Generation**: PDFs are dynamically compiled on-demand from historical document snapshots.
- **Temporary Cache**: Generated PDFs for previewing are written to `context.cacheDir` (`/cache/pdf/temp_doc.pdf`).
- **Exporting**: Users can save PDFs to public downloads via `MediaStore`.
- **Sharing**: Sharing via external apps (WhatsApp, Mail) utilizes `FileProvider` with temporary read permission `content://` URIs (`content://com.vivaanenterprise.app.fileprovider/cache/pdf/...`). `file://` URIs are strictly forbidden.

---

## 7. In-App PDF Viewer Specifications

### Presentation Architecture (`feature/pdfviewer/`)
- `PdfViewerRoute`: Scopes ViewModel and wires back/share/download callbacks.
- `PdfViewerViewModel`: Loads document snapshot, triggers native `PdfDocument` generation to temporary cache file, and initializes `PdfRenderer`.
- `PdfViewerScreen`: Displays full-screen preview with top app bar (Title, Share icon, Export icon, Print icon) and scrollable bitmap pages rendered by `PdfRenderer`.

### Viewer UX Requirements
- Dark/neutral canvas backdrop (`#202B36`) with white document page centered.
- Smooth pinch-to-zoom and panning without breaking vertical list scrolling at 1.0x scale.
- Clear error state with retry button if PDF rendering fails.
