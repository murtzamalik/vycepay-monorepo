export function Avatar({ name, size = 'md' }: { name: string; size?: 'sm' | 'md' | 'lg' }) {
  const initials = name.split(' ').map((p) => p[0]).join('').slice(0, 2).toUpperCase() || '?'
  return <div className={`avatar avatar-${size}`}>{initials}</div>
}
