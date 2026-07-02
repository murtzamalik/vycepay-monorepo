'use client'

import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { KeyValueGrid } from '@/components/detail/KeyValueGrid'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { EntityLink } from '@/components/ui/EntityLink'
import { formatDateTime } from '@/lib/format'
import { ErrorState, SkeletonTable } from '@/components/ui/States'
import { AdminPasswordResetAction } from '@/components/shared/ResourceActions'

export function AdminUserDetail() {
  const params = useParams<{ id: string }>()
  const { data, isLoading, error } = useQuery({
    queryKey: ['admin-user', params.id],
    queryFn: () => apiFetch<Record<string, unknown>>(`/users/${params.id}`),
  })
  const roles = useQuery({
    queryKey: ['roles'],
    queryFn: () => apiFetch<{ id: number; name: string }[]>('/roles'),
  })

  if (isLoading) return <SkeletonTable />
  if (error || !data) return <ErrorState message="Unable to load admin user." />

  const roleIds = (data.roleIds as number[] | undefined) ?? []
  const assignedRoles = roles.data?.filter((r) => roleIds.includes(r.id)) ?? []

  return (
    <div className="grid">
      <PageHeader
        title={String(data.fullName ?? data.username)}
        description="Backoffice operator account."
        actions={<AdminPasswordResetAction id={params.id} />}
      />
      <div className="card">
        <KeyValueGrid items={[
          { label: 'Username', value: String(data.username) },
          { label: 'Email', value: String(data.email) },
          { label: 'Status', value: <StatusBadge status={data.status} /> },
          { label: 'Last Login', value: formatDateTime(data.lastLoginAt) },
          {
            label: 'Roles',
            value: assignedRoles.length ? (
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                {assignedRoles.map((role) => (
                  <EntityLink key={role.id} href={`/admin/roles/${role.id}`}>
                    <span className="badge badge-info">{role.name}</span>
                  </EntityLink>
                ))}
              </div>
            ) : (
              <span className="muted">No roles assigned</span>
            ),
          },
        ]} />
      </div>
    </div>
  )
}
