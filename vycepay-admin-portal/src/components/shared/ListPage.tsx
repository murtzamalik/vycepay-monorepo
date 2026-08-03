'use client'

import { Suspense, useMemo } from 'react'
import { PageHeader } from '@/components/layout/PageHeader'
import { FilterBar } from '@/components/layout/FilterBar'
import { PaginationBar } from '@/components/layout/PaginationBar'
import { DateRangeFilter } from '@/components/layout/DateRangeFilter'
import { DataTable } from '@/components/ui/DataTable'
import { ErrorState, SkeletonTable } from '@/components/ui/States'
import { useListQuery, type ListFilters } from '@/lib/hooks/useListQuery'
import { buildQuery, errorMessage } from '@/lib/api'
import type { Column } from '@/lib/columns/types'

function ColumnFilters<T extends Record<string, unknown>>({
  columns,
  filters,
  setFilters,
}: {
  columns: Column<T>[]
  filters: ListFilters
  setFilters: (patch: Partial<ListFilters>) => void
}) {
  return (
    <>
      {columns.map((col) => {
        if (!col.filter) return null
        const f = col.filter
        if (f.type === 'select') {
          return (
            <select
              key={f.param}
              className="input input-sm"
              value={filters[f.param] ?? ''}
              onChange={(e) => setFilters({ [f.param]: e.target.value })}
              aria-label={col.label}
            >
              <option value="">{f.placeholder ?? `All ${col.label}`}</option>
              {(f.options ?? []).map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </select>
          )
        }
        return (
          <input
            key={f.param}
            className="input input-sm"
            placeholder={f.placeholder ?? col.label}
            defaultValue={filters[f.param] ?? ''}
            onKeyDown={(e) => {
              if (e.key === 'Enter') setFilters({ [f.param]: (e.target as HTMLInputElement).value })
            }}
          />
        )
      })}
    </>
  )
}

function sortRowsClient<T extends Record<string, unknown>>(rows: T[], sort?: string, order?: string): T[] {
  if (!sort) return rows
  const dir = order === 'asc' ? 1 : -1
  return [...rows].sort((a, b) => {
    const av = a[sort]
    const bv = b[sort]
    if (av == null && bv == null) return 0
    if (av == null) return 1
    if (bv == null) return -1
    if (typeof av === 'number' && typeof bv === 'number') return (av - bv) * dir
    return String(av).localeCompare(String(bv), undefined, { numeric: true }) * dir
  })
}

function ListPageInner<T extends Record<string, unknown>>({
  title,
  description,
  endpoint,
  columns,
  exportPath,
  filters,
  filterSlot,
  showDateRange,
  hideHeader,
  hideSearch,
  headerActions,
  clientSort,
}: {
  title: string
  description: string
  endpoint: string
  columns: Column<T>[]
  exportPath?: string
  filters?: React.ReactNode
  filterSlot?: (ctx: { filters: ListFilters; setFilters: (patch: Partial<ListFilters>) => void }) => React.ReactNode
  showDateRange?: boolean
  hideHeader?: boolean
  hideSearch?: boolean
  headerActions?: React.ReactNode
  /** When true, sort locally (for non-paginated endpoints like roles). */
  clientSort?: boolean
}) {
  const { data, isLoading, error, filters: f, setFilters } = useListQuery<T>(endpoint)

  const exportHref = exportPath ? `${exportPath}${buildQuery(f)}` : undefined
  const sortKey = f.sort
  const sortOrder = (f.order === 'asc' ? 'asc' : 'desc') as 'asc' | 'desc'

  const rows = useMemo(() => {
    if (!data) return []
    if (clientSort) return sortRowsClient(data.content, sortKey, sortOrder)
    return data.content
  }, [data, clientSort, sortKey, sortOrder])

  return (
    <div className="grid">
      {!hideHeader ? (
        <PageHeader
          title={title}
          description={description}
          actions={
            <>
              {headerActions}
              {exportHref ? <a className="btn secondary" href={exportHref}>Export CSV</a> : null}
            </>
          }
        />
      ) : null}
      <FilterBar
        search={hideSearch ? undefined : f.search}
        onSearch={hideSearch ? undefined : (search) => setFilters({ search })}
      >
        <ColumnFilters columns={columns} filters={f} setFilters={setFilters} />
        {filters}
        {filterSlot ? filterSlot({ filters: f, setFilters }) : null}
        {showDateRange ? (
          <DateRangeFilter fromDate={f.fromDate} toDate={f.toDate} onChange={(fromDate, toDate) => setFilters({ fromDate, toDate })} />
        ) : null}
      </FilterBar>
      {isLoading ? <SkeletonTable /> : null}
      {error ? <ErrorState message={errorMessage(error, 'Unable to load data.')} /> : null}
      {data ? (
        <DataTable
          columns={columns}
          rows={rows}
          rowKey={(row, i) => String(row.id ?? row.externalId ?? i)}
          sortKey={sortKey}
          sortOrder={sortOrder}
          onSort={(key, order) => setFilters({ sort: key, order })}
        />
      ) : null}
      {!clientSort ? <PaginationBar data={data} onPageChange={(page) => setFilters({ page: String(page) })} /> : null}
    </div>
  )
}

export function ListPage<T extends Record<string, unknown>>(props: Parameters<typeof ListPageInner<T>>[0]) {
  return <Suspense fallback={<SkeletonTable />}><ListPageInner {...props} /></Suspense>
}
