# Vivaan Enterprise Design System Specification

## 1. Design Direction
Vivaan Enterprise is a calm, professional, business-focused utility app built for daily invoicing, purchase ordering, and client accounting.

### Key Visual Principles:
- **Professional & Clean**: Utilitarian aesthetic with high information density, clear contrast, and minimal distraction.
- **Cool-Toned Business Palette**: Dominated by deep slates, cool grays, and subtle steel blue accents.
- **Restrained & Minimal**: No flashy gradients, no decorative rounded cards, no cartoonish icons, and no unnecessary animations.
- **Utility-First**: Clear form labels, legible financial tables, distinct total highlights, and unambiguous field-level validation errors.

---

## 2. Color System & Semantic Tokens

### Light Theme (Default)
| Token Name | Hex Code | Role / Usage |
| :--- | :--- | :--- |
| **Primary** | `#334E68` | Primary branding, active tab icons, key action buttons |
| **On Primary** | `#FFFFFF` | Text/icons on Primary background |
| **Primary Container** | `#D9E2EC` | Selected chips, table header fills, subtle focus states |
| **On Primary Container** | `#102A43` | Text/icons on Primary Container background |
| **Secondary** | `#486581` | Subtitles, secondary buttons, structural borders |
| **Tertiary / Accent** | `#627D98` | Interactive accents, secondary icons, badge fills |
| **Background** | `#F8FAFC` | App background behind scrollable screens |
| **Surface** | `#FFFFFF` | Card backgrounds, dialog surfaces, top app bar |
| **Surface Variant** | `#E9EFF5` | Form field backgrounds, divider lines, table row zebra striping |
| **Primary Text** | `#102A43` | High-emphasis body text, titles, invoice numbers |
| **Secondary Text** | `#627D98` | Medium-emphasis captions, field labels, metadata |
| **Outline** | `#BCCCDC` | Text field outlines, card borders, table grid lines |
| **Error** | `#D32F2F` | Validation errors, destructive action dialogs |
| **Success** | `#2E7D32` | `SYNCED` indicators, payment completion badges |
| **Warning** | `#ED6C02` | `PENDING` sync status badges, duplicate document number warnings |

### Dark Theme
| Token Name | Hex Code | Role / Usage |
| :--- | :--- | :--- |
| **Primary** | `#9FB3C8` | Primary branding, active tab icons |
| **On Primary** | `#102A43` | Text/icons on Primary background |
| **Primary Container** | `#243B53` | Selected chip fills, table header containers |
| **Background** | `#0B1219` | App background |
| **Surface** | `#17202A` | Card backgrounds, dialog surfaces |
| **Elevated Surface** | `#202B36` | Floating action surfaces, app bar background |
| **Primary Text** | `#F0F4F8` | High-emphasis text |
| **Secondary Text** | `#BCCCDC` | Medium-emphasis text |
| **Outline** | `#486581` | Borders, outlines, dividers |

---

## 3. Typography & Hierarchy
The application uses native Android system typography (Roboto) to avoid external Google Fonts network dependencies.

| Style Role | Font Weight | Line Height / Spacing | Usage |
| :--- | :--- | :--- | :--- |
| `Title Large` | SemiBold (600) | 22sp / 28sp | Screen headers (e.g., "Tax Invoice #VE/06/2026-27") |
| `Title Medium` | Medium (500) | 16sp / 24sp | Card section titles, dialog titles, table section headers |
| `Body Large` | Normal (400) | 16sp / 24sp | Form input text, primary list item titles |
| `Body Medium` | Normal (400) | 14sp / 20sp | Subtitle text, table cell content, client address lines |
| `Label Large` | Medium (500) | 14sp / 20sp | Button text, active tab labels |
| `Label Medium` | Medium (500) | 12sp / 16sp | Form input labels, sync status tags, table header text |

*All typography MUST support system font scaling for accessibility.*

---

