import { useEffect, useState } from 'react'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import type { MeResponse } from '../types/auth'
import type {
  ContributionScheduleConfigResponse,
  LoanProductConfigResponse,
  PenaltyRuleResponse,
  SystemConfigResponse,
} from '../types/config'
import './Settings.css'

function fmtCurrency(value: number | string): string {
  const parsed = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(parsed)) return '0'
  return parsed.toLocaleString('en-KE')
}

function fmtRate(rate: number, method: string): string {
  if (method === 'PERCENTAGE') return `${rate}%`
  return `KES ${fmtCurrency(rate)}`
}

function configValue(configs: SystemConfigResponse[], key: string): string {
  const found = configs.find(c => c.configKey === key)
  return found?.configValue ?? '-'
}

export function Settings() {
  const { request } = useAuthenticatedApi()

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [profile, setProfile] = useState<MeResponse | null>(null)
  const [schedules, setSchedules] = useState<ContributionScheduleConfigResponse[]>([])
  const [loanProducts, setLoanProducts] = useState<LoanProductConfigResponse[]>([])
  const [penaltyRules, setPenaltyRules] = useState<PenaltyRuleResponse[]>([])
  const [systemConfigs, setSystemConfigs] = useState<SystemConfigResponse[]>([])

  useEffect(() => {
    let cancelled = false

    async function fetchAll() {
      setLoading(true)
      setError(null)
      try {
        const me = await request<MeResponse>('/api/v1/me')
        if (cancelled) return

        setProfile(me)

        if (!me.roles.includes('ADMIN')) {
          setLoanProducts([])
          setSchedules([])
          setPenaltyRules([])
          setSystemConfigs([])
          return
        }

        const [prods, scheds, penalties, configs] = await Promise.all([
          request<LoanProductConfigResponse[]>('/api/v1/config/loan-products'),
          request<ContributionScheduleConfigResponse[]>('/api/v1/config/contribution-schedules'),
          request<PenaltyRuleResponse[]>('/api/v1/config/penalty-rules'),
          request<SystemConfigResponse[]>('/api/v1/config/system'),
        ])
        if (cancelled) return

        setLoanProducts(prods)
        setSchedules(scheds)
        setPenaltyRules(penalties)
        setSystemConfigs(configs)
      } catch (err) {
        if (cancelled) return
        setError(err instanceof Error ? err.message : 'Failed to load settings')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void fetchAll()
    return () => { cancelled = true }
  }, [request])

  if (loading) {
    return (
      <div className="settings-page">
        <div className="page-header">
          <h1 className="page-title">Settings</h1>
        </div>
        <hr className="rule rule--strong" />
        <p className="settings-loading">Loading settings...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="settings-page">
        <div className="page-header">
          <h1 className="page-title">Settings</h1>
        </div>
        <hr className="rule rule--strong" />
        <div className="ops-feedback ops-feedback--error" role="status">{error}</div>
      </div>
    )
  }

  const displayName = profile?.member
    ? `${profile.member.firstName} ${profile.member.lastName}`.trim()
    : profile?.username ?? '-'
  const displayRole = profile?.roles?.length ? profile.roles.join(', ') : '-'
  const isAdmin = Boolean(profile?.roles?.includes('ADMIN'))

  return (
    <div className="settings-page">
      <div className="page-header">
        <h1 className="page-title">Settings</h1>
      </div>

      <hr className="rule rule--strong" />

      {/* Profile */}
      <div className="settings-group">
        <h2 className="settings-group-title">Profile</h2>
        <hr className="rule" />
        <div className="settings-row">
          <div>
            <span className="settings-row-label">Name</span>
            <span className="settings-row-desc">Your display name in the system</span>
          </div>
          <span className="settings-row-value">{displayName}</span>
        </div>
        <div className="settings-row">
          <div>
            <span className="settings-row-label">Email</span>
          </div>
          <span className="settings-row-value">{profile?.email ?? '-'}</span>
        </div>
        <div className="settings-row">
          <div>
            <span className="settings-row-label">Role</span>
          </div>
          <span className="badge badge--active">{displayRole}</span>
        </div>
      </div>

      {isAdmin ? (
        <>
          {/* Contribution Schedule */}
          <div className="settings-group">
            <h2 className="settings-group-title">Contribution Schedule</h2>
            <hr className="rule" />
            {schedules.length === 0 ? (
              <div className="settings-row">
                <span className="settings-row-label">No contribution schedules configured</span>
              </div>
            ) : schedules.map(schedule => (
              <div key={schedule.id}>
                <div className="settings-row">
                  <div>
                    <span className="settings-row-label">{schedule.name}</span>
                    <span className="settings-row-desc">
                      {schedule.frequency} &middot; Penalty {schedule.penaltyEnabled ? 'enabled' : 'disabled'}
                      {!schedule.active && ' \u00b7 Inactive'}
                    </span>
                  </div>
                  <span className="settings-row-value">KES {fmtCurrency(schedule.amount)}</span>
                </div>
              </div>
            ))}
          </div>

          {/* Loan Products */}
          <div className="settings-group">
            <h2 className="settings-group-title">Loan Products</h2>
            <hr className="rule" />
            {loanProducts.length === 0 ? (
              <div className="settings-row">
                <span className="settings-row-label">No loan products configured</span>
              </div>
            ) : loanProducts.map(product => (
              <div key={product.id} className="settings-row">
                <div>
                  <span className="settings-row-label">{product.name}</span>
                  <span className="settings-row-desc">
                    Max KES {fmtCurrency(product.maxAmount)} &middot; {product.maxTermMonths} months
                    &middot; {product.interestMethod.replace('_', ' ')}
                    {product.requiresGuarantor && ' \u00b7 Guarantor required'}
                    {!product.active && ' \u00b7 Inactive'}
                  </span>
                </div>
                <span className="settings-row-value">{product.annualInterestRate}% p.a.</span>
              </div>
            ))}
          </div>

          {/* Penalties */}
          <div className="settings-group">
            <h2 className="settings-group-title">Penalties</h2>
            <hr className="rule" />
            {penaltyRules.length === 0 ? (
              <div className="settings-row">
                <span className="settings-row-label">No penalty rules configured</span>
              </div>
            ) : penaltyRules.map(rule => (
              <div key={rule.id} className="settings-row">
                <div>
                  <span className="settings-row-label">{rule.name}</span>
                  <span className="settings-row-desc">
                    {rule.penaltyType.replace('_', ' ')}
                    {rule.compounding && ' \u00b7 Compounding'}
                    {!rule.active && ' \u00b7 Inactive'}
                  </span>
                </div>
                <span className="settings-row-value penalty-value">
                  {fmtRate(rule.rate, rule.calculationMethod)}
                </span>
              </div>
            ))}
          </div>

          {/* System */}
          <div className="settings-group">
            <h2 className="settings-group-title">System</h2>
            <hr className="rule" />
            {systemConfigs.length === 0 ? (
              <div className="settings-row">
                <span className="settings-row-label">No system configuration found</span>
              </div>
            ) : (
              <>
                <div className="settings-row">
                  <div>
                    <span className="settings-row-label">SACCO Name</span>
                  </div>
                  <span className="settings-row-value">{configValue(systemConfigs, 'sacco.name')}</span>
                </div>
                <div className="settings-row">
                  <div>
                    <span className="settings-row-label">Registration Number</span>
                  </div>
                  <span className="settings-row-value data">{configValue(systemConfigs, 'sacco.registration.number')}</span>
                </div>
                <div className="settings-row">
                  <div>
                    <span className="settings-row-label">Financial Year</span>
                  </div>
                  <span className="settings-row-value">{configValue(systemConfigs, 'sacco.financial.year')}</span>
                </div>
                <div className="settings-row">
                  <div>
                    <span className="settings-row-label">Currency</span>
                  </div>
                  <span className="settings-row-value">{configValue(systemConfigs, 'sacco.currency')}</span>
                </div>
              </>
            )}
          </div>
        </>
      ) : (
        <div className="settings-group">
          <h2 className="settings-group-title">Administration Settings</h2>
          <hr className="rule" />
          <div className="settings-row">
            <div>
              <span className="settings-row-label">Access</span>
              <span className="settings-row-desc">
                System configuration is available to administrators only.
              </span>
            </div>
            <span className="settings-row-value">Restricted</span>
          </div>
        </div>
      )}

      <hr className="rule rule--strong settings-bottom-rule" />
    </div>
  )
}
