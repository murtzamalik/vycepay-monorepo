'use client'

import { formatCell } from '@/lib/format'
import type { Column } from '@/lib/columns/types'

export function DataTable<T extends Record<string, unknown>>({
  columns,
  rows,
  rowKey,
}: {
  columns: Column<T>[]
  rows: T[]
  rowKey?: (row: T, idx: number) => string | number
}) {
  if (!rows.length) return <div className="card muted">No records found.</div>
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>{columns.map((col) => <th key={col.key}>{col.label}</th>)}</tr>
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
