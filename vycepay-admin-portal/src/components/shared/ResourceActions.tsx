'use client'

import { FormEvent, useState } from 'react'
import { useRouter } from 'next/navigation'
import { apiFetch } from '@/lib/api'
import { ConfirmDialog } from './ConfirmDialog'
import { PermissionGuard } from './PermissionGuard'

export function CustomerActions({ id, status }: { id: string; status?: unknown }) {
  const router = useRouter()
  const nextStatus = status === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED'
  return <PermissionGuard permission="customer:suspend"><ConfirmDialog title={`${nextStatus === 'ACTIVE' ? 'Reactivate' : 'Suspend'} customer`} description="This changes the customer's ability to transact and is written to the admin audit log." actionLabel={nextStatus === 'ACTIVE' ? 'Reactivate customer' : 'Suspend customer'} danger={nextStatus !== 'ACTIVE'} onConfirm={async (reason) => { await apiFetch(`/customers/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status: nextStatus, reason }) }); router.refresh() }} /></PermissionGuard>
}

export function WalletActions({ id, status }: { id: string; status?: unknown }) {
  const router = useRouter()
  const nextStatus = status === 'FROZEN' ? 'ACTIVE' : 'FROZEN'
  return <PermissionGuard permission="wallet:freeze"><ConfirmDialog title={`${nextStatus === 'ACTIVE' ? 'Unfreeze' : 'Freeze'} wallet`} description="This controls wallet transaction access and requires an audit reason." actionLabel={nextStatus === 'ACTIVE' ? 'Unfreeze wallet' : 'Freeze wallet'} danger={nextStatus !== 'ACTIVE'} onConfirm={async (reason) => { await apiFetch(`/wallets/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status: nextStatus, reason }) }); router.refresh() }} /></PermissionGuard>
}

export function CallbackActions({ id }: { id: string }) {
  const router = useRouter()
  return <PermissionGuard permission="callback:retry"><ConfirmDialog title="Retry callback" description="Queues this Choice Bank callback for processing again and records the reason." actionLabel="Retry callback" onConfirm={async (reason) => { await apiFetch(`/callbacks/${id}/retry`, { method: 'POST', body: JSON.stringify({ reason }) }); router.refresh() }} /></PermissionGuard>
}

export function AdminPasswordResetAction({ id }: { id: string }) {
  const router = useRouter()
  const [open, setOpen] = useState(false)
  const [error, setError] = useState('')
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    const form = new FormData(event.currentTarget)
    try {
      await apiFetch(`/users/${id}/reset-password`, { method: 'POST', body: JSON.stringify({ newPassword: form.get('newPassword'), reason: form.get('reason') }) })
      setOpen(false)
      router.refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Password reset failed')
    }
  }
  return <PermissionGuard permission="admin:manage_users"><button className="btn danger" onClick={() => setOpen(true)}>Reset password</button>{open ? <div className="modal-backdrop" role="dialog" aria-modal="true" aria-label="Reset admin password"><form className="modal card grid" onSubmit={submit}><div><h2>Reset admin password</h2><p className="muted">This revokes existing admin sessions and requires an audit reason.</p></div>{error ? <div className="error">{error}</div> : null}<input className="input" name="newPassword" type="password" placeholder="New temporary password" required /><textarea className="input" name="reason" placeholder="Reason for password reset" required rows={4} /><div className="actions"><button className="btn danger">Reset password</button><button className="btn secondary" type="button" onClick={() => setOpen(false)}>Cancel</button></div></form></div> : null}</PermissionGuard>
}
