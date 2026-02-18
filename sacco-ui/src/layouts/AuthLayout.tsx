import { Outlet } from 'react-router-dom'
import './AuthLayout.css'

export function AuthLayout() {
  return (
    <div className="auth-layout">
      <div className="auth-card">
        <div className="auth-brand">
          <span className="auth-monogram">IC</span>
          <h1 className="auth-brand-name">InnerCircle</h1>
          <p className="auth-brand-tagline">SACCO Management</p>
        </div>
        <hr className="rule rule--strong" />
        <Outlet />
      </div>
    </div>
  )
}
