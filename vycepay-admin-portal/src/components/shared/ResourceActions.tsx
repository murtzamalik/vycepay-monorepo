'use client'

import { FormEvent, useState } from 'react'
import { useRouter } from 'next/navigation'
import { useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api'
import { ConfirmDialog } from './ConfirmDialog'
import { PermissionGuard } from './PermissionGuard'

function useMutationRefresh(keys: string[][]) {
  const router = useRouter()
  const queryClient = useQueryClient()
  return async () => {
    await Promise.all(keys.map((key) => queryClient.invalidateQueries({ queryKey: key })))
    router.refresh()
  }
}

export function CustomerActions({ id, status }: { id: string; status?: unknown }) {
  const nextStatus = status === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED'
  const refresh = useMutationRefresh([['customer', id], ['customer-summary', id], ['customers']])
  return (
    <PermissionGuard permission="customer:suspend">
      <ConfirmDialog
        title={`${nextStatus === 'ACTIVE' ? 'Reactivate' : 'Suspend'} customer`}
        description="This changes the customer's ability to transact and is written to the admin audit log."
        actionLabel={nextStatus === 'ACTIVE' ? 'Reactivate customer' : 'Suspend customer'}
        danger={nextStatus !== 'ACTIVE'}
        onConfirm={async (reason) => {
          await apiFetch(`/customers/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status: nextStatus, reason }) })
          await refresh()
        }}
      />
    </PermissionGuard>
  )
}

export function WalletActions({ id, status }: { id: string; status?: unknown }) {
  const nextStatus = status === 'FROZEN' ? 'ACTIVE' : 'FROZEN'
  const refresh = useMutationRefresh([['wallet', id], ['wallets']])
  return (
    <PermissionGuard permission="wallet:freeze">
      <ConfirmDialog
        title={`${nextStatus === 'ACTIVE' ? 'Unfreeze' : 'Freeze'} wallet`}
        description="This controls wallet transaction access and requires an audit reason."
        actionLabel={nextStatus === 'ACTIVE' ? 'Unfreeze wallet' : 'Freeze wallet'}
        danger={nextStatus !== 'ACTIVE'}
        onConfirm={async (reason) => {
          await apiFetch(`/wallets/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status: nextStatus, reason }) })
          await refresh()
        }}
      />
    </PermissionGuard>
  )
}

export function CallbackActions({ id, processed }: { id: string; processed?: boolean }) {
  const refresh = useMutationRefresh([['callback', id], ['callbacks']])
  return (
    <PermissionGuard permission="callback:retry">
      <ConfirmDialog
        title="Retry callback"
        description={processed
          ? 'Re-queues this callback for processing. Use when a previously processed callback needs to run again.'
          : 'Queues this Choice Bank callback for processing again and records the reason.'}
        actionLabel="Retry callback"
        onConfirm={async (reason) => {
          await apiFetch(`/callbacks/${id}/retry`, { method: 'POST', body: JSON.stringify({ reason }) })
          await refresh()
        }}
      />
    </PermissionGuard>
  )
}

export function AdminPasswordResetAction({ id }: { id: string }) {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [error, setError] = useState('')
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    const form = new FormData(event.currentTarget)
    try {
      await apiFetch(`/users/${id}/reset-password`, { method: 'POST', body: JSON.stringify({ newPassword: form.get('newPassword'), reason: form.get('reason') }) })
      setOpen(false)
      await queryClient.invalidateQueries({ queryKey: ['admin-user', id] })
      router.refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Password reset failed')
    }
  }
  return <PermissionGuard permission="admin:manage_users"><button className="btn danger" onClick={() => setOpen(true)}>Reset password</button>{open ? <div className="modal-backdrop" role="dialog" aria-modal="true" aria-label="Reset admin password"><form className="modal card grid" onSubmit={submit}><div><h2>Reset admin password</h2><p className="muted">This revokes existing admin sessions and requires an audit reason.</p></div>{error ? <div className="error">{error}</div> : null}<input className="input" name="newPassword" type="password" placeholder="New temporary password" required /><textarea className="input" name="reason" placeholder="Reason for password reset" required rows={4} /><div className="actions"><button className="btn danger">Reset password</button><button className="btn secondary" type="button" onClick={() => setOpen(false)}>Cancel</button></div></form></div> : null}</PermissionGuard>
}
