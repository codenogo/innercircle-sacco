# InnerCircle SACCO — Design System

## Direction: "The Ledger"

Grounded in the physical world of Kenyan financial record-keeping. Fountain pen ink on warm cream paper, faded blue ruled lines, M-Pesa green as the sole accent. Every screen should feel like opening a well-kept accounts book.

**Who:** SACCO treasurers recording contributions at weekend meetings, admins approving loans during the week, members checking balances on the go. Community money, managed with care.

**What they do:** Record contributions, disburse loans, check balances, prepare reports. Every shilling tracked.

**Feel:** Precise like a well-kept ledger. Trustworthy. Dense but readable. Not cold — this is a community's money. Not playful — this is serious record-keeping.

---

## Palette

### Ink — deep blue-black, fountain pen on paper
| Token | Value | Use |
|-------|-------|-----|
| `--ink` | `#1e2030` | Primary text |
| `--ink-secondary` | `#4d4f62` | Supporting text |
| `--ink-muted` | `#6b6d80` | Metadata, labels |
| `--ink-faint` | `#a8aab8` | Placeholders, disabled |

### Paper — warm cream, ledger page
| Token | Value | Use |
|-------|-------|-----|
| `--paper` | `#f5f0e5` | Base surface, main content |
| `--paper-alt` | `#ede7db` | Sidebar background, alternating table rows |
| `--paper-inset` | `#e5dfd3` | Input backgrounds, recessed areas |

### Rule — faded blue, printed lines in an accounts book
| Token | Value | Use |
|-------|-------|-----|
| `--rule` | `rgba(90, 110, 150, 0.12)` | Default borders, table lines |
| `--rule-strong` | `rgba(90, 110, 150, 0.22)` | Section separators, emphasis |
| `--rule-subtle` | `rgba(90, 110, 150, 0.06)` | Hover backgrounds, whisper borders |

### M-Pesa — sole accent, Kenyan financial green
| Token | Value | Use |
|-------|-------|-----|
| `--mpesa` | `#3a7d44` | Buttons, active indicators, positive amounts |
| `--mpesa-hover` | `#2d6435` | Button hover state |
| `--mpesa-soft` | `rgba(58, 125, 68, 0.10)` | Background tint, selection |
| `--mpesa-text` | `#2d6435` | Green text (income, success) |

### Semantic
| Token | Value | Use |
|-------|-------|-----|
| `--deficit` | `#b94a3d` | Overdue, negative amounts, errors |
| `--deficit-soft` | `rgba(185, 74, 61, 0.08)` | Error background tint |
| `--caution` | `#c08a15` | Warnings, pending states |
| `--caution-soft` | `rgba(192, 138, 21, 0.08)` | Warning background tint |
| `--info` | `#4a6fa5` | Informational states |
| `--info-soft` | `rgba(74, 111, 165, 0.08)` | Info background tint |

### Interactive
| Token | Value | Use |
|-------|-------|-----|
| `--hover` | `rgba(90, 110, 150, 0.08)` | Row hover, subtle interactive feedback |
| `--backdrop` | `rgba(45, 40, 32, 0.40)` | Modal/overlay backdrop |
| `--backdrop-light` | `rgba(30, 32, 48, 0.25)` | Mobile sidebar backdrop |
| `--text-on-accent` | `#fff` | White text on `--mpesa` backgrounds |

---

## Typography

Three faces, three jobs:

| Token | Family | Why |
|-------|--------|-----|
| `--font-heading` | Source Serif 4 | Authority of financial documents. Says "this is an institution." |
| `--font-body` | DM Sans | Warm precision. Clean UI text that doesn't feel clinical. |
| `--font-data` | JetBrains Mono | Every shilling clearly readable. `tabular-nums` for column alignment. |

### Type Scale
| Token | Size | Use |
|-------|------|-----|
| `--text-xs` | 0.75rem (12px) | Metadata, fine print, field hints |
| `--text-sm` | 0.8125rem (13px) | Labels, table headers, nav links |
| `--text-base` | 0.875rem (14px) | Body text, table data |
| `--text-md` | 1rem (16px) | Section headings |
| `--text-lg` | 1.25rem (20px) | Page titles |
| `--text-xl` | 1.5rem (24px) | Hero numbers |

