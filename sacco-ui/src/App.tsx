import { lazy, Suspense } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { AppShell } from './layouts/AppShell'
import { AuthLayout } from './layouts/AuthLayout'
import { RequireAuth } from './components/RequireAuth'
import { RequireRole } from './components/RequireRole'
import { ErrorBoundary } from './components/ErrorBoundary'
import { SkeletonRow, SkeletonStat } from './components/Skeleton'

/* ─── Lazy-loaded pages — each becomes its own chunk ─── */
const Dashboard = lazy(() => import('./pages/Dashboard').then(m => ({ default: m.Dashboard })))
const Members = lazy(() => import('./pages/Members').then(m => ({ default: m.Members })))
const MemberProfile = lazy(() => import('./pages/MemberProfile').then(m => ({ default: m.MemberProfile })))
const Contributions = lazy(() => import('./pages/Contributions').then(m => ({ default: m.Contributions })))
const Loans = lazy(() => import('./pages/Loans').then(m => ({ default: m.Loans })))
const Payouts = lazy(() => import('./pages/Payouts').then(m => ({ default: m.Payouts })))
const PettyCash = lazy(() => import('./pages/PettyCash').then(m => ({ default: m.PettyCash })))
const Ledger = lazy(() => import('./pages/Ledger').then(m => ({ default: m.Ledger })))
const Reports = lazy(() => import('./pages/Reports').then(m => ({ default: m.Reports })))
const Settings = lazy(() => import('./pages/Settings').then(m => ({ default: m.Settings })))
const Operations = lazy(() => import('./pages/Operations').then(m => ({ default: m.Operations })))
const UsersAdmin = lazy(() => import('./pages/UsersAdmin').then(m => ({ default: m.UsersAdmin })))
const ContributionCategories = lazy(() => import('./pages/ContributionCategories').then(m => ({ default: m.ContributionCategories })))
const ContributionOperations = lazy(() => import('./pages/ContributionOperations').then(m => ({ default: m.ContributionOperations })))
const LoanWorkflow = lazy(() => import('./pages/LoanWorkflow').then(m => ({ default: m.LoanWorkflow })))
const LoanBatch = lazy(() => import('./pages/LoanBatch').then(m => ({ default: m.LoanBatch })))
const LoanBenefits = lazy(() => import('./pages/LoanBenefits').then(m => ({ default: m.LoanBenefits })))
const PayoutOperations = lazy(() => import('./pages/PayoutOperations').then(m => ({ default: m.PayoutOperations })))
const LedgerStatements = lazy(() => import('./pages/LedgerStatements').then(m => ({ default: m.LedgerStatements })))
const ExportCenter = lazy(() => import('./pages/ExportCenter').then(m => ({ default: m.ExportCenter })))
const RoleDashboards = lazy(() => import('./pages/RoleDashboards').then(m => ({ default: m.RoleDashboards })))
const SystemConfiguration = lazy(() => import('./pages/SystemConfiguration').then(m => ({ default: m.SystemConfiguration })))
const AuditTrail = lazy(() => import('./pages/AuditTrail').then(m => ({ default: m.AuditTrail })))
const MyProfile = lazy(() => import('./pages/MyProfile').then(m => ({ default: m.MyProfile })))
const Login = lazy(() => import('./pages/Login').then(m => ({ default: m.Login })))
const Signup = lazy(() => import('./pages/Signup').then(m => ({ default: m.Signup })))
const ForgotPassword = lazy(() => import('./pages/ForgotPassword').then(m => ({ default: m.ForgotPassword })))
const Investments = lazy(() => import('./pages/Investments').then(m => ({ default: m.Investments })))
const InvestmentOperations = lazy(() => import('./pages/InvestmentOperations').then(m => ({ default: m.InvestmentOperations })))
const InvestmentDetail = lazy(() => import('./pages/InvestmentDetail').then(m => ({ default: m.InvestmentDetail })))
const MeetingsAndFines = lazy(() => import('./pages/MeetingsAndFines').then(m => ({ default: m.MeetingsAndFines })))
const WelfareClaims = lazy(() => import('./pages/WelfareClaims').then(m => ({ default: m.WelfareClaims })))
const MemberExitWorkflow = lazy(() => import('./pages/MemberExitWorkflow').then(m => ({ default: m.MemberExitWorkflow })))
const ResetPassword = lazy(() => import('./pages/ResetPassword').then(m => ({ default: m.ResetPassword })))

