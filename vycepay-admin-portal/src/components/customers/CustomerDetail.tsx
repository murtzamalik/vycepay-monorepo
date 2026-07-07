'use client'

import { useState } from 'react'
import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api'
import {
  formatDate,
  formatDateTime,
  formatFullName,
  formatGender,
  formatIdType,
  formatKes,
  formatKycStatus,
  formatMobile,
  formatRelative,
} from '@/lib/format'
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

function field(value: unknown, fallback = '—') {
  if (value === null || value === undefined || value === '') return fallback
  return value
}

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
  const k = { ...c, ...(kyc.data ?? {}) }
  const name = formatFullName(c.firstName, c.middleName ?? k.middleName, c.lastName)
  const displayName = name === '—' ? 'Customer' : name
  const mobile = formatMobile(c.mobileCountryCode, c.mobile)
  const kycLabel = formatKycStatus(c.kycStatus, c.kycStatusLabel)

  const header = (
    <div style={{ display: 'flex', gap: 18, alignItems: 'center' }}>
      <Avatar name={displayName} size="lg" />
      <div style={{ flex: 1 }}>
        <div className="actions" style={{ marginBottom: 4 }}>
          <h2 style={{ margin: 0 }}>{displayName}</h2>
          <StatusBadge status={c.status} />
          {c.kycStatus ? <StatusBadge status={kycLabel} /> : null}
        </div>
        <div className="muted" style={{ fontSize: 12 }}>
          <span className="mono">{mobile}</span>
          {field(c.email) !== '—' ? <><span> · </span><span>{String(c.email)}</span></> : null}
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
        <div className="grid" style={{ gridTemplateColumns: '1fr 1fr', gap: 16 }}>
          <div className="card">
            <div className="section-hdr"><span className="section-title">Contact & Identity</span></div>
            <KeyValueGrid items={[
              { label: 'Full Name', value: name },
              { label: 'Mobile', value: <span className="mono">{mobile}</span> },
              { label: 'Email', value: String(field(c.email)) },
              { label: 'Customer ID', value: <span className="mono">{String(c.externalId)}</span> },
              { label: 'Internal ID', value: <span className="mono">#{String(c.id)}</span> },
              { label: 'Account Status', value: <StatusBadge status={c.status} /> },
              { label: 'Registered', value: formatDateTime(c.createdAt) },
              { label: 'Last Updated', value: formatDateTime(c.updatedAt) },
              { label: 'Registered Devices', value: String(field(c.deviceCount, '0')) },
            ]} />
          </div>

          <div className="card">
            <div className="section-hdr"><span className="section-title">KYC Profile</span><StatusBadge status={kycLabel} /></div>
            <KeyValueGrid items={[
              { label: 'KYC Status', value: kycLabel },
              { label: 'Date of Birth', value: formatDate(k.birthday) },
              { label: 'Gender', value: formatGender(k.gender, k.genderLabel) },
              { label: 'ID Type', value: formatIdType(k.idType, k.idTypeLabel) },
              { label: 'ID Number', value: <span className="mono">{String(field(k.idNumber))}</span> },
              { label: 'KRA PIN', value: <span className="mono">{String(field(k.kraPin))}</span> },
              { label: 'Address', value: String(field(k.address)) },
              { label: 'Choice Onboarding ID', value: <span className="mono">{String(field(k.choiceOnboardingRequestId))}</span> },
              { label: 'Choice User ID', value: <span className="mono">{String(field(k.choiceUserId))}</span> },
              { label: 'KYC Submitted', value: formatDateTime(k.kycSubmittedAt ?? k.createdAt) },
            ]} />
            {k.rejectionReasonMsgs ? (
              <div style={{ marginTop: 12, padding: 12, borderRadius: 8, background: 'rgba(255,61,87,.08)', border: '1px solid rgba(255,61,87,.2)' }}>
                <div className="kpi-label" style={{ color: 'var(--danger)' }}>Rejection reason</div>
                <div style={{ fontSize: 13 }}>{String(k.rejectionReasonMsgs)}</div>
              </div>
            ) : null}
          </div>

          <div className="card">
            <div className="section-hdr"><span className="section-title">Wallet & Banking</span><StatusBadge status={c.walletStatus} /></div>
            <div style={{ fontSize: 28, fontWeight: 800, marginBottom: 12 }} className="mono">{formatKes(c.walletBalance)}</div>
            <KeyValueGrid items={[
              { label: 'Wallet Status', value: <StatusBadge status={c.walletStatus} /> },
              { label: 'Currency', value: String(field(c.walletCurrency, 'KES')) },
              { label: 'Choice Account ID', value: <span className="mono">{String(field(c.choiceAccountId))}</span> },
              { label: 'Account Type', value: String(field(c.choiceAccountType)) },
              { label: 'Balance Updated', value: formatDateTime(c.walletBalanceUpdatedAt) },
            ]} />
            {c.walletId ? <div style={{ marginTop: 12 }}><EntityLink href={`/wallets/${c.walletId}`}>View wallet →</EntityLink></div> : null}
          </div>

          <div className="card">
            <div className="section-title" style={{ marginBottom: 14 }}>Documents</div>
            <KeyValueGrid items={[
              { label: 'ID Front', value: k.idFrontUrl ? <a href={String(k.idFrontUrl)} target="_blank" rel="noreferrer">View document</a> : 'Not stored locally' },
              { label: 'Selfie', value: k.selfieUrl ? <a href={String(k.selfieUrl)} target="_blank" rel="noreferrer">View document</a> : 'Not stored locally' },
            ]} />
            <p className="muted" style={{ fontSize: 12, marginTop: 12 }}>
              ID photos are submitted to Choice Bank during onboarding. Local copies appear here when document URLs are stored.
            </p>
          </div>
        </div>
      ) : null}

      {tab === 'transactions' && tx.data ? (
        <DataTable columns={txColumns} rows={(tx.data as { content?: Record<string, unknown>[] }).content ?? []} />
      ) : null}

      {tab === 'kyc' && kyc.data && Object.keys(kyc.data).length > 0 ? (
        <div className="card">
          <KeyValueGrid items={[
            { label: 'Status', value: formatKycStatus(kyc.data.status, kyc.data.kycStatusLabel) },
            { label: 'Choice Onboarding ID', value: <span className="mono">{String(field(kyc.data.choiceOnboardingRequestId))}</span> },
            { label: 'Choice User ID', value: <span className="mono">{String(field(kyc.data.choiceUserId))}</span> },
            { label: 'Choice Account ID', value: <span className="mono">{String(field(kyc.data.choiceAccountId))}</span> },
            { label: 'Full Name', value: formatFullName(c.firstName, kyc.data.middleName, c.lastName) },
            { label: 'Date of Birth', value: formatDate(kyc.data.birthday) },
            { label: 'Gender', value: formatGender(kyc.data.gender, kyc.data.genderLabel) },
            { label: 'ID Type', value: formatIdType(kyc.data.idType, kyc.data.idTypeLabel) },
            { label: 'ID Number', value: <span className="mono">{String(field(kyc.data.idNumber))}</span> },
            { label: 'KRA PIN', value: <span className="mono">{String(field(kyc.data.kraPin))}</span> },
            { label: 'Address', value: String(field(kyc.data.address)) },
            { label: 'Submitted', value: formatDateTime(kyc.data.createdAt) },
            { label: 'Last Updated', value: formatDateTime(kyc.data.updatedAt) },
          ]} />
        </div>
      ) : tab === 'kyc' ? (
        <div className="card muted">No KYC record found for this customer.</div>
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