### Hierarchy Rules
- Headings: `--font-heading`, weight 600, `--tracking-tight`
- Labels: `--font-body`, weight 500, `--tracking-wide`, uppercase, `--ink-muted`
- Data/amounts: `--font-data`, `tabular-nums`, weight 500
- Negative amounts in parentheses — accounting convention

---

## Depth Strategy: Borders + Pragmatic Shadows

Ruled lines are the primary structural element — most surfaces are flat, separated by `--rule` borders. Shadows are reserved for elements that genuinely float above the page: modals, dropdown panels, and interactive hover feedback.

### Borders
- Use `--rule` tokens (faded blue rgba), not solid hex colors
- Horizontal rules (`<hr class="rule">`) are the primary structural element
- Three intensities: `--rule-subtle` (whisper), `--rule` (default), `--rule-strong` (section breaks)

### Shadows — for floating elements only
| Token | Value | Use |
|-------|-------|-----|
| `--shadow-sm` | `0 1px 2px rgba(90, 80, 60, 0.06), 0 1px 3px rgba(90, 80, 60, 0.04)` | Button hover lift, subtle card elevation |
| `--shadow-md` | `0 2px 4px rgba(90, 80, 60, 0.07), 0 4px 8px rgba(90, 80, 60, 0.05)` | Auth card, elevated panels |
| `--shadow-lg` | `0 4px 8px rgba(90, 80, 60, 0.08), 0 8px 16px rgba(90, 80, 60, 0.06)` | Modals, dropdown panels, popovers |
| `--shadow-xl` | `0 8px 16px rgba(90, 80, 60, 0.10), 0 16px 32px rgba(90, 80, 60, 0.08)` | Reserved for maximum elevation |

Shadow colors use warm `rgba(90, 80, 60, ...)` — sepia-toned to match the paper palette. Never blue/gray shadows.

### Inset Shadows — form input depth
| Token | Value | Use |
|-------|-------|-----|
| `--inset-shadow` | `inset 0 1px 2px rgba(30, 32, 48, 0.06)` | Default input resting state |
| `--inset-shadow-hover` | `inset 0 1px 2px rgba(30, 32, 48, 0.04)` | Input hover (slightly lighter) |

---

## Spacing

8px base, 4px micro. Tighter than typical — financial tools are dense.

| Token | Value | Use |
|-------|-------|-----|
| `--space-1` | 4px | Micro gaps, icon-to-text |
| `--space-2` | 8px | Tight padding, table cells |
| `--space-3` | 12px | Nav item padding, field gaps |
| `--space-4` | 16px | Card padding, section margins |
| `--space-5` | 24px | Major gaps between groups |
| `--space-6` | 32px | Page padding, large sections |
| `--space-7` | 48px | Main content horizontal padding |
| `--space-8` | 64px | Maximum spacing (rare) |

---

## Border Radius

Barely there. Accounts books have sharp corners.

| Token | Value | Use |
|-------|-------|-----|
| `--radius-sm` | 2px | Inputs, buttons, avatars, scrollbar thumbs |
| `--radius-md` | 3px | Cards, auth card, dropdown panels |
| `--radius-lg` | 4px | Modals, largest containers |

---

## Transitions

| Token | Value | Use |
|-------|-------|-----|
| `--ease` | `150ms ease-out` | Default micro-interactions |
| `--ease-fast` | `100ms ease-out` | Button backgrounds, badge color |
| `--ease-medium` | `250ms ease-out` | Shadows, transforms, layout shifts |
| `--ease-slow` | `400ms ease-out` | Page-level transitions (rare) |

No bounce. No spring. Quiet and instant.

---

## Signature Elements

These make InnerCircle feel like a ledger, not a template:

1. **Ruled lines** (`<hr class="rule">`) — faded blue horizontal rules as the primary structural element, replacing card borders where possible
2. **Dot leaders** (`.dot-leader`) — dotted line connecting label to value (label ........... amount), like handwritten ledger entries
3. **Alternating cream rows** (`.ledger-row--alt` / `--paper-alt`) — table rows alternate between `--paper` and `--paper-alt`, like ruled ledger paper
4. **Accounting parentheses** — outflows shown as `(50,000)` not `-50,000`
5. **KES prefix in muted small caps** — currency code as quiet context, amount as the focus
6. **Active nav tab** — 3px green left border on active sidebar item, like a ledger book tab
7. **Warm sepia shadows** — when shadows are used, they use warm `rgba(90, 80, 60, ...)` tones, not neutral grays

