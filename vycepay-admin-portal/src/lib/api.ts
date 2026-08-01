export type ApiEnvelope<T> = { success: boolean; code: string; message: string; requestId?: string; data: T }

export type PaginatedResponse<T> = {
  content: T[]
  page: number
  size: number
  total: number
  totalPages: number
}

/** Thrown on non-OK API responses; preserves code/message/requestId for UI and support. */
export class ApiClientError extends Error {
  readonly code?: string
  readonly requestId?: string
  readonly status: number

  constructor(opts: { message: string; code?: string; requestId?: string; status: number }) {
    super(opts.message)
    this.name = 'ApiClientError'
    this.code = opts.code
    this.requestId = opts.requestId
    this.status = opts.status
  }

  displayMessage(): string {
    if (this.requestId) {
      return `${this.message} (Request ID: ${this.requestId})`
    }
    return this.message
  }
}

/** Resolves a customer-facing message from a thrown API or generic error. */
export function errorMessage(err: unknown, fallback = 'Request failed'): string {
  if (err instanceof ApiClientError) {
    return err.displayMessage()
  }
  if (err instanceof Error && err.message) {
    return err.message
  }
  return fallback
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path.startsWith('/api/') ? path : `/api/admin${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
    cache: 'no-store',
  })
  if (!response.ok) {
    const text = await response.text()
    let message = text || 'Request failed'
    let code: string | undefined
    let requestId: string | undefined
    try {
      const json = JSON.parse(text) as { message?: string; code?: string; requestId?: string }
      message = json.message || json.code || message
      code = json.code
      requestId = json.requestId
    } catch {
      message = text || message
    }
    throw new ApiClientError({ message, code, requestId, status: response.status })
  }
  const json = (await response.json()) as ApiEnvelope<T>
  return json.data
}

export function buildQuery(params: Record<string, string | number | boolean | undefined | null>): string {
  const q = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') q.set(key, String(value))
  })
  const s = q.toString()
  return s ? `?${s}` : ''
}

export function asRows(data: unknown): Record<string, unknown>[] {
  if (Array.isArray(data)) return data as Record<string, unknown>[]
  if (data && typeof data === 'object' && Array.isArray((data as { content?: unknown }).content)) {
    return (data as { content: Record<string, unknown>[] }).content
  }
  if (data && typeof data === 'object') return [data as Record<string, unknown>]
  return []
}

export function asPaginated<T = Record<string, unknown>>(data: unknown): PaginatedResponse<T> {
  if (data && typeof data === 'object' && 'content' in (data as object)) {
    const p = data as PaginatedResponse<T>
    return { content: p.content ?? [], page: p.page ?? 0, size: p.size ?? 20, total: p.total ?? 0, totalPages: p.totalPages ?? 0 }
  }
  const rows = asRows(data) as T[]
  return { content: rows, page: 0, size: rows.length, total: rows.length, totalPages: 1 }
}

export function defaultDateRange(): { fromDate: string; toDate: string } {
  const to = new Date()
  const from = new Date()
  from.setDate(from.getDate() - 30)
  return { fromDate: from.toISOString().slice(0, 10), toDate: to.toISOString().slice(0, 10) }
}
