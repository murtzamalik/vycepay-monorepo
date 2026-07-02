import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { StatusBadge } from '@/components/ui/StatusBadge'

describe('StatusBadge', () => {
  it('renders known status with success variant class', () => {
    render(<StatusBadge status="ACTIVE" />)
    const badge = screen.getByText('ACTIVE')
    expect(badge.className).toContain('badge-success')
  })

  it('renders unknown status with info variant class', () => {
    render(<StatusBadge status="CUSTOM" />)
    const badge = screen.getByText('CUSTOM')
    expect(badge.className).toContain('badge-info')
  })
})