---

## Component Patterns

### Sidebar
- `--paper-alt` background (slightly warmer than main content)
- Separated from content by a single `--rule` border
- Nav links: `--text-sm`, `--ink-secondary`, icon + label
- Active state: `--ink` text, `--mpesa-soft` gradient background, 3px `--mpesa` left border with `--radius-sm` rounding
- User info at bottom: avatar with initials, name, role in uppercase `--text-xs`
- Mobile: slides in from left with `--backdrop-light` overlay

### Dashboard
- Page header: serif title left, italic date/period right, separated by `rule--strong`
- Sections titled with `.label` (uppercase small caps), followed by `rule`
- Fund summary uses dot leaders
- Tables use `ledger-table` pattern: ruled lines, alternating rows, right-aligned amounts

### Page Layout Pattern
Every app page follows this structure:
1. `.page-header` — flex row: title (left) + action button (right)
2. `.page-subtitle` — italic muted text under title
3. `hr.rule.rule--strong` — heavy separator after header
4. `.page-section` — titled block with `.page-section-title` (uppercase small caps) + `hr.rule`
5. `.page-summary` — inline stats row: `Active: **24** | Pending: **2** | Total Shares: **KES 291,000**`
6. Content (table, dot leaders, cards)
7. `hr.rule.rule--strong` — closing rule at bottom

### Members
- Search input with icon (`.filter-search-wrap`) + status filter dropdown
- Table columns: Name (with email subtitle), Phone (mono), Status (badge), Joined, Shares (right-aligned mono)
- Member name as primary text, email as `--text-xs --ink-muted` below
- Summary: active count, pending count, total shares

### Contributions
- Month selector input + collection rate percentage display
- Dot-leader summary: Expected, Collected (green), Outstanding (red) with ruled total line
- Table: Member, Category, Date, Status badge, Amount
- Status values: `PAID` (green), `PARTIAL` (amber), `PENDING` (amber), `LATE` (red)

### Loans
- Portfolio summary section with dot leaders: Disbursed, Repaid (green), Outstanding (bold total)
- Table columns: Loan ID (muted mono), Member (with date subtitle), Rate%, Status, Guarantors, Principal, Balance
- Status: `ACTIVE` (green), `PENDING` (amber), `COMPLETED` (green), `DEFAULTED` (red)
- Defaulted loan balances shown in `--deficit` color

### Payouts
- Monthly summary: Total Paid Out, Pending Approval (caution color)
- Table: ID (muted mono), Member (with date subtitle), Type, Status badge, Reference (mono), Amount
- Payout types: Bank Transfer, M-Pesa, Cash, Share Withdrawal
- Status: `COMPLETED` (green), `PENDING` (amber), `PROCESSING` (amber), `REJECTED` (red)

### General Ledger
- TanStack Table with virtual scrolling for millions of rows
- Column-level search filters: each column header has an inline text/select filter
- Expandable journal entries: click a row to reveal all lines of the multi-line journal entry in a sub-table
- Infinite scroll with "Load More" button at the bottom
- 7 columns: Ref, Date, Description, Account, Debit, Credit, Balance
- Running balance column in bold (`font-weight: 600`)
- **Double-ruled totals row** — `border-top: 3px double var(--rule-strong)` — accounting convention
- Uses `--mpesa` for focus rings on filter inputs (not generic blue accent)

### Reports
- Quick stats grid: 4 columns showing key metrics (label uppercase `--text-xs`, value `--text-md` bold mono)
- Report cards: bordered containers with icon + title + description, PDF/CSV export buttons on the right
- Card hover: `--rule-strong` border + `--rule-subtle` background + `--shadow-sm`
- 6 reports: Financial Summary, Contribution Report, Loan Portfolio, Member Statement, Member Register, Trial Balance
- Member selection modal uses `--backdrop` overlay

### Settings
- Grouped sections with `.settings-group-title` (serif `--text-md` weight 600)
- Key-value rows: `.settings-row` with label + optional description (left), value (right, mono for data)
- Rows separated by `--rule-subtle` bottom border
- Sections: Profile, Contribution Schedule, Loan Products, Penalties, System
- Penalty values shown in `--deficit` color
- Role shown as status badge

