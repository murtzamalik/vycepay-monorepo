'use client'

import { useState } from 'react'

export function ActionPage({ title, description }: { title: string; description: string }) {
  const [reason, setReason] = useState('')
  return <div className="grid"><h1>{title}</h1><p className="muted">{description}</p><div className="card"><label>Reason</label><textarea className="input" value={reason} onChange={(e) => setReason(e.target.value)} rows={4} placeholder="Required for high-risk admin actions" /><p className="muted">Use the detail screen action buttons once connected to a selected resource.</p></div></div>
}
