'use client'

import { FormEvent, useState } from 'react'
import { useRouter } from 'next/navigation'
import { apiFetch, errorMessage } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { PermissionGuard } from '@/components/shared/PermissionGuard'

export default function BulkSmsPage() {
  const router = useRouter()
  const [recipients, setRecipients] = useState('')
  const [message, setMessage] = useState('')
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    const phones = recipients
      .split(/[\s,;]+/)
      .map((s) => s.trim())
      .filter(Boolean)
    if (phones.length === 0) {
      setError('Enter at least one phone number')
      return
    }
    if (phones.length > 100) {
      setError('Maximum 100 recipients')
      return
    }
    setSubmitting(true)
    try {
      const result = await apiFetch<{ batchId?: string }>('/sms/bulk', {
        method: 'POST',
        body: JSON.stringify({ recipients: phones, message, reason }),
      })
      if (result?.batchId) {
        router.push(`/sms?batchId=${encodeURIComponent(result.batchId)}`)
      } else {
        router.push('/sms')
      }
    } catch (err) {
      setError(errorMessage(err, 'Failed to send bulk SMS'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <PermissionGuard permission="sms:bulk">
      <div className="grid">
        <PageHeader
          title="Bulk SMS"
          description="Send a plain SMS to a phone list (Kenya numbers, max 100). Format: 2547XXXXXXXX."
        />
        <form className="card grid" onSubmit={onSubmit} style={{ gap: 12, maxWidth: 640 }}>
          <label className="grid" style={{ gap: 4 }}>
            <span>Recipients (comma, space, or newline separated)</span>
            <textarea
              value={recipients}
              onChange={(e) => setRecipients(e.target.value)}
              rows={4}
              required
              placeholder="254712345678&#10;0712345678"
            />
          </label>
          <label className="grid" style={{ gap: 4 }}>
            <span>Message</span>
            <textarea
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              rows={4}
              maxLength={640}
              required
            />
          </label>
          <label className="grid" style={{ gap: 4 }}>
            <span>Audit reason</span>
            <input
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              minLength={10}
              maxLength={512}
              required
            />
          </label>
          {error ? <div className="error">{error}</div> : null}
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn" type="submit" disabled={submitting}>
              {submitting ? 'Sending…' : 'Send bulk SMS'}
            </button>
            <button className="btn secondary" type="button" onClick={() => router.push('/sms')}>
              Cancel
            </button>
          </div>
        </form>
      </div>
    </PermissionGuard>
  )
}