### Auth Pages (Login, Signup, Forgot/Reset Password)
- `AuthLayout`: centered on `--paper-alt` background
- Auth card: `--paper` background, `--rule` border, `--radius-md`, `--shadow-md`
- IC monogram + brand name at top, `rule--strong` separator
- Inputs: `--paper-inset` background, `--rule` border, `var(--inset-shadow)` resting state
- Input hover: `--rule-strong` border, `var(--inset-shadow-hover)`
- Input focus: `--mpesa` border, `--paper` background, focus ring `0 0 0 3px var(--mpesa-soft)`
- Submit button: `--mpesa` background, `--text-on-accent` text, weight 600
- Footer links: `--rule-subtle` top border, `--ink-muted` text, `--mpesa-text` links
- Success states: green circle icon, heading, muted description
- Labels: uppercase, `--text-xs`, `--ink-muted`

### Form Fields
- Label above input, `--space-1` gap
- Input: `--paper-inset` bg, 1px `--rule` border, `--radius-sm`, `var(--inset-shadow)`
- Hover: `--rule-strong` border, `var(--inset-shadow-hover)`
- Focus: `--mpesa` border, `--paper` background, `0 0 0 3px var(--mpesa-soft)` ring
- Hint text: `--text-xs`, `--ink-faint`
- Error text: `--text-xs`, `--deficit`
- Side-by-side fields: `.field-row` grid, 2 columns, `--space-3` gap

### Modals
- `--backdrop` overlay with `backdrop-filter: blur(2px)`
- Card: `--paper` background, `--rule` border, `--radius-md`, `--shadow-lg`
- Three sizes: `--sm` (380px), `--md` (480px), `--lg` (560px)
- Mobile: bottom-sheet style, full width, rounded top corners only
- Slide-up animation with subtle spring (`250ms cubic-bezier(0.34, 1.56, 0.64, 1)`)

### Custom Select
- Trigger: `--paper-inset` background, `--rule` border, `var(--inset-shadow)`
- Open state: `--mpesa` border, `--paper` background, focus ring
- Panel: `--paper` background, `--rule` border, `--radius-md`, `--shadow-lg`
- Options: `--rule-subtle` hover background, `--mpesa-text` selected text
- Searchable with inline search input
- Filter variant (`.filter-select-wrap`): compact size for use in filter bars

### Status Badges
- Inline-flex with icon gap, uppercase `--text-xs`, `--radius-sm`
- Active/Paid/Approved: `--mpesa-text` on `--mpesa-soft`
- Pending/Partial: `--caution` on `--caution-soft`
- Overdue/Rejected/Defaulted: `--deficit` on `--deficit-soft`
- Inactive: `--ink-muted` on `--paper-inset`
- Borders at 0.15 opacity of the semantic color base

---

## Files

| File | Purpose |
|------|---------|
| `src/styles/tokens.css` | All design tokens |
| `src/styles/reset.css` | CSS reset |
| `src/styles/global.css` | Global styles, utilities, signature elements |
| `src/styles/components.css` | Shared component patterns (buttons, badges, filter bars, tables, empty states) |
| `src/styles/skeleton.css` | Loading skeleton animation |
| `src/styles/spinner.css` | Loading spinner |
| `src/styles/progress.css` | Progress bar |
| `src/styles/toast.css` | Toast notifications |
| `src/styles/alert.css` | Alert banners (success, error, warning, info) |
| `src/styles/tooltip.css` | Tooltip styles |
| `src/styles/auth.css` | Shared auth form styles |
| `src/layouts/AppShell.tsx/.css` | Main app layout with sidebar |
| `src/layouts/AuthLayout.tsx/.css` | Centered auth page layout |
| `src/components/Sidebar.tsx/.css` | Navigation sidebar |
| `src/components/Modal.tsx/.css` | Dialog modals |
| `src/components/Select.tsx/.css` | Custom dropdown select |
| `src/components/DatePicker.tsx/.css` | Calendar date picker |
| `src/components/MonthPicker.tsx/.css` | Month/year selector |
| `src/components/MakerCheckerWarning.css` | Dual-authorization warning |
| `src/pages/Dashboard.tsx/.css` | Ledger-style dashboard |
| `src/pages/Ledger.tsx/.css` | General ledger (TanStack Table + virtual scroll) |
| `src/pages/Login.tsx` | Sign in |
| `src/pages/Signup.tsx` | Create account |
| `src/pages/ForgotPassword.tsx` | Request password reset |
| `src/pages/ResetPassword.tsx` | Set new password |
