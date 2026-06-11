'use client'

import { useQuery } from '@tanstack/react-query'
import { apiFetch, asRows } from '@/lib/api'
import { DataTable } from '@/components/shared/DataTable'

export function Dashboard() {
  const summary = useQuery({ queryKey: ['dashboard-summary'], queryFn: () => apiFetch<Record<string, unknown>>('/dashboard/summary') })
  const recent = useQuery({ queryKey: ['recent-transactions'], queryFn: () => apiFetch<unknown>('/dashboard/recent-transactions') })
  return <div className="grid"><div><h1>Dashboard</h1><p className="muted">Operational overview, alerts, and recent transaction activity.</p></div>{summary.error ? <div className="error">Unable to load dashboard summary.</div> : null}<div className="grid kpi-grid">{Object.entries(summary.data || {}).map(([key, value]) => <div className="card" key={key}><div className="muted">{key}</div><h2>{String(value)}</h2></div>)}</div><div className="card"><h2>Recent Transactions</h2>{recent.data ? <DataTable rows={asRows(recent.data)} /> : <p className="muted">Loading...</p>}</div></div>
}
