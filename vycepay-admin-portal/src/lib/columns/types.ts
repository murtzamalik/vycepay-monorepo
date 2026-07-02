export type Column<T = Record<string, unknown>> = {
  key: string
  label: string
  render?: (row: T) => React.ReactNode
  mono?: boolean
}