function PageFallback() {
  return (
    <div className="ops-page" style={{ padding: 'var(--space-5)' }}>
      <SkeletonStat />
      <SkeletonRow cells={4} />
      <SkeletonRow cells={4} />
    </div>
  )
}

export function App() {
  return (
    <ErrorBoundary>
      <Suspense fallback={<PageFallback />}>
      <Routes>
        {/* Auth pages — outside the app shell */}
        <Route element={<AuthLayout />}>
          <Route path="login" element={<Login />} />
          <Route path="signup" element={<Signup />} />
          <Route path="forgot-password" element={<ForgotPassword />} />
          <Route path="reset-password" element={<ResetPassword />} />
        </Route>

        {/* App pages — inside the shell with sidebar */}
        <Route element={<RequireAuth><AppShell /></RequireAuth>}>
        <Route
          index
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER']}>
              <Dashboard />
            </RequireRole>
          )}
        />
        <Route path="members" element={<Members />} />
        <Route path="members/:id" element={<MemberProfile />} />
        <Route path="contributions" element={<Contributions />} />
        <Route path="loans" element={<Loans />} />
        <Route path="payouts" element={<Payouts />} />
        <Route
          path="petty-cash"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER']}>
              <PettyCash />
            </RequireRole>
          )}
        />
        <Route path="investments" element={<Investments />} />
        <Route
          path="investments/:id"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER']}>
              <InvestmentDetail />
            </RequireRole>
          )}
        />
        <Route
          path="investment-ops"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER']}>
              <InvestmentOperations />
            </RequireRole>
          )}
        />
        <Route
          path="meetings-fines"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER', 'SECRETARY', 'CHAIRPERSON', 'VICE_CHAIRPERSON', 'VICE_TREASURER']}>
              <MeetingsAndFines />
            </RequireRole>
          )}
        />
        <Route
          path="welfare-claims"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER', 'CHAIRPERSON', 'VICE_CHAIRPERSON', 'VICE_TREASURER']}>
              <WelfareClaims />
            </RequireRole>
          )}
        />
        <Route
          path="member-exit"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER', 'MEMBER', 'CHAIRPERSON', 'VICE_CHAIRPERSON', 'VICE_TREASURER']}>
              <MemberExitWorkflow />
            </RequireRole>
          )}
        />
        <Route
          path="ledger"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER']}>
              <Ledger />
            </RequireRole>
          )}
        />
        <Route
          path="reports"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER']}>
              <Reports />
            </RequireRole>
          )}
        />
        <Route path="settings" element={<Settings />} />
        <Route
          path="operations"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER', 'MEMBER', 'SECRETARY', 'CHAIRPERSON', 'VICE_CHAIRPERSON', 'VICE_TREASURER']}>
              <Operations />
            </RequireRole>
          )}
        />
        <Route
          path="users-admin"
          element={(
            <RequireRole allowed={['ADMIN']}>
              <UsersAdmin />
            </RequireRole>
          )}
        />
        <Route
          path="contribution-categories"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER']}>
              <ContributionCategories />
            </RequireRole>
          )}
        />
        <Route
          path="contribution-ops"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER']}>
              <ContributionOperations />
            </RequireRole>
          )}
        />
        <Route
          path="loan-workflow"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER']}>
              <LoanWorkflow />
            </RequireRole>
          )}
        />
        <Route
          path="loan-batch"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER']}>
              <LoanBatch />
            </RequireRole>
          )}
        />
        <Route
          path="loan-benefits"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER']}>
              <LoanBenefits />
            </RequireRole>
          )}
        />
        <Route
          path="payout-ops"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER']}>
              <PayoutOperations />
            </RequireRole>
          )}
        />
        <Route
          path="ledger-statements"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER']}>
              <LedgerStatements />
            </RequireRole>
          )}
        />
        <Route
          path="export-center"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER']}>
              <ExportCenter />
            </RequireRole>
          )}
        />
        <Route
          path="role-dashboards"
          element={(
            <RequireRole allowed={['ADMIN', 'TREASURER']}>
              <RoleDashboards />
            </RequireRole>
          )}
        />
        <Route
          path="system-config"
          element={(
            <RequireRole allowed={['ADMIN']}>
              <SystemConfiguration />
            </RequireRole>
          )}
        />
        <Route
          path="audit-trail"
          element={(
            <RequireRole allowed={['ADMIN']}>
              <AuditTrail />
            </RequireRole>
          )}
        />
        <Route path="profile" element={<MyProfile />} />
      </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      </Suspense>
    </ErrorBoundary>
  )
}
