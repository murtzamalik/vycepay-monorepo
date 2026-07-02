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
import { WalletActions } from '@/components/shared/ResourceActions'

export function WalletDetail() {
  const params = useParams<{ id: string }>()
  const { data, isLoading, error } = useQuery({ queryKey: ['wallet', params.id], queryFn: () => apiFetch<Record<string, unknown>>(`/wallets/${params.id}`) })
  if (isLoading) return <SkeletonTable />
  if (error || !data) return <ErrorState message="Unable to load wallet." />
  return (
    <DetailLayout
      header={<div><h2>Wallet #{String(data.id)}</h2><StatusBadge status={data.status} /></div>}
      actions={<WalletActions id={params.id} status={data.status} />}
    >
      <div className="card" style={{ maxWidth: 480 }}>
        <div style={{ fontSize: 32, fontWeight: 800 }} className="mono">{formatKes(data.balance_cache ?? data.balance)}</div>
        <KeyValueGrid items={[
          { label: 'Customer', value: <EntityLink href={`/customers/${data.customerExternalId}`}>{String(data.customerExternalId)}</EntityLink> },
          { label: 'Choice Account', value: <span className="mono">{String(data.choice_account_id ?? data.choiceAccountId)}</span> },
          { label: 'Currency', value: String(data.currency ?? 'KES') },
          { label: 'Status', value: <StatusBadge status={data.status} /> },
          { label: 'Last Updated', value: formatDateTime(data.last_balance_update_at ?? data.updated_at) },
        ]} />
      </div>
    </DetailLayout>
  )
}
