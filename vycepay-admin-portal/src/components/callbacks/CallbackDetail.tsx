'use client'

import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api'
import { formatDateTime } from '@/lib/format'
import { DetailLayout } from '@/components/detail/DetailLayout'
import { KeyValueGrid } from '@/components/detail/KeyValueGrid'
import { JsonViewer } from '@/components/detail/JsonViewer'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { ErrorState, SkeletonTable } from '@/components/ui/States'
import { CallbackActions } from '@/components/shared/ResourceActions'

export function CallbackDetail() {
  const params = useParams<{ id: string }>()
  const { data, isLoading, error } = useQuery({ queryKey: ['callback', params.id], queryFn: () => apiFetch<Record<string, unknown>>(`/callbacks/${params.id}`) })
  if (isLoading) return <SkeletonTable />
  if (error || !data) return <ErrorState message="Unable to load callback." />
  const redacted = !data.raw_payload
  let payload: unknown = data.raw_payload
  if (payload && typeof payload === 'string') {
    try { payload = JSON.parse(payload) } catch { /* keep string */ }
  }
  return (
    <DetailLayout
      header={<div><h2>Callback #{String(data.id)}</h2><StatusBadge status={data.processed ? 'COMPLETED' : 'PENDING'} /></div>}
      actions={<CallbackActions id={params.id} />}
    >
      <div className="card">
        <KeyValueGrid items={[
          { label: 'Choice Request ID', value: <span className="mono">{String(data.choice_request_id ?? data.choiceRequestId)}</span> },
          { label: 'Notification Type', value: String(data.notification_type ?? data.notificationType) },
          { label: 'Processed', value: data.processed ? 'Yes' : 'No' },
          { label: 'Processed At', value: formatDateTime(data.processed_at ?? data.processedAt) },
          { label: 'Error', value: String(data.processing_error ?? data.processingError ?? '—') },
          { label: 'Received', value: formatDateTime(data.created_at ?? data.createdAt) },
        ]} />
      </div>
      <JsonViewer data={payload ?? {}} redacted={redacted} />
    </DetailLayout>
  )
}
