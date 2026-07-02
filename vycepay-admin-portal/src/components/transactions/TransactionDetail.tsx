'use client'

import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api'
import { formatDateTime, formatKes } from '@/lib/format'
import { DetailLayout } from '@/components/detail/DetailLayout'
import { KeyValueGrid } from '@/components/detail/KeyValueGrid'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { EntityLink } from '@/components/ui/EntityLink'
import { ErrorState, SkeletonTable } from '@/components/ui/States'

export function TransactionDetail() {
  const params = useParams<{ id: string }>()
  const { data, isLoading, error } = useQuery({ queryKey: ['tx', params.id], queryFn: () => apiFetch<Record<string, unknown>>(`/transactions/${params.id}`) })
  if (isLoading) return <SkeletonTable />
  if (error || !data) return <ErrorState message="Unable to load transaction." />
  return (
    <DetailLayout header={<div><h2 className="mono">{String(data.external_id ?? data.externalId)}</h2><StatusBadge status={data.status} /></div>}>
      <div className="card">
        <KeyValueGrid items={[
          { label: 'Type', value: <StatusBadge status={data.type} /> },
          { label: 'Amount', value: <span className="mono">{formatKes(data.amount)}</span> },
          { label: 'Fee', value: formatKes(data.fee_amount ?? data.feeAmount ?? 0) },
          { label: 'Status', value: <StatusBadge status={data.status} /> },
          { label: 'Customer', value: <EntityLink href={`/customers/${data.customerExternalId}`}>{String(data.customerExternalId)}</EntityLink> },
          { label: 'Wallet', value: data.walletId ? <EntityLink href={`/wallets/${data.walletId}`}>#{String(data.walletId)}</EntityLink> : '—' },
          { label: 'Choice Request ID', value: <span className="mono">{String(data.choice_request_id ?? data.choiceRequestId ?? '—')}</span> },
          { label: 'Choice TX ID', value: <span className="mono">{String(data.choice_tx_id ?? '—')}</span> },
          { label: 'Payee', value: String(data.payee_account_name ?? '—') },
          { label: 'Error Code', value: String(data.error_code ?? data.errorCode ?? '—') },
          { label: 'Error Message', value: String(data.error_msg ?? data.errorMsg ?? '—') },
          { label: 'Created', value: formatDateTime(data.created_at ?? data.createdAt) },
          { label: 'Completed', value: formatDateTime(data.completed_at ?? data.completedAt) },
        ]} />
      </div>
    </DetailLayout>
  )
}
