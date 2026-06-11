'use client'

import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api'

export function DetailPage({ title, endpoint, actions }: { title: string; endpoint: (id: string) => string; actions?: (id: string, data?: Record<string, unknown>) => React.ReactNode }) {
  const params = useParams<{ id: string }>()
  const { data, isLoading, error } = useQuery({ queryKey: [title, params.id], queryFn: () => apiFetch<Record<string, unknown>>(endpoint(params.id)) })
  return <div className="grid"><div className="actions" style={{ justifyContent: 'space-between' }}><h1>{title}</h1><div className="actions">{actions?.(params.id, data)}</div></div>{isLoading ? <div className="card muted">Loading...</div> : null}{error ? <div className="error">Unable to load detail.</div> : null}{data ? <pre className="card" style={{ overflow: 'auto' }}>{JSON.stringify(data, null, 2)}</pre> : null}</div>
}
