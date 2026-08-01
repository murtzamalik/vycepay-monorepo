'use client'

import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch, errorMessage } from '@/lib/api'
import { formatDateTime } from '@/lib/format'
import { DetailLayout } from '@/components/detail/DetailLayout'
import { KeyValueGrid } from '@/components/detail/KeyValueGrid'
import { JsonViewer } from '@/components/detail/JsonViewer'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { DataTable } from '@/components/ui/DataTable'
import { ErrorState, SkeletonTable } from '@/components/ui/States'
import { NotificationResendActions } from '@/components/shared/ResourceActions'
import type { Column } from '@/lib/columns/types'

const attemptColumns: Column[] = [
  { key: 'id', label: 'ID' },
  { key: 'status', label: 'Status', render: (r) => <StatusBadge status={String(r.status ?? '')} /> },
  { key: 'triggerSource', label: 'Trigger' },
  { key: 'successCount', label: 'OK' },
  { key: 'failureCount', label: 'Fail' },
  { key: 'skipReason', label: 'Skip' },
  { key: 'createdAt', label: 'At', render: (r) => formatDateTime(r.createdAt) },
]

export function NotificationDetail() {
  const params = useParams<{ id: string }>()
  const { data, isLoading, error } = useQuery({
    queryKey: ['notification', params.id],
    queryFn: () => apiFetch<Record<string, unknown>>(`/notifications/${params.id}`),
  })
  if (isLoading) return <SkeletonTable />
  if (error || !data) return <ErrorState message={errorMessage(error, 'Unable to load notification.')} />

  let payload: unknown = data.data_json ?? data.dataJson
  if (payload && typeof payload === 'string') {
    try { payload = JSON.parse(payload) } catch { /* keep string */ }
  }
  const attempts = Array.isArray(data.attempts) ? (data.attempts as Record<string, unknown>[]) : []

  return (
    <DetailLayout
      header={
        <div>
          <h2>Notification #{String(data.id)}</h2>
          <StatusBadge status={String(data.source ?? '')} />
        </div>
      }
      actions={<NotificationResendActions id={params.id} />}
    >
      <div className="card">
        <KeyValueGrid items={[
          { label: 'Public ID', value: <span className="mono">{String(data.public_id ?? data.publicId)}</span> },
          { label: 'Customer', value: String(data.customerExternalId ?? data.customer_id ?? data.customerId ?? '—') },
          { label: 'Push type', value: String(data.push_type ?? data.pushType) },
          { label: 'Notification type', value: String(data.notification_type ?? data.notificationType ?? '—') },
          { label: 'Title', value: String(data.title) },
          { label: 'Body', value: String(data.body) },
          { label: 'Batch', value: String(data.batch_id ?? data.batchId ?? '—') },
          { label: 'Read at', value: formatDateTime(data.read_at ?? data.readAt) },
          { label: 'Created', value: formatDateTime(data.created_at ?? data.createdAt) },
        ]} />
      </div>
      <JsonViewer data={payload ?? {}} />
      <div className="card">
        <div className="section-title">Delivery attempts</div>
        <DataTable columns={attemptColumns} rows={attempts} rowKey={(row, i) => String(row.id ?? i)} />
      </div>
    </DetailLayout>
  )
}
