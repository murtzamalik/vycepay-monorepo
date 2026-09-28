'use client'

import Link from 'next/link'
import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch, errorMessage } from '@/lib/api'
import { formatDateTime } from '@/lib/format'
import { DetailLayout } from '@/components/detail/DetailLayout'
import { KeyValueGrid } from '@/components/detail/KeyValueGrid'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { ErrorState, SkeletonTable } from '@/components/ui/States'
import { SmsOutboxRetryActions } from '@/components/shared/ResourceActions'
import { EntityLink } from '@/components/ui/EntityLink'

export function SmsOutboxDetail() {
  const params = useParams<{ id: string }>()
  const { data, isLoading, error } = useQuery({
    queryKey: ['sms-outbox', params.id],
    queryFn: () => apiFetch<Record<string, unknown>>(`/sms/outbox/${params.id}`),
  })
  if (isLoading) return <SkeletonTable />
  if (error || !data) return <ErrorState message={errorMessage(error, 'Unable to load SMS outbox row.')} />

  const status = String(data.status ?? '')
  const canRetry = status !== 'SENT'
  const notificationId = data.notificationId ?? data.notification_id
  const customerExt = data.customerExternalId ?? data.customer_external_id

  return (
    <DetailLayout
      header={
        <div>
          <h2>SMS outbox #{String(data.id)}</h2>
          <StatusBadge status={status} />
        </div>
      }
      actions={
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <Link className="btn secondary" href="/sms/outbox">Back to outbox</Link>
          {canRetry ? <SmsOutboxRetryActions id={params.id} /> : null}
        </div>
      }
    >
      <div className="card">
        <KeyValueGrid items={[
          { label: 'Public ID', value: <span className="mono">{String(data.publicId ?? data.public_id)}</span> },
          { label: 'Status', value: <StatusBadge status={status} /> },
          { label: 'Dedupe key', value: <span className="mono">{String(data.dedupeKey ?? data.dedupe_key ?? '—')}</span> },
          { label: 'Recipient', value: <span className="mono">{String(data.recipientMasked ?? '—')}</span> },
          { label: 'Message', value: String(data.messageBody ?? data.message_body ?? '—') },
          { label: 'Attempts', value: `${data.attemptCount ?? data.attempt_count ?? 0} / ${data.maxAttempts ?? data.max_attempts ?? '—'}` },
          { label: 'Next attempt', value: formatDateTime(data.nextAttemptAt ?? data.next_attempt_at) },
          { label: 'Last error', value: String(data.lastError ?? data.last_error ?? '—') },
          { label: 'Provider UID', value: String(data.providerUid ?? data.provider_uid ?? '—') },
          { label: 'Customer', value: customerExt
            ? <EntityLink href={`/customers/${customerExt}`}><span className="mono">{String(customerExt)}</span></EntityLink>
            : String(data.customerId ?? '—') },
          { label: 'Inbox notification', value: notificationId
            ? <EntityLink href={`/notifications/${notificationId}`}><span className="mono">#{String(notificationId)}</span></EntityLink>
            : '—' },
          { label: 'Sent at', value: formatDateTime(data.sentAt ?? data.sent_at) },
          { label: 'Created', value: formatDateTime(data.createdAt ?? data.created_at) },
          { label: 'Updated', value: formatDateTime(data.updatedAt ?? data.updated_at) },
        ]} />
      </div>
    </DetailLayout>
  )
}
