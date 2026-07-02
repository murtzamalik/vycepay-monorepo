export function KeyValueGrid({ items }: { items: { label: string; value: React.ReactNode }[] }) {
  return (
    <div className="kv-grid">
      {items.map((item) => (
        <div className="kv-row" key={item.label}>
          <span className="kv-label">{item.label}</span>
          <span className="kv-value">{item.value}</span>
        </div>
      ))}
    </div>
  )
}
