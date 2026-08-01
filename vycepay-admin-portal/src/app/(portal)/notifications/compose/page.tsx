'use client'

import { FormEvent, useState } from 'react'
import { useRouter } from 'next/navigation'
import { apiFetch, errorMessage } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { PermissionGuard } from '@/components/shared/PermissionGuard'

export default function ComposeNotificationPage() {
  const router = useRouter()
  const [customerIds, setCustomerIds] = useState('')
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    const ids = customerIds
      .split(/[\s,]+/)
      .map((s) => s.trim())
      .filter(Boolean)
      .map((s) => Number(s))
      .filter((n) => Number.isFinite(n) && n > 0)
    if (ids.length === 0) {
      setError('Enter at least one internal customer id')
      return
    }
    if (ids.length > 100) {
      setError('Maximum 100 recipients')
      return
    }
    setSubmitting(true)
    try {
      const result = await apiFetch<{ batchId?: string }>('/notifications/compose', {
        method: 'POST',
        body: JSON.stringify({ customerIds: ids, title, body, reason }),
      })
      if (result?.batchId) {
        router.push(`/notifications?batchId=${encodeURIComponent(result.batchId)}`)
      } else {
        router.push('/notifications')
      }
    } catch (err) {
      setError(errorMessage(err, 'Failed to compose notification'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <PermissionGuard permission="notification:compose">
      <div className="grid">
        <PageHeader title="Compose notification" description="Send a custom message to one or more customers (max 100)." />
        <form className="card grid" onSubmit={onSubmit} style={{ gap: 12, maxWidth: 640 }}>
          <label className="grid" style={{ gap: 4 }}>
            <span>Customer IDs (internal, comma or space separated)</span>
            <textarea value={customerIds} onChange={(e) => setCustomerIds(e.target.value)} rows={3} required />
          </label>
          <label className="grid" style={{ gap: 4 }}>
            <span>Title</span>
            <input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={128} required />
          </label>
          <label className="grid" style={{ gap: 4 }}>
            <span>Body</span>
            <textarea value={body} onChange={(e) => setBody(e.target.value)} rows={4} maxLength={512} required />
          </label>
          <label className="grid" style={{ gap: 4 }}>
            <span>Audit reason</span>
            <input value={reason} onChange={(e) => setReason(e.target.value)} minLength={10} maxLength={512} required />
          </label>
          {error ? <div className="error">{error}</div> : null}
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn" type="submit" disabled={submitting}>{submitting ? 'Sending…' : 'Send'}</button>
            <button className="btn secondary" type="button" onClick={() => router.push('/notifications')}>Cancel</button>
          </div>
        </form>
      </div>
    </PermissionGuard>
  )
}
