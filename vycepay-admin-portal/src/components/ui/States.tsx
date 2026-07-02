export function EmptyState({ message }: { message: string }) {
  return <div className="card muted empty-state">{message}</div>
}

export function SkeletonTable() {
  return <div className="card skeleton">Loading...</div>
}

export function ErrorState({ message }: { message: string }) {
  return <div className="error">{message}</div>
}
