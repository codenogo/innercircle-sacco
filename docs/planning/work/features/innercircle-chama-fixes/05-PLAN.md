# Plan 05: CSV Injection & PDF Fix

## Goal
Fix CSV injection vulnerability in export and fix PDF page break that truncates output.

## Prerequisites
- [ ] Plan 04 complete (ExportController authorization must be in place)

## Tasks

### Task 1: Fix CSV injection in escapeCsv() (B7)
**Files:** `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/service/ExportServiceImpl.java`
**Action:**
Fix `escapeCsv()` method (lines 167-173) to prevent formula injection per OWASP guidance. Before checking for commas/quotes, prefix formula trigger characters with a single quote:
```java
private String escapeCsv(String value) {
    if (value == null) return "";
    // OWASP: prefix formula characters to prevent CSV injection
    if (!value.isEmpty()) {
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@'
                || first == '\t' || first == '\r') {
            value = "'" + value;
        }
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
}
```

**Verify:**
```bash
mvn compile -pl sacco-reporting -q
```

**Done when:** `escapeCsv()` prefixes formula characters (`=`, `+`, `-`, `@`, `\t`, `\r`) with single quote.

### Task 2: Fix PDF page break (W17)
**Files:** `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/service/ExportServiceImpl.java`
**Action:**
Fix `memberStatementToPdf()` (lines 101-108) to properly handle page breaks instead of using `break`:
```java
// Replace the page break block (lines 101-108)
PDPageContentStream cs = new PDPageContentStream(document, page);
float y = 770;
// ... existing header code ...

for (MemberStatementEntry entry : statement.entries()) {
    if (y < 80) {
        cs.close();
        page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        cs = new PDPageContentStream(document, page);
        cs.setFont(fontRegular, 7);
        y = 770;
    }
    // ... existing row rendering ...
}
```
The key change: instead of `break` (which stops rendering), open a new `PDPageContentStream` on the new page, reset `y`, and continue the loop. The `cs` variable must be declared outside the try-with-resources so it can be reassigned. Close `cs` manually at the end.

**Verify:**
```bash
mvn compile -pl sacco-reporting -q
```

**Done when:** PDF rendering continues on new pages instead of stopping at page boundary.

## Verification

After all tasks:
```bash
mvn compile -q
```

## Commit Message
```
fix(reporting): prevent CSV injection and fix PDF multi-page rendering

- B7: Prefix formula characters with single quote in escapeCsv()
- W17: Continue PDF rendering on new pages instead of breaking at page boundary
```

---
*Planned: 2026-02-15*
