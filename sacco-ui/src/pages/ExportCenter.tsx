import { FileArrowDown } from '@phosphor-icons/react'
import './Operations.css'

interface ExportItem {
  name: string
  endpoint: string
  format: string
}

const exportsList: ExportItem[] = [
  { name: 'Member Statement (PDF)', endpoint: '/api/v1/export/member-statement/{memberId}/pdf', format: 'PDF' },
  { name: 'Member Statement (CSV)', endpoint: '/api/v1/export/member-statement/{memberId}/csv', format: 'CSV' },
  { name: 'Financial Summary (CSV)', endpoint: '/api/v1/export/financial-summary/csv', format: 'CSV' },
]

export function ExportCenter() {
  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Export Center</h1>
          <p className="page-subtitle">PDF and CSV exports from reporting APIs</p>
        </div>
      </div>

      <hr className="rule rule--strong" />

      <div className="reports-list">
        {exportsList.map(item => (
          <div key={item.endpoint} className="report-card">
            <div className="report-card-info">
              <div className="ops-card-title-row">
                <FileArrowDown size={15} className="ops-card-icon" />
                <span className="report-card-title">{item.name}</span>
              </div>
              <span className="ops-card-api data">{item.endpoint}</span>
            </div>
            <div className="report-card-actions">
              <button type="button" className="btn btn--secondary btn--small" disabled>
                Generate {item.format}
              </button>
            </div>
          </div>
        ))}
      </div>

      <p className="ops-note">Wire these buttons to signed download links when export services are ready.</p>
    </div>
  )
}
