import { useRef, type ReactNode, type RefObject } from 'react'
import { useVirtualizer } from '@tanstack/react-virtual'
import { SkeletonTableRows } from './Skeleton'
import './DataTable.css'

/* ─── Public types ─── */

export interface ColumnDef<T> {
  key: string
  header: string | ReactNode
  width?: string
  render: (row: T, index: number) => ReactNode
  className?: string
  headerClassName?: string
  sortable?: boolean
  sortKey?: string
}

export interface DataTableProps<T> {
  columns: ColumnDef<T>[]
  data: T[]
  getRowKey: (row: T) => string
  loading?: boolean
  skeletonRows?: number
  emptyMessage?: string | ReactNode
  maxHeight?: string
  getRowClassName?: (row: T, index: number) => string
  onRowClick?: (row: T) => void
  sortColumn?: string
  sortDirection?: 'asc' | 'desc'
  onSort?: (column: string) => void
  stickyTotals?: ReactNode
  estimateRowSize?: number | ((index: number) => number)
  overscan?: number
  scrollRef?: RefObject<HTMLDivElement | null>
  afterScrollContent?: ReactNode
}

/* ─── Component ─── */

export function DataTable<T>({
  columns,
  data,
  getRowKey,
  loading = false,
  skeletonRows = 3,
  emptyMessage = 'No data found.',
  maxHeight = 'calc(100vh - 260px)',
  getRowClassName,
  onRowClick,
  sortColumn,
  sortDirection,
  onSort,
  stickyTotals,
  estimateRowSize = 36,
  overscan = 14,
  scrollRef: externalScrollRef,
  afterScrollContent,
}: DataTableProps<T>) {
  const internalScrollRef = useRef<HTMLDivElement>(null)
  const scrollRef = externalScrollRef ?? internalScrollRef

  const rowVirtualizer = useVirtualizer({
    count: data.length,
    getScrollElement: () => scrollRef.current,
    estimateSize: typeof estimateRowSize === 'function' ? estimateRowSize : () => estimateRowSize,
    overscan,
  })

  const virtualRows = rowVirtualizer.getVirtualItems()
  const topPadding = virtualRows.length > 0 ? virtualRows[0].start : 0
  const bottomPadding =
    virtualRows.length > 0
      ? rowVirtualizer.getTotalSize() - virtualRows[virtualRows.length - 1].end
      : 0

  // Build CSS custom properties for column widths
  const colVars: Record<string, string> = {}
  columns.forEach((col, i) => {
    if (col.width) {
      colVars[`--dt-col-${i}`] = col.width
    }
  })

  const showSkeleton = loading && data.length === 0
  const showEmpty = !loading && data.length === 0

  return (
    <div className="datatable-frame" style={colVars as React.CSSProperties}>
      <div
        className="datatable-scroll-container"
        ref={scrollRef}
        style={{ maxHeight }}
      >
        <table className="datatable">
          <colgroup>
            {columns.map((col, i) => (
              <col
                key={col.key}
                style={col.width ? { width: `var(--dt-col-${i})` } : undefined}
              />
            ))}
          </colgroup>
          <thead>
            <tr>
              {columns.map(col => {
                const isSorted = col.sortable && col.sortKey === sortColumn
                const headerCls = [
                  'label',
                  col.sortable ? 'sortable' : '',
                  col.headerClassName ?? '',
                ].filter(Boolean).join(' ')

                return (
                  <th
                    key={col.key}
                    className={headerCls}
                    onClick={
                      col.sortable && col.sortKey && onSort
                        ? () => onSort(col.sortKey!)
                        : undefined
                    }
                    aria-sort={
                      isSorted
                        ? sortDirection === 'asc'
                          ? 'ascending'
                          : 'descending'
                        : undefined
                    }
                  >
                    {col.header}
                    {isSorted && (
                      <span className="datatable-sort-indicator">
                        {sortDirection === 'asc' ? ' \u25B2' : ' \u25BC'}
                      </span>
                    )}
                  </th>
                )
              })}
            </tr>
          </thead>
          <tbody>
            {showSkeleton ? (
              <SkeletonTableRows cols={columns.length} rows={skeletonRows} />
            ) : showEmpty ? (
              <tr>
                <td colSpan={columns.length} className="table-empty">
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              <>
                {topPadding > 0 && (
                  <tr className="datatable-spacer-row" aria-hidden="true">
                    <td colSpan={columns.length} style={{ height: `${topPadding}px` }} />
                  </tr>
                )}

                {virtualRows.map(virtualRow => {
                  const row = data[virtualRow.index]
                  if (!row) return null
                  const rowKey = getRowKey(row)
                  const rowCls = getRowClassName
                    ? getRowClassName(row, virtualRow.index)
                    : virtualRow.index % 2 === 1
                      ? 'datatable-row--alt'
                      : ''

                  return (
                    <tr
                      key={rowKey}
                      className={rowCls}
                      onClick={onRowClick ? () => onRowClick(row) : undefined}
                      style={onRowClick ? { cursor: 'pointer' } : undefined}
                    >
                      {columns.map(col => (
                        <td key={col.key} className={col.className}>
                          {col.render(row, virtualRow.index)}
                        </td>
                      ))}
                    </tr>
                  )
                })}

                {bottomPadding > 0 && (
                  <tr className="datatable-spacer-row" aria-hidden="true">
                    <td colSpan={columns.length} style={{ height: `${bottomPadding}px` }} />
                  </tr>
                )}
              </>
            )}
          </tbody>
        </table>
        {afterScrollContent}
      </div>

      {stickyTotals}
    </div>
  )
}
