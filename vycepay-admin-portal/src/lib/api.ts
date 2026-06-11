export type ApiEnvelope<T> = { success: boolean; code: string; message: string; requestId?: string; data: T }

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path.startsWith('/api/') ? path : `/api/admin${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
    cache: 'no-store',
  })
  if (!response.ok) {
    const text = await response.text()
    let message = text || 'Request failed'
    try {
      const json = JSON.parse(text)
      message = json.message || json.code || message
    } catch {
      message = text || message
    }
    throw new Error(message)
  }
  const json = (await response.json()) as ApiEnvelope<T>
  return json.data
}

export function asRows(data: unknown): Record<string, unknown>[] {
  if (Array.isArray(data)) return data as Record<string, unknown>[]
  if (data && typeof data === 'object' && Array.isArray((data as { content?: unknown }).content)) {
    return (data as { content: Record<string, unknown>[] }).content
  }
  if (data && typeof data === 'object') return [data as Record<string, unknown>]
  return []
}