## 4. Spacing Scale
Layouts follow a strict 4dp/8dp modular spacing grid:
- **`xxs`**: 2dp (micro offsets, inline badge padding)
- **`xs`**: 4dp (tight component spacing, chip padding)
- **`sm`**: 8dp (field-to-label spacing, inner card padding)
- **`md`**: 16dp (standard screen padding, list item gaps, form field vertical gaps)
- **`lg`**: 24dp (section dividers, card group gaps)
- **`xl`**: 32dp (major screen section margins)
- **`xxl`**: 48dp (empty state illustrations, bottom sheet clearance)

---

## 5. Shape System
Shapes follow a modern, rounded Material 3 scale to give the app a friendly, spacious, and highly polished aesthetic:
- **Extra Small (`6dp`)**: Micro tags, inline status indicators.
- **Small Controls (`8dp`)**: Filter chips, badge containers.
- **Medium Controls (`16dp`)**: Text input fields, selector fields, table cells, secondary buttons, list item cards.
- **Large Controls & Cards (`24dp`)**: Surface cards, financial summary cards, primary action containers.
- **Extra Large (`28dp`)**: Bottom sheets, modal dialogs, top sheet containers.
- **Full / Pill (`100dp`)**: Action buttons, search bars, pill chips.

---

## 6. Target Reusable Components Specification

When Step 3 implements the design system, it will build the following atomic Compose components:

1. **`AppScaffold`**: Root layout with integrated top app bar, status bar handling, and optional offline banner.
2. **`AppTopBar`**: Consistent title, back navigation arrow, and screen action icons.
3. **`AppPrimaryButton`**: Full-width or inline primary filled button (`#334E68`).
4. **`AppSecondaryButton`**: Outlined secondary action button for back/cancel actions.
5. **`AppTextField`**: Outlined text field with floating label, explicit error text slot, and clear focus indicator.
6. **`AppSearchField`**: Search input box with leading search icon and trailing clear button.
7. **`AppCard`**: Low-elevation (1dp) surface card with subtle `#BCCCDC` outline border.
8. **`AppSectionHeader`**: Clean section divider with bold title and optional action link (e.g., "+ Add Line Item").
9. **`AppEmptyState`**: Centered icon, title, description, and primary action button for zero-data views.
10. **`AppErrorState`**: Error message container with retry button.
11. **`AppLoadingState`**: Centered circular progress indicator.
12. **`AppOfflineBanner`**: Subtle top warning banner showing "Offline Mode - Changes will sync when online".
13. **`AppSyncIndicator`**: Small chip showing `SYNCED` (green check), `PENDING` (orange clock), or `FAILED` (red exclamation).
14. **`MoneyField`**: Custom numeric text field formatted for Indian currency (rupees/paise) with keyboard type `Decimal`.
15. **`QuantityField`**: Numeric field formatted for integer/unit quantities.

---

## 7. Screen Visual & Usability Rules

### Form Screens (Invoice & PO Entry)
- Form input fields must feature high-contrast labels and clear visual indicators for required fields.
- Numeric keyboards (`KeyboardType.Number` / `KeyboardType.Decimal`) MUST automatically display when focusing quantity or rate fields.
- Form fields must handle IME actions (`Next`, `Done`) smoothly to navigate between fields without hiding the keyboard unexpectedly.

### Financial Tables & Totals
- Numerical columns (Quantity, Rate, Amount, Taxable Value, GST, Total) MUST right-align in tables.
- Text descriptions (Item Name, HSN/SAC) left-align.
- Grand totals must be visually emphasized using bold typography and a subtle background highlight container (`#D9E2EC`).
- Currency formatting must conform to Indian numbering format (e.g., `₹ 1,50,000.00`).

### In-App PDF Viewer
- Viewport must use a dark/neutral backdrop (`#202B36`) with the white A4 document centered.
- Pinch-to-zoom and pan gestures must not lock or conflict with vertical scrolling.
- Share and Download action buttons must remain pinned in the top app bar or bottom bar.

---

## 8. Brand & Logo Direction
- **Brand Name**: Vivaan Enterprise
- **Concept**: Minimalist geometric "VE" monogram integrated with a stylized document/tax invoice motif.
- **Palette**: Navy Blue (`#334E68`) and Steel Gray (`#486581`) on clean white background.
- **Guidelines**: No gradients, no cartoon graphics, no 3D reflections. Must remain legible at 48x48dp launcher icon sizes.
