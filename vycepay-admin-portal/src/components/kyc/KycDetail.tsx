'use client'

import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch, errorMessage } from '@/lib/api'
import { formatDateTime } from '@/lib/format'
import { DetailLayout } from '@/components/detail/DetailLayout'
import { KeyValueGrid } from '@/components/detail/KeyValueGrid'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { EntityLink } from '@/components/ui/EntityLink'
import { ErrorState, SkeletonTable } from '@/components/ui/States'
import { DocumentGallery } from '@/components/kyc/DocumentGallery'
import { BiometricTimeline } from '@/components/kyc/BiometricTimeline'

export function KycDetail() {
  const params = useParams<{ id: string }>()
  const { data, isLoading, error } = useQuery({
    queryKey: ['kyc', params.id],
    queryFn: () => apiFetch<Record<string, unknown>>(`/kyc/${params.id}`),
  })
  if (isLoading) return <SkeletonTable />
  if (error || !data) return <ErrorState message={errorMessage(error, 'Unable to load KYC record.')} />

  const name = `${data.firstName ?? data.first_name ?? ''} ${data.lastName ?? data.last_name ?? ''}`.trim()
  const biometric = (data.biometric ?? null) as Record<string, unknown> | null
  const documents = data.documents as Record<string, string | null> | null | undefined
  // API always sets documents: object | null (null = no kyc:view_documents)
  const docsRestricted = data.documents == null

  return (
    <DetailLayout
      header={
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'center' }}>
          <h2 style={{ margin: 0 }}>KYC — {name || `#${params.id}`}</h2>
          <StatusBadge status={data.status} />
          {biometric?.status ? <StatusBadge status={String(biometric.status)} /> : null}
        </div>
      }
    >
      <div className="card">
        <div className="section-title" style={{ marginBottom: 14 }}>Summary</div>
        <KeyValueGrid
          items={[
            { label: 'Customer', value: <EntityLink href={`/customers/${data.customerExternalId}`}>{String(data.customerExternalId)}</EntityLink> },
            { label: 'Choice Onboarding ID', value: <span className="mono">{String(data.choice_onboarding_request_id ?? data.choiceOnboardingRequestId ?? '—')}</span> },
            { label: 'Choice User ID', value: <span className="mono">{String(data.choice_user_id ?? '—')}</span> },
            { label: 'ID Type', value: String(data.id_type ?? data.idType ?? '—') },
            { label: 'ID Number', value: String(data.id_number ?? '—') },
            { label: 'Status', value: <StatusBadge status={data.status} /> },
            { label: 'Submitted', value: formatDateTime(data.created_at ?? data.createdAt) },
            { label: 'Biometric session', value: <span className="mono">{String(biometric?.sessionId ?? '—')}</span> },
            { label: 'Biometric status', value: biometric?.status ? <StatusBadge status={String(biometric.status)} /> : '—' },
            { label: 'Face match', value: biometric?.faceMatch == null ? '—' : String(biometric.faceMatch) },
            { label: 'Similarity', value: biometric?.faceSimilarity != null ? String(biometric.faceSimilarity) : '—' },
            { label: 'Fail code', value: String(biometric?.failReasonCode ?? '—') },
            { label: 'Fail message', value: String(biometric?.failReasonMsg ?? '—') },
          ]}
        />
      </div>

      {!biometric ? (
        <div className="card muted" style={{ marginTop: 12 }}>
          No biometric session (pre-cutover or legacy submit). Choice-only record.
        </div>
      ) : null}

      <div style={{ marginTop: 12 }}>
        <DocumentGallery documents={docsRestricted ? null : documents} restricted={docsRestricted} />
      </div>

      <div style={{ marginTop: 12 }}>
        <BiometricTimeline attempts={(biometric?.attempts as never) ?? []} />
      </div>
    </DetailLayout>
  )
}
