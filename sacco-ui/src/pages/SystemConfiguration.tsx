import './Operations.css'

const systemKeys = [
  { key: 'SACCO_NAME', value: 'InnerCircle SACCO' },
  { key: 'CURRENCY', value: 'KES' },
  { key: 'FINANCIAL_YEAR_START', value: 'January' },
]

const loanProducts = [
  { name: 'Standard Loan', rate: '12% p.a.', cap: '3x savings' },
  { name: 'Emergency Loan', rate: '10% p.a.', cap: '1x savings' },
]

const schedules = [
  { category: 'Monthly', dueDay: '15', gracePeriod: '5 days' },
  { category: 'Special', dueDay: 'Ad-hoc', gracePeriod: '0 days' },
]

const penalties = [
  { name: 'Late Contribution', value: 'KES 500' },
  { name: 'Loan Default', value: '5% of arrears' },
  { name: 'Meeting Absence', value: 'KES 200' },
]

export function SystemConfiguration() {
  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">System Configuration</h1>
          <p className="page-subtitle">Config module frontend for system and policy settings</p>
        </div>
      </div>

      <hr className="rule rule--strong" />

      <div className="settings-group">
        <h2 className="settings-group-title">System Keys</h2>
        <hr className="rule" />
        {systemKeys.map(item => (
          <div className="settings-row" key={item.key}>
            <div><span className="settings-row-label data">{item.key}</span></div>
            <span className="settings-row-value">{item.value}</span>
          </div>
        ))}
      </div>

      <div className="settings-group">
        <h2 className="settings-group-title">Loan Products</h2>
        <hr className="rule" />
        {loanProducts.map(item => (
          <div className="settings-row" key={item.name}>
            <div>
              <span className="settings-row-label">{item.name}</span>
              <span className="settings-row-desc">{item.cap}</span>
            </div>
            <span className="settings-row-value">{item.rate}</span>
          </div>
        ))}
      </div>

      <div className="settings-group">
        <h2 className="settings-group-title">Contribution Schedules</h2>
        <hr className="rule" />
        {schedules.map(item => (
          <div className="settings-row" key={item.category}>
            <div>
              <span className="settings-row-label">{item.category}</span>
              <span className="settings-row-desc">Grace period: {item.gracePeriod}</span>
            </div>
            <span className="settings-row-value">{item.dueDay}</span>
          </div>
        ))}
      </div>

      <div className="settings-group">
        <h2 className="settings-group-title">Penalty Rules</h2>
        <hr className="rule" />
        {penalties.map(item => (
          <div className="settings-row" key={item.name}>
            <div><span className="settings-row-label">{item.name}</span></div>
            <span className="settings-row-value penalty-value">{item.value}</span>
          </div>
        ))}
      </div>

      <p className="ops-note">
        API coverage: /api/v1/config/system, /loan-products, /contribution-schedules, /penalty-rules.
      </p>
    </div>
  )
}
