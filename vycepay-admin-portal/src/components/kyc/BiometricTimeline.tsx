'use client'

import { Fragment, useState } from 'react'
import { formatDateTime } from '@/lib/format'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { JsonViewer } from '@/components/detail/JsonViewer'

type Attempt = {
  attemptId?: string
  step?: string
  attemptNo?: number
  outcome?: string
  livenessResult?: string
  confidenceScore?: number
  matchResult?: boolean
  similarity?: number
  failCode?: string
  failMessage?: string
  latencyMs?: number
  createdAt?: string
  responseJson?: unknown
}

function detail(a: Attempt): string {
  const parts: string[] = []
  if (a.livenessResult) parts.push(String(a.livenessResult))
  if (a.confidenceScore != null) parts.push(String(a.confidenceScore))
  if (a.similarity != null) parts.push(`${a.similarity}%`)
  if (a.failCode) parts.push(String(a.failCode))
  return parts.join(' · ') || '—'
}

function safeParse(raw: string) {
  try { return JSON.parse(raw) } catch { return raw }
}

export function BiometricTimeline({ attempts }: { attempts?: Attempt[] | null }) {
  const [open, setOpen] = useState<string | null>(null)
  if (!attempts?.length) {
    return <div className="card muted">No biometric attempts recorded.</div>
  }
  return (
    <div className="card">
      <div className="section-title" style={{ marginBottom: 14 }}>Biometric timeline</div>
      <div style={{ overflowX: 'auto' }}>
        <table className="data-table" style={{ width: '100%', fontSize: 13 }}>
          <thead>
            <tr>
              <th>Step</th>
              <th>Outcome</th>
              <th>Detail</th>
              <th>Attempt</th>
              <th>When</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {attempts.map((a) => {
              const id = String(a.attemptId ?? `${a.step}-${a.attemptNo}`)
              return (
                <Fragment key={id}>
                  <tr>
                    <td className="mono">{a.step}</td>
                    <td><StatusBadge status={a.outcome} /></td>
                    <td>{detail(a)}</td>
                    <td className="mono">{a.attemptNo}</td>
                    <td>{formatDateTime(a.createdAt)}</td>
                    <td>
                      <button type="button" className="btn secondary btn-sm" onClick={() => setOpen(open === id ? null : id)}>
                        {open === id ? 'Hide' : 'JSON'}
                      </button>
                    </td>
                  </tr>
                  {open === id ? (
                    <tr>
                      <td colSpan={6}>
                        {a.failMessage ? <div style={{ marginBottom: 8 }}>{a.failMessage}</div> : null}
                        <JsonViewer data={typeof a.responseJson === 'string' ? safeParse(a.responseJson) : a.responseJson} />
                      </td>
                    </tr>
                  ) : null}
                </Fragment>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
