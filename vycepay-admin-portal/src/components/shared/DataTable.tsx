export function DataTable({ rows }: { rows: Record<string, unknown>[] }) {
  if (!rows.length) return <div className="card muted">No records found.</div>
  const columns = Object.keys(rows[0]).slice(0, 8)
  return <div className="table-wrap"><table><thead><tr>{columns.map((col) => <th key={col}>{col}</th>)}</tr></thead><tbody>{rows.map((row, idx) => <tr key={idx}>{columns.map((col) => <td key={col}>{format(row[col])}</td>)}</tr>)}</tbody></table></div>
}
function format(value: unknown) { if (value === null || value === undefined) return '-'; if (typeof value === 'boolean') return value ? 'Yes' : 'No'; if (typeof value === 'object') return JSON.stringify(value); return String(value) }
