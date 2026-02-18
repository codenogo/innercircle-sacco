import { Routes, Route, Navigate } from 'react-router-dom'
import { AppShell } from './layouts/AppShell'
import { AuthLayout } from './layouts/AuthLayout'
import { Dashboard } from './pages/Dashboard'
import { Members } from './pages/Members'
import { Contributions } from './pages/Contributions'
import { Loans } from './pages/Loans'
import { Payouts } from './pages/Payouts'
import { Ledger } from './pages/Ledger'
import { Reports } from './pages/Reports'
import { Settings } from './pages/Settings'
import { Operations } from './pages/Operations'
import { UsersAdmin } from './pages/UsersAdmin'
import { MemberProfile } from './pages/MemberProfile'
import { ContributionCategories } from './pages/ContributionCategories'
import { ContributionOperations } from './pages/ContributionOperations'
import { LoanWorkflow } from './pages/LoanWorkflow'
import { LoanBatch } from './pages/LoanBatch'
import { LoanBenefits } from './pages/LoanBenefits'
import { PayoutOperations } from './pages/PayoutOperations'
import { LedgerStatements } from './pages/LedgerStatements'
import { ExportCenter } from './pages/ExportCenter'
import { RoleDashboards } from './pages/RoleDashboards'
import { SystemConfiguration } from './pages/SystemConfiguration'
import { AuditTrail } from './pages/AuditTrail'
import { MyProfile } from './pages/MyProfile'
import { Login } from './pages/Login'
import { Signup } from './pages/Signup'
import { ForgotPassword } from './pages/ForgotPassword'
import { ResetPassword } from './pages/ResetPassword'
import { RequireAuth } from './components/RequireAuth'
import { RequireRole } from './components/RequireRole'

export function App() {
  return (
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
            <RequireRole allowed={['ADMIN', 'TREASURER']}>
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
            <RequireRole allowed={['ADMIN']}>
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
  )
}
