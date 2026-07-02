'use client'

import { useState } from 'react'
import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch, buildQuery } from '@/lib/api'
import { formatDate, formatDateTime, formatKes, formatRelative } from '@/lib/format'
import { DetailLayout } from '@/components/detail/DetailLayout'
import { KeyValueGrid } from '@/components/detail/KeyValueGrid'
import { DataTable } from '@/components/ui/DataTable'
import { Avatar } from '@/components/ui/Avatar'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { EntityLink } from '@/components/ui/EntityLink'
import { ErrorState, SkeletonTable } from '@/components/ui/States'
import { CustomerActions } from '@/components/shared/ResourceActions'
import type { Column } from '@/lib/columns/types'

const TABS = [
  { id: 'overview', label: 'Overview' },
  { id: 'transactions', label: 'Transactions' },
  { id: 'kyc', label: 'KYC' },
  { id: 'activity', label: 'Activity' },
]

const txColumns: Column[] = [
  { key: 'externalId', label: 'TX ID', mono: true, render: (r) => <EntityLink href={`/transactions/${r.externalId}`}>{String(r.externalId)}</EntityLink> },
  { key: 'type', label: 'Type', render: (r) => <StatusBadge status={r.type} /> },
  { key: 'amount', label: 'Amount', render: (r) => formatKes(r.amount) },
  { key: 'status', label: 'Status', render: (r) => <StatusBadge status={r.status} /> },
  { key: 'createdAt', label: 'Date', render: (r) => formatDate(r.createdAt) },
]

export function CustomerDetail() {
  const params = useParams<{ id: string }>()
  const id = params.id
  const [tab, setTab] = useState('overview')

  const detail = useQuery({ queryKey: ['customer', id], queryFn: () => apiFetch<Record<string, unknown>>(`/customers/${id}`) })
  const summary = useQuery({ queryKey: ['customer-summary', id], queryFn: () => apiFetch<Record<string, unknown>>(`/customers/${id}/summary`) })
  const kyc = useQuery({ queryKey: ['customer-kyc', id], queryFn: () => apiFetch<Record<string, unknown>>(`/customers/${id}/kyc`), enabled: tab === 'kyc' || tab === 'overview' })
  const tx = useQuery({ queryKey: ['customer-tx', id], queryFn: () => apiFetch<unknown>(`/customers/${id}/transactions?page=0&size=20`), enabled: tab === 'transactions' })
  const activity = useQuery({ queryKey: ['customer-activity', id], queryFn: () => apiFetch<unknown>(`/customers/${id}/activity?page=0&size=20`), enabled: tab === 'activity' })

  if (detail.isLoading) return <SkeletonTable />
  if (detail.error || !detail.data) return <ErrorState message="Unable to load customer." />

  const c = detail.data
  const name = `${c.firstName ?? ''} ${c.lastName ?? ''}`.trim() || 'Customer'

  const header = (
    <div style={{ display: 'flex', gap: 18, alignItems: 'center' }}>
      <Avatar name={name} size="lg" />
      <div>
        <div className="actions" style={{ marginBottom: 4 }}>
          <h2 style={{ margin: 0 }}>{name}</h2>
          <StatusBadge status={c.status} />
        </div>
        <div className="muted" style={{ fontSize: 12 }}>
          <span className="mono">{String(c.mobile ?? '')}</span>
          <span> · </span>
          <span className="mono">{String(c.externalId)}</span>
          <span> · </span>
          <span>Member since {formatDate(c.createdAt)}</span>
        </div>
        {summary.data ? (
          <div className="actions" style={{ marginTop: 16, paddingTop: 16, borderTop: '1px solid var(--border)' }}>
            <div><div className="kpi-label">Total Sent</div><div className="mono">{formatKes(summary.data.totalSent)}</div></div>
            <div><div className="kpi-label">Deposited</div><div className="mono">{formatKes(summary.data.totalDeposited)}</div></div>
            <div><div className="kpi-label">Transactions</div><div className="mono">{String(summary.data.transactionCount)}</div></div>
            <div><div className="kpi-label">Last Active</div><div>{formatRelative(summary.data.lastActiveAt)}</div></div>
            <div><div className="kpi-label">Failed Tx</div><div className="mono" style={{ color: 'var(--danger)' }}>{String(summary.data.failedTransactionCount)}</div></div>
          </div>
        ) : null}
      </div>
    </div>
  )

  return (
    <DetailLayout header={header} actions={<CustomerActions id={id} status={c.status} />} tabs={TABS} activeTab={tab} onTabChange={setTab}>
      {tab === 'overview' ? (
        <div className="grid" style={{ gridTemplateColumns: '1fr 1fr' }}>
          <div className="card">
            <div className="section-hdr"><span className="section-title">Wallet</span><StatusBadge status={c.walletStatus} /></div>
            <div style={{ fontSize: 28, fontWeight: 800 }} className="mono">{formatKes(c.walletBalance)}</div>
            <p className="muted">Balance</p>
            {c.walletId ? <EntityLink href={`/wallets/${c.walletId}`}>View wallet →</EntityLink> : null}
            <p className="mono muted" style={{ marginTop: 8 }}>{String(c.choiceAccountId ?? '')}</p>
          </div>
          <div className="card">
            <div className="section-title" style={{ marginBottom: 14 }}>Customer Details</div>
            <KeyValueGrid items={[
              { label: 'Full Name', value: name },
              { label: 'Mobile', value: String(c.mobile ?? '—') },
              { label: 'Email', value: String(c.email ?? '—') },
              { label: 'External ID', value: <span className="mono">{String(c.externalId)}</span> },
              { label: 'KYC Status', value: <StatusBadge status={c.kycStatus} /> },
              { label: 'Registered', value: formatDateTime(c.createdAt) },
            ]} />
          </div>
        </div>
      ) : null}
      {tab === 'transactions' && tx.data ? (
        <DataTable columns={txColumns} rows={(tx.data as { content?: Record<string, unknown>[] }).content ?? []} />
      ) : null}
      {tab === 'kyc' && kyc.data ? (
        <div className="card">
          <KeyValueGrid items={[
            { label: 'Status', value: <StatusBadge status={kyc.data.status} /> },
            { label: 'Choice Onboarding ID', value: <span className="mono">{String(kyc.data.choice_onboarding_request_id ?? kyc.data.choiceOnboardingRequestId ?? '—')}</span> },
            { label: 'ID Type', value: String(kyc.data.id_type ?? kyc.data.idType ?? '—') },
            { label: 'ID Number', value: String(kyc.data.id_number ?? '—') },
          ]} />
        </div>
      ) : null}
      {tab === 'activity' && activity.data ? (
        <DataTable columns={[
          { key: 'action', label: 'Action' },
          { key: 'resourceType', label: 'Resource' },
          { key: 'createdAt', label: 'When', render: (r) => formatDateTime(r.createdAt) },
        ]} rows={(activity.data as { content?: Record<string, unknown>[] }).content ?? []} />
      ) : null}
    </DetailLayout>
  )
}
