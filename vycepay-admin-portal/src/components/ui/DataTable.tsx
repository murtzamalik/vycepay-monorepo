'use client'

import { formatCell } from '@/lib/format'
import type { Column } from '@/lib/columns/types'

export function DataTable<T extends Record<string, unknown>>({
  columns,
  rows,
  rowKey,
  sortKey,
  sortOrder,
  onSort,
}: {
  columns: Column<T>[]
  rows: T[]
  rowKey?: (row: T, idx: number) => string | number
  sortKey?: string
  sortOrder?: 'asc' | 'desc'
  onSort?: (key: string, order: 'asc' | 'desc') => void
}) {
  if (!rows.length) return <div className="card muted">No records found.</div>

  function handleSort(key: string) {
    if (!onSort) return
    if (sortKey === key) {
      onSort(key, sortOrder === 'asc' ? 'desc' : 'asc')
    } else {
      onSort(key, 'desc')
    }
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key}>
                {col.sortable && onSort ? (
                  <button
                    type="button"
                    className="sort-header"
                    onClick={() => handleSort(col.key)}
                    style={{
                      background: 'none',
                      border: 'none',
                      padding: 0,
                      cursor: 'pointer',
                      font: 'inherit',
                      color: 'inherit',
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: 4,
                    }}
                  >
                    {col.label}
                    {sortKey === col.key ? (sortOrder === 'asc' ? ' ↑' : ' ↓') : ''}
                  </button>
                ) : (
                  col.label
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, idx) => (
            <tr key={rowKey ? rowKey(row, idx) : idx}>
              {columns.map((col) => (
                <td key={col.key} className={col.mono ? 'mono' : undefined}>
                  {col.render ? col.render(row) : formatCell(row[col.key])}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
