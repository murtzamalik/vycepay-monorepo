'use client'

import { FormEvent, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch, asRows } from '@/lib/api'
import { DataTable } from '@/components/shared/DataTable'
import { PermissionGuard } from '@/components/shared/PermissionGuard'

export function AdminUserManagement() {
  const queryClient = useQueryClient()
  const [error, setError] = useState('')
  const users = useQuery({ queryKey: ['admin-users'], queryFn: () => apiFetch<unknown>('/users') })
  const create = useMutation({
    mutationFn: (body: unknown) => apiFetch('/users', { method: 'POST', body: JSON.stringify(body) }),
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['admin-users'] }) },
  })

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    const form = new FormData(event.currentTarget)
    try {
      await create.mutateAsync({
        username: form.get('username'),
        email: form.get('email'),
        fullName: form.get('fullName'),
        password: form.get('password'),
        roleIds: String(form.get('roleIds') || '').split(',').map((value) => Number(value.trim())).filter(Boolean),
        reason: form.get('reason'),
      })
      event.currentTarget.reset()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to create admin user')
    }
  }

  return <div className="grid"><div><h1>Admin Users</h1><p className="muted">Manage backoffice operator accounts and role assignments.</p></div><PermissionGuard permission="admin:manage_users" fallback={<div className="card muted">Permission denied.</div>}><form className="card grid" onSubmit={submit}><h2>Create admin user</h2>{error ? <div className="error">{error}</div> : null}<input className="input" name="username" placeholder="Username" required /><input className="input" name="email" type="email" placeholder="Email" required /><input className="input" name="fullName" placeholder="Full name" required /><input className="input" name="password" type="password" placeholder="Temporary password" required /><input className="input" name="roleIds" placeholder="Role IDs, comma-separated" required /><textarea className="input" name="reason" placeholder="Reason for creating this admin" required rows={3} /><button className="btn" disabled={create.isPending}>{create.isPending ? 'Creating...' : 'Create admin user'}</button></form></PermissionGuard>{users.isLoading ? <div className="card muted">Loading...</div> : null}{users.error ? <div className="error">Unable to load admin users.</div> : null}{users.data ? <DataTable rows={asRows(users.data)} /> : null}</div>
}
