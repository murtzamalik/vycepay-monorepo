'use client'

import { FormEvent, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api'
import { ListPage } from '@/components/shared/ListPage'
import { EntityLink } from '@/components/ui/EntityLink'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { PermissionGuard } from '@/components/shared/PermissionGuard'
import { PageHeader } from '@/components/layout/PageHeader'
import { formatDateTime } from '@/lib/format'
import type { Column } from '@/lib/columns/types'

const columns: Column[] = [
  { key: 'username', label: 'Username', mono: true },
  { key: 'fullName', label: 'Name' },
  { key: 'email', label: 'Email' },
  { key: 'status', label: 'Status', render: (r) => <StatusBadge status={r.status} /> },
  { key: 'lastLoginAt', label: 'Last Login', render: (r) => formatDateTime(r.lastLoginAt) },
  {
    key: 'actions',
    label: '',
    render: (r) => (
      <EntityLink href={`/admin/users/${r.id}`}>
        <span className="btn secondary btn-sm">View</span>
      </EntityLink>
    ),
  },
]

function AdminUserCreateForm() {
  const queryClient = useQueryClient()
  const [error, setError] = useState('')
  const roles = useQuery({
    queryKey: ['roles'],
    queryFn: () => apiFetch<{ id: number; name: string }[]>('/roles'),
  })
  const create = useMutation({
    mutationFn: (body: unknown) => apiFetch('/users', { method: 'POST', body: JSON.stringify(body) }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['/users'] })
    },
  })

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    const form = new FormData(event.currentTarget)
    const roleIds = roles.data?.filter((r) => form.get(`role-${r.id}`) === 'on').map((r) => r.id) ?? []
    if (!roleIds.length) {
      setError('Select at least one role.')
      return
    }
    try {
      await create.mutateAsync({
        username: form.get('username'),
        email: form.get('email'),
        fullName: form.get('fullName'),
        password: form.get('password'),
        roleIds,
        reason: form.get('reason'),
      })
      event.currentTarget.reset()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to create admin user')
    }
  }

  return (
    <form className="card grid" onSubmit={submit}>
      <h2>Create admin user</h2>
      {error ? <div className="error">{error}</div> : null}
      <input className="input" name="username" placeholder="Username" required />
      <input className="input" name="email" type="email" placeholder="Email" required />
      <input className="input" name="fullName" placeholder="Full name" required />
      <input className="input" name="password" type="password" placeholder="Temporary password" required />
      <div className="section-title">Roles</div>
      <div className="grid">
        {roles.data?.map((role) => (
          <label key={role.id} style={{ display: 'flex', gap: 8 }}>
            <input type="checkbox" name={`role-${role.id}`} />
            <span>{role.name}</span>
          </label>
        ))}
      </div>
      <textarea className="input" name="reason" placeholder="Reason for creating this admin" required rows={3} />
      <button className="btn" disabled={create.isPending || roles.isLoading}>
        {create.isPending ? 'Creating...' : 'Create admin user'}
      </button>
    </form>
  )
}

export function AdminUserManagement() {
  return (
    <div className="grid">
      <PageHeader title="Admin Users" description="Manage backoffice operator accounts and role assignments." />
      <PermissionGuard permission="admin:manage_users" fallback={<div className="card muted">Permission denied.</div>}>
        <AdminUserCreateForm />
      </PermissionGuard>
      <ListPage
        title="Admin Users"
        description=""
        endpoint="/users"
        columns={columns}
        hideHeader
      />
    </div>
  )
}
