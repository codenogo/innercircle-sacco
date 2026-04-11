import type { ReactNode } from 'react'
import './StatCard.css'

export interface StatCardItem {
  label: string
  value: ReactNode
  /** Optional CSS class applied to the value element */
  valueClassName?: string
}

interface StatCardGridProps {
  items: StatCardItem[]
  /** Max columns — defaults to items.length, capped at 4 */
  columns?: number
}

export function StatCardGrid({ items, columns }: StatCardGridProps) {
  const cols = columns ?? Math.min(items.length, 4)

  return (
    <div
      className="stat-card-grid"
      style={{ '--stat-cols': cols } as React.CSSProperties}
    >
      {items.map((item, i) => (
        <div key={i} className="stat-card">
          <span className="stat-card-label">{item.label}</span>
          <span className={`stat-card-value${item.valueClassName ? ` ${item.valueClassName}` : ''}`}>
            {item.value}
          </span>
        </div>
      ))}
    </div>
  )
}

