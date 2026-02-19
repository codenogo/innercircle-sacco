/* ─── Skeleton — loading placeholders ─── */

export function SkeletonText({ size = 'md' }: { size?: 'sm' | 'md' | 'lg' }) {
  const cls = size === 'sm' ? 'skeleton skeleton-text--sm'
            : size === 'lg' ? 'skeleton skeleton-text--lg'
            : 'skeleton skeleton-text'
  return <div className={cls} />
}

export function SkeletonRow({ cells = 4 }: { cells?: number }) {
  return (
    <div className="skeleton-row">
      {Array.from({ length: cells }, (_, i) => (
        <div key={i} className="skeleton-row-cell" />
      ))}
    </div>
  )
}

export function SkeletonTableRows({ rows = 3, cols }: { rows?: number; cols: number }) {
  return (
    <>
      {Array.from({ length: rows }, (_, r) => (
        <tr key={r} className="skeleton-table-row">
          {Array.from({ length: cols }, (_, c) => (
            <td key={c}>
              <div className="skeleton skeleton-table-cell" />
            </td>
          ))}
        </tr>
      ))}
    </>
  )
}

export function SkeletonStat() {
  return <div className="skeleton-stat" />
}

export function SkeletonHeader() {
  return <div className="skeleton skeleton-header" />
}
