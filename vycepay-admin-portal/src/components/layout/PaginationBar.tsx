'use client'

import type { PaginatedResponse } from '@/lib/api'

export function PaginationBar({
  data,
  onPageChange,
}: {
  data: PaginatedResponse<unknown> | null
  onPageChange: (page: number) => void
}) {
  if (!data || data.totalPages <= 1) return null
  const page = data.page ?? 0
  return (
    <div className="pagination">
      <span className="muted">
        Page {page + 1} of {data.totalPages} · {data.total} total
      </span>
      <div className="actions">
        <button className="btn secondary btn-sm" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>Previous</button>
        <button className="btn secondary btn-sm" disabled={page + 1 >= data.totalPages} onClick={() => onPageChange(page + 1)}>Next</button>
      </div>
    </div>
  )
}
