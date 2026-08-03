export type ColumnFilter = {
  type: 'select' | 'text'
  param: string
  options?: { value: string; label: string }[]
  placeholder?: string
}

export type Column<T = Record<string, unknown>> = {
  key: string
  label: string
  render?: (row: T) => React.ReactNode
  mono?: boolean
  /** When true, header is clickable and sends sort/order query params (or client-sorts when enabled). */
  sortable?: boolean
  /** Per-column filter control rendered in the filter bar. */
  filter?: ColumnFilter
}
