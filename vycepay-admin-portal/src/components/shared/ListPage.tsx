'use client'

import { Suspense } from 'react'
import { PageHeader } from '@/components/layout/PageHeader'
import { FilterBar } from '@/components/layout/FilterBar'
import { PaginationBar } from '@/components/layout/PaginationBar'
import { DateRangeFilter } from '@/components/layout/DateRangeFilter'
import { DataTable } from '@/components/ui/DataTable'
import { ErrorState, SkeletonTable } from '@/components/ui/States'
import { useListQuery, type ListFilters } from '@/lib/hooks/useListQuery'
import { buildQuery } from '@/lib/api'
import type { Column } from '@/lib/columns/types'

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
}) {
  const { data, isLoading, error, filters: f, setFilters } = useListQuery<T>(endpoint)

  const exportHref = exportPath ? `${exportPath}${buildQuery(f)}` : undefined

  return (
    <div className="grid">
      {!hideHeader ? (
        <PageHeader
          title={title}
          description={description}
          actions={exportHref ? <a className="btn secondary" href={exportHref}>Export CSV</a> : undefined}
        />
      ) : null}
      <FilterBar search={f.search} onSearch={(search) => setFilters({ search })}>
        {filters}
        {filterSlot ? filterSlot({ filters: f, setFilters }) : null}
        {showDateRange ? (
          <DateRangeFilter fromDate={f.fromDate} toDate={f.toDate} onChange={(fromDate, toDate) => setFilters({ fromDate, toDate })} />
        ) : null}
      </FilterBar>
      {isLoading ? <SkeletonTable /> : null}
      {error ? <ErrorState message="Unable to load data." /> : null}
      {data ? <DataTable columns={columns} rows={data.content} rowKey={(row, i) => String(row.id ?? row.externalId ?? i)} /> : null}
      <PaginationBar data={data} onPageChange={(page) => setFilters({ page: String(page) })} />
    </div>
  )
}

export function ListPage<T extends Record<string, unknown>>(props: Parameters<typeof ListPageInner<T>>[0]) {
  return <Suspense fallback={<SkeletonTable />}><ListPageInner {...props} /></Suspense>
}
