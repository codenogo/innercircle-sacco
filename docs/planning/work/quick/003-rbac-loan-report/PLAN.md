# Quick: Add @PreAuthorize RBAC to LoanController and ReportController (W1 gap from auth-hardening review)

## Goal
Add @PreAuthorize RBAC to LoanController and ReportController (W1 gap from auth-hardening review)

## Files
- `sacco-loan/src/main/java/com/innercircle/sacco/loan/controller/LoanController.java`
- `sacco-reporting/src/main/java/com/innercircle/sacco/reporting/controller/ReportController.java`

## Approach
[Brief description]

## Verify
```bash
mvn -pl sacco-loan test -q
mvn -pl sacco-reporting test -q
```
