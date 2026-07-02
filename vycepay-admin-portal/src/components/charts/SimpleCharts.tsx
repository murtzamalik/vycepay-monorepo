'use client'

import type { ReactNode } from 'react'

type Point = Record<string, unknown>

const CHART_COLORS = ['#4f79ff', '#0fd67c', '#f5a623', '#ff3d57']

export function SimpleLineChart({ data, xKey, lines }: { data: Point[]; xKey: string; lines: { key: string; color: string }[] }) {
  if (!data.length) return <div className="muted">No data</div>
  const width = 600
  const height = 200
  const pad = 30
  const maxY = Math.max(...lines.flatMap((l) => data.map((d) => Number(d[l.key] ?? 0))), 1)
  const stepX = (width - pad * 2) / Math.max(data.length - 1, 1)

  return (
    <svg viewBox={`0 0 ${width} ${height}`} width="100%" height="100%">
      {lines.map((line) => {
        const points = data.map((d, i) => {
          const x = pad + i * stepX
          const y = height - pad - (Number(d[line.key] ?? 0) / maxY) * (height - pad * 2)
          return `${x},${y}`
        }).join(' ')
        return <polyline key={line.key} fill="none" stroke={line.color} strokeWidth="2" points={points} />
      })}
    </svg>
  )
}

export function SimpleBarChart({ data, xKey, yKey, color = CHART_COLORS[0] }: { data: Point[]; xKey: string; yKey: string; color?: string }) {
  if (!data.length) return <div className="muted">No data</div>
  const width = 600
  const height = 200
  const pad = 30
  const maxY = Math.max(...data.map((d) => Number(d[yKey] ?? 0)), 1)
  const barW = (width - pad * 2) / data.length - 4

  return (
    <svg viewBox={`0 0 ${width} ${height}`} width="100%" height="100%">
      {data.map((d, i) => {
        const h = (Number(d[yKey] ?? 0) / maxY) * (height - pad * 2)
        const x = pad + i * (barW + 4)
        const y = height - pad - h
        return <rect key={i} x={x} y={y} width={barW} height={h} fill={color} rx="3" />
      })}
    </svg>
  )
}

export function SimplePieChart({ data }: { data: { name: string; value: number }[] }) {
  const total = data.reduce((s, d) => s + d.value, 0) || 1
  const r = 70
  const cx = 100
  const cy = 100
  const slices: ReactNode[] = []
  let angle = 0
  for (let i = 0; i < data.length; i++) {
    const d = data[i]
    const slice = (d.value / total) * Math.PI * 2
    const x1 = cx + r * Math.cos(angle)
    const y1 = cy + r * Math.sin(angle)
    angle += slice
    const x2 = cx + r * Math.cos(angle)
    const y2 = cy + r * Math.sin(angle)
    const large = slice > Math.PI ? 1 : 0
    slices.push(
      <path key={d.name} d={`M ${cx} ${cy} L ${x1} ${y1} A ${r} ${r} 0 ${large} 1 ${x2} ${y2} Z`} fill={CHART_COLORS[i % CHART_COLORS.length]} />
    )
  }
  return <svg viewBox="0 0 200 200" width="100%" height="100%">{slices}</svg>
}
