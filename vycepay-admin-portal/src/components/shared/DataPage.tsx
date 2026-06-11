'use client'

import { useQuery } from '@tanstack/react-query'
import { apiFetch, asRows } from '@/lib/api'
import { DataTable } from './DataTable'

export function DataPage({ title, description, endpoint, exportPath }: { title: string; description: string; endpoint: string; exportPath?: string }) {
  const { data, error, isLoading } = useQuery({ queryKey: [endpoint], queryFn: () => apiFetch<unknown>(endpoint) })
  return <div className="grid"><div className="actions" style={{ justifyContent: 'space-between' }}><div><h1>{title}</h1><p className="muted">{description}</p></div>{exportPath ? <a className="btn secondary" href={exportPath}>Export CSV</a> : null}</div>{isLoading ? <div className="card muted">Loading...</div> : null}{error ? <div className="error">Unable to load data.</div> : null}{data ? <DataTable rows={asRows(data)} /> : null}</div>
}
