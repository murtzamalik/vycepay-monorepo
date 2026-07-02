'use client'

export function FilterBar({
  search,
  onSearch,
  children,
}: {
  search?: string
  onSearch?: (value: string) => void
  children?: React.ReactNode
}) {
  return (
    <div className="filter-bar card">
      {onSearch ? (
        <input
          className="input input-sm"
          placeholder="Search..."
          defaultValue={search}
          onKeyDown={(e) => {
            if (e.key === 'Enter') onSearch((e.target as HTMLInputElement).value)
          }}
        />
      ) : null}
      {children}
    </div>
  )
}
