# Frontend-Backend Wiring — Context

## Summary

Wire all remaining frontend pages from mock data to real backend REST APIs. The frontend already has a working API client (`apiClient.ts`), auth hook (`useAuthenticatedApi`), and Vite proxy to `localhost:8080`. Members, Auth, and Profile pages are already integrated. ~50+ endpoints remain.

## Pages to Wire

| Page | Backend Endpoints | Priority |
|------|-------------------|----------|
| Dashboard | `/api/v1/dashboard/treasurer`, `/api/v1/dashboard/admin`, analytics | High |
| Contributions | `/api/v1/contributions`, categories, member summaries | High |
| Loans | `/api/v1/loans`, repayments, schedules, interest | High |
| Payouts | `/api/v1/payouts`, approve, process | High |
| Ledger | `/api/v1/ledger/accounts`, journal entries, trial balance | Medium |
| Audit Trail | `/api/v1/audit` with filtering | Medium |
| Reports | `/api/v1/reports/financial-summary`, member statements | Medium |
| Settings/Config | `/api/v1/config/system`, loan products, schedules, penalties | Medium |

## Architecture Decisions

- **Service per domain**: `contributionService.ts`, `loanService.ts`, `payoutService.ts`, etc.
- **Reuse existing infra**: `apiClient.ts` + `useAuthenticatedApi` hook
- **Cursor pagination**: All list views use `CursorPage<T>` pattern from Members
- **TypeScript types**: Co-located with service files, matching backend DTOs
- **Local state**: `useState`/`useEffect`, no global state library
- **Error handling**: `ApiError` class + loading/error UI per page
- **No new dependencies**: Pure fetch, no axios/react-query

## Constraints

- Backend on `localhost:8080`, Vite proxy on `/api`
- All responses use `ApiResponse<T>` envelope
- JWT Bearer auth with auto-refresh
- Role-based access (ADMIN, TREASURER, MEMBER)
