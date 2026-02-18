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
| `--ink-muted` | `#7d7f92` | Metadata, labels |
| `--ink-faint` | `#a8aab8` | Placeholders, disabled |

### Paper — warm cream, ledger page
| Token | Value | Use |
|-------|-------|-----|
| `--paper` | `#faf7f1` | Base surface, sidebar, cards |
| `--paper-alt` | `#f5f2ec` | Alternating table rows |
| `--paper-inset` | `#f0ede7` | Input backgrounds, recessed areas |

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

## Depth Strategy: Borders Only

No shadows. Ledger books are flat. Ruled lines define structure.

- Borders use `--rule` tokens (faded blue rgba), not solid hex colors
- Horizontal rules (`<hr class="rule">`) are the primary structural element
- Three intensities: `--rule-subtle` (whisper), `--rule` (default), `--rule-strong` (section breaks)

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
| `--radius-sm` | 2px | Inputs, buttons, avatars |
| `--radius-md` | 3px | Cards, auth card |
| `--radius-lg` | 4px | Modals, largest containers |

---

## Signature Elements

These make InnerCircle feel like a ledger, not a template:

1. **Ruled lines** (`<hr class="rule">`) — faded blue horizontal rules as the primary structural element, replacing shadows and card borders
2. **Dot leaders** (`.dot-leader`) — dotted line connecting label to value (label ........... amount), like handwritten ledger entries
3. **Alternating cream rows** (`.ledger-row--alt` / `--paper-alt`) — table rows alternate between `--paper` and `--paper-alt`, like ruled ledger paper
4. **Accounting parentheses** — outflows shown as `(50,000)` not `-50,000`
5. **KES prefix in muted small caps** — currency code as quiet context, amount as the focus
6. **Active nav tab** — 2px green left border on active sidebar item, like a ledger book tab

---

## Component Patterns

### Sidebar
- Same `--paper` background as main content (not a different color)
- Separated from content by a single `--rule` border
- Nav links: `--text-sm`, `--ink-secondary`, icon + label
- Active state: `--ink` text, `--rule-subtle` background, 2px `--mpesa` left border
- User info at bottom: avatar with initials, name, role in uppercase `--text-xs`

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
- Account filter dropdown (All Accounts, Member Savings, Loan Receivable, Cash at Bank, Interest Revenue)
- Journal table with 7 columns: Ref, Date, Description, Account, Debit, Credit, Balance
- Running balance column in bold (`font-weight: 600`)
- **Double-ruled totals row** — `border-top: 3px double var(--rule-strong)` — accounting convention
- This page is wider (`max-width: 880px`) to fit the debit/credit/balance columns

### Reports
- Quick stats grid: 4 columns showing key metrics (label uppercase `--text-xs`, value `--text-md` bold mono)
- Report cards: bordered containers with icon + title + description, PDF/CSV export buttons on the right
- Card hover: `--rule-strong` border + `--rule-subtle` background
- 6 reports: Financial Summary, Contribution Report, Loan Portfolio, Member Statement, Member Register, Trial Balance

### Settings
- Grouped sections with `.settings-group-title` (serif `--text-md` weight 600)
- Key-value rows: `.settings-row` with label + optional description (left), value (right, mono for data)
- Rows separated by `--rule-subtle` bottom border
- Sections: Profile, Contribution Schedule, Loan Products, Penalties, System
- Penalty values shown in `--deficit` color
- Role shown as status badge

### Auth Pages (Login, Signup, Forgot/Reset Password)
- `AuthLayout`: centered on `--paper-alt` background
- Auth card: `--paper` background, `--rule` border, `--radius-md`
- IC monogram + brand name at top, `rule--strong` separator
- Inputs: `--paper-inset` background, `--rule` border, `--rule-strong` on hover/focus
- Submit button: `--mpesa` background, white text, weight 600
- Footer links: `--rule-subtle` top border, `--ink-muted` text, `--mpesa-text` links
- Success states: green circle icon, heading, muted description
- Labels: uppercase, `--text-xs`, `--ink-muted`

### Form Fields
- Label above input, `--space-1` gap
- Input: `--paper-inset` bg, 1px `--rule` border, `--radius-sm`
- Focus: border becomes `--rule-strong`, background becomes `--paper`
- Hint text: `--text-xs`, `--ink-faint`
- Error text: `--text-xs`, `--deficit`
- Side-by-side fields: `.field-row` grid, 2 columns, `--space-3` gap

---

## Transitions

`--ease: 150ms ease-out` for all micro-interactions. No bounce. No spring. Quiet and instant.

---

## Files

| File | Purpose |
|------|---------|
| `src/styles/tokens.css` | All design tokens |
| `src/styles/reset.css` | CSS reset |
| `src/styles/global.css` | Global styles, utilities, signature elements |
| `src/styles/auth.css` | Shared auth form styles |
| `src/layouts/AppShell.tsx/.css` | Main app layout with sidebar |
| `src/layouts/AuthLayout.tsx/.css` | Centered auth page layout |
| `src/components/Sidebar.tsx/.css` | Navigation sidebar |
| `src/pages/Dashboard.tsx/.css` | Ledger-style dashboard |
| `src/pages/Login.tsx` | Sign in |
| `src/pages/Signup.tsx` | Create account |
| `src/pages/ForgotPassword.tsx` | Request password reset |
| `src/pages/ResetPassword.tsx` | Set new password |
