'use client'

import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch, errorMessage } from '@/lib/api'
import { formatDateTime } from '@/lib/format'
import { DetailLayout } from '@/components/detail/DetailLayout'
import { KeyValueGrid } from '@/components/detail/KeyValueGrid'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { DataTable } from '@/components/ui/DataTable'
import { ErrorState, SkeletonTable } from '@/components/ui/States'
import { SmsResendActions } from '@/components/shared/ResourceActions'
import type { Column } from '@/lib/columns/types'

const attemptColumns: Column[] = [
  { key: 'id', label: 'ID' },
  { key: 'status', label: 'Status', render: (r) => <StatusBadge status={String(r.status ?? '')} /> },
  { key: 'triggerSource', label: 'Trigger' },
  { key: 'providerUid', label: 'Provider UID', mono: true },
  { key: 'errorMessage', label: 'Error' },
  { key: 'createdAt', label: 'At', render: (r) => formatDateTime(r.createdAt) },
]

export function SmsDetail() {
  const params = useParams<{ id: string }>()
  const { data, isLoading, error } = useQuery({
    queryKey: ['sms', params.id],
    queryFn: () => apiFetch<Record<string, unknown>>(`/sms/${params.id}`),
  })
  if (isLoading) return <SkeletonTable />
  if (error || !data) return <ErrorState message={errorMessage(error, 'Unable to load SMS.')} />

  const attempts = Array.isArray(data.attempts) ? (data.attempts as Record<string, unknown>[]) : []
  const purpose = String(data.purpose ?? '')
  const body = purpose === 'AUTH_OTP'
    ? String(data.messageRedacted ?? data.message_redacted ?? '—')
    : String(data.messageBody ?? data.message_body ?? data.messageRedacted ?? '—')

  return (
    <DetailLayout
      header={
        <div>
          <h2>SMS #{String(data.id)}</h2>
          <StatusBadge status={String(data.status ?? '')} />
        </div>
      }
      actions={<SmsResendActions id={params.id} />}
    >
      <div className="card">
        <KeyValueGrid items={[
          { label: 'Public ID', value: <span className="mono">{String(data.publicId ?? data.public_id)}</span> },
          { label: 'Recipient', value: <span className="mono">{String(data.recipientMasked ?? '—')}</span> },
          { label: 'Purpose', value: purpose },
          { label: 'OTP purpose', value: String(data.otpPurpose ?? data.otp_purpose ?? '—') },
          { label: 'Message', value: body },
          { label: 'Provider', value: String(data.provider ?? '—') },
          { label: 'Provider UID', value: String(data.providerUid ?? data.provider_uid ?? '—') },
          { label: 'Error', value: String(data.errorMessage ?? data.error_message ?? '—') },
          { label: 'Batch', value: String(data.batchId ?? data.batch_id ?? '—') },
          { label: 'Customer', value: String(data.customerExternalId ?? data.customerId ?? '—') },
          { label: 'Sent at', value: formatDateTime(data.sentAt ?? data.sent_at) },
          { label: 'Created', value: formatDateTime(data.createdAt ?? data.created_at) },
        ]} />
      </div>
      <div className="card">
        <div className="section-title">Delivery attempts</div>
        <DataTable columns={attemptColumns} rows={attempts} rowKey={(row, i) => String(row.id ?? i)} />
      </div>
    </DetailLayout>
  )
}
