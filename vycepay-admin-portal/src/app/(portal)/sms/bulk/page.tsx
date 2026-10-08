'use client'

import { FormEvent, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { apiFetch, errorMessage } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { PermissionGuard } from '@/components/shared/PermissionGuard'

type AudienceMode = 'MANUAL' | 'ALL_CUSTOMERS'

type AudiencePreview = {
  totalCustomers: number
  withValidMobile: number
  skippedInvalid: number
}

export default function BulkSmsPage() {
  const router = useRouter()
  const [mode, setMode] = useState<AudienceMode>('MANUAL')
  const [recipients, setRecipients] = useState('')
  const [message, setMessage] = useState('')
  const [reason, setReason] = useState('')
  const [confirmAll, setConfirmAll] = useState(false)
  const [preview, setPreview] = useState<AudiencePreview | null>(null)
  const [previewLoading, setPreviewLoading] = useState(false)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (mode !== 'ALL_CUSTOMERS') {
      setPreview(null)
      setConfirmAll(false)
      return
    }
    let cancelled = false
    setPreviewLoading(true)
    setError('')
    apiFetch<AudiencePreview>('/sms/bulk/audience-preview')
      .then((data) => {
        if (!cancelled) setPreview(data)
      })
      .catch((err) => {
        if (!cancelled) setError(errorMessage(err, 'Failed to load audience preview'))
      })
      .finally(() => {
        if (!cancelled) setPreviewLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [mode])

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')

    if (mode === 'ALL_CUSTOMERS') {
      if (!confirmAll) {
        setError('Confirm sending to all registered customers')
        return
      }
      if (preview && preview.withValidMobile === 0) {
        setError('No eligible customers with a valid mobile')
        return
      }
      setSubmitting(true)
      try {
        const result = await apiFetch<{ batchId?: string }>('/sms/bulk', {
          method: 'POST',
          body: JSON.stringify({ audience: 'ALL_CUSTOMERS', message, reason }),
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
      return
    }

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
        body: JSON.stringify({ audience: 'MANUAL', recipients: phones, message, reason }),
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
          description="Send a plain SMS to a phone list (max 100) or all registered customers (ACTIVE, PENDING, SUSPENDED — not DEACTIVATED). Kenya format: 2547XXXXXXXX."
        />
        <form className="card grid" onSubmit={onSubmit} style={{ gap: 12, maxWidth: 640 }}>
          <fieldset className="grid" style={{ gap: 8, border: 0, padding: 0, margin: 0 }}>
            <legend style={{ fontWeight: 600, marginBottom: 4 }}>Audience</legend>
            <label style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <input
                type="radio"
                name="audience"
                checked={mode === 'MANUAL'}
                onChange={() => setMode('MANUAL')}
              />
              <span>Phone list</span>
            </label>
            <label style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <input
                type="radio"
                name="audience"
                checked={mode === 'ALL_CUSTOMERS'}
                onChange={() => setMode('ALL_CUSTOMERS')}
              />
              <span>All registered customers</span>
            </label>
          </fieldset>

          {mode === 'MANUAL' ? (
            <label className="grid" style={{ gap: 4 }}>
              <span>Recipients (comma, space, or newline separated)</span>
              <textarea
                value={recipients}
                onChange={(e) => setRecipients(e.target.value)}
                rows={4}
                required
                placeholder={"254712345678\n0712345678"}
              />
            </label>
          ) : (
            <div className="grid" style={{ gap: 8 }}>
              {previewLoading ? (
                <p style={{ margin: 0, color: 'var(--muted, #6B7394)' }}>Loading audience…</p>
              ) : preview ? (
                <p style={{ margin: 0 }}>
                  Will send to <strong>{preview.withValidMobile}</strong> customers
                  {preview.skippedInvalid > 0
                    ? ` (${preview.skippedInvalid} skipped — invalid mobile; ${preview.totalCustomers} eligible rows)`
                    : ` (${preview.totalCustomers} eligible)`}
                  . Delivery runs in the background after submit.
                </p>
              ) : null}
              <label style={{ display: 'flex', gap: 8, alignItems: 'flex-start' }}>
                <input
                  type="checkbox"
                  checked={confirmAll}
                  onChange={(e) => setConfirmAll(e.target.checked)}
                  required
                />
                <span>I confirm sending to all registered customers</span>
              </label>
            </div>
          )}

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
            <button className="btn" type="submit" disabled={submitting || (mode === 'ALL_CUSTOMERS' && previewLoading)}>
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
