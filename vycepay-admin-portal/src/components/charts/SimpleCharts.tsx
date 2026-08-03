'use client'

import type { ReactNode } from 'react'

type Point = Record<string, unknown>

const CHART_COLORS = ['#4f79ff', '#0fd67c', '#f5a623', '#ff3d57']

function shortLabel(v: unknown): string {
  const s = String(v ?? '')
  if (s.length >= 10 && s.includes('-')) return s.slice(5) // MM-DD
  return s.length > 10 ? s.slice(0, 8) : s
}

export function SimpleLineChart({
  data,
  xKey,
  lines,
}: {
  data: Point[]
  xKey: string
  lines: { key: string; color: string; label?: string }[]
}) {
  if (!data.length) return <div className="chart-empty muted">No volume data for this range</div>
  const width = 640
  const height = 220
  const padL = 44
  const padR = 16
  const padT = 16
  const padB = 36
  const maxY = Math.max(...lines.flatMap((l) => data.map((d) => Number(d[l.key] ?? 0))), 1)
  const plotW = width - padL - padR
  const plotH = height - padT - padB
  const stepX = data.length <= 1 ? plotW : plotW / (data.length - 1)
  const ticks = 4

  return (
    <div className="chart-wrap">
      <svg viewBox={`0 0 ${width} ${height}`} width="100%" height="100%" role="img" aria-label="Line chart">
        {Array.from({ length: ticks + 1 }, (_, i) => {
          const y = padT + (plotH * i) / ticks
          const val = maxY * (1 - i / ticks)
          return (
            <g key={i}>
              <line x1={padL} y1={y} x2={width - padR} y2={y} stroke="var(--border)" strokeWidth="1" />
              <text x={padL - 8} y={y + 4} textAnchor="end" className="chart-axis-text" fill="var(--text-muted)" fontSize="10">
                {val >= 1000 ? `${(val / 1000).toFixed(0)}k` : Math.round(val)}
              </text>
            </g>
          )
        })}
        {lines.map((line) => {
          const points = data.map((d, i) => {
            const x = padL + i * stepX
            const y = padT + plotH - (Number(d[line.key] ?? 0) / maxY) * plotH
            return `${x},${y}`
          }).join(' ')
          return <polyline key={line.key} fill="none" stroke={line.color} strokeWidth="2.5" strokeLinejoin="round" points={points} />
        })}
        {data.map((d, i) => {
          if (data.length > 14 && i % Math.ceil(data.length / 7) !== 0 && i !== data.length - 1) return null
          const x = padL + i * stepX
          return (
            <text key={i} x={x} y={height - 10} textAnchor="middle" fill="var(--text-muted)" fontSize="10">
              {shortLabel(d[xKey])}
            </text>
          )
        })}
      </svg>
      {lines.some((l) => l.label) ? (
        <div className="chart-legend">
          {lines.map((l) => (
            <span key={l.key} className="chart-legend-item">
              <span className="chart-legend-swatch" style={{ background: l.color }} />
              {l.label ?? l.key}
            </span>
          ))}
        </div>
      ) : null}
    </div>
  )
}

export function SimpleBarChart({
  data,
  xKey,
  yKey,
  color = CHART_COLORS[0],
}: {
  data: Point[]
  xKey: string
  yKey: string
  color?: string
}) {
  if (!data.length) return <div className="chart-empty muted">No KYC status data</div>
  const width = 640
  const height = 200
  const padL = 44
  const padR = 16
  const padT = 16
  const padB = 40
  const maxY = Math.max(...data.map((d) => Number(d[yKey] ?? 0)), 1)
  const plotW = width - padL - padR
  const plotH = height - padT - padB
  const barW = Math.max(12, plotW / data.length - 10)

  return (
    <div className="chart-wrap">
      <svg viewBox={`0 0 ${width} ${height}`} width="100%" height="100%" role="img" aria-label="Bar chart">
        {data.map((d, i) => {
          const h = (Number(d[yKey] ?? 0) / maxY) * plotH
          const x = padL + i * (plotW / data.length) + (plotW / data.length - barW) / 2
          const y = padT + plotH - h
          return (
            <g key={i}>
              <rect x={x} y={y} width={barW} height={Math.max(h, 2)} fill={color} rx="4" />
              <text x={x + barW / 2} y={height - 14} textAnchor="middle" fill="var(--text-muted)" fontSize="10">
                {shortLabel(d[xKey])}
              </text>
              <text x={x + barW / 2} y={y - 4} textAnchor="middle" fill="var(--text-secondary)" fontSize="10">
                {Number(d[yKey] ?? 0).toLocaleString()}
              </text>
            </g>
          )
        })}
      </svg>
    </div>
  )
}

export function SimplePieChart({ data }: { data: { name: string; value: number }[] }) {
  const total = data.reduce((s, d) => s + d.value, 0)
  if (!total) return <div className="chart-empty muted">No transaction type data</div>
  const r = 58
  const cx = 90
  const cy = 90
  const slices: ReactNode[] = []
  let angle = -Math.PI / 2
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
  return (
    <div className="chart-wrap pie-wrap">
      <svg viewBox="0 0 180 180" width="140" height="140" role="img" aria-label="Pie chart">{slices}</svg>
      <div className="chart-legend pie-legend">
        {data.map((d, i) => (
          <div key={d.name} className="chart-legend-item">
            <span className="chart-legend-swatch" style={{ background: CHART_COLORS[i % CHART_COLORS.length] }} />
            <span>{d.name}</span>
            <span className="mono muted">{d.value.toLocaleString()}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
