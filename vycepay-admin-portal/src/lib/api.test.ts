import { describe, expect, it } from 'vitest'
import { ApiClientError, errorMessage } from './api'

describe('ApiClientError', () => {
  it('includes requestId in displayMessage', () => {
    const err = new ApiClientError({
      message: 'Customer not found',
      code: 'CUSTOMER_NOT_FOUND',
      requestId: 'req-1',
      status: 404,
    })
    expect(err.displayMessage()).toBe('Customer not found (Request ID: req-1)')
    expect(errorMessage(err)).toBe('Customer not found (Request ID: req-1)')
  })

  it('omits requestId when absent', () => {
    const err = new ApiClientError({
      message: 'Invalid credentials',
      code: 'INVALID_CREDENTIALS',
      status: 401,
    })
    expect(err.displayMessage()).toBe('Invalid credentials')
  })

  it('errorMessage falls back for unknown errors', () => {
    expect(errorMessage(null, 'Unable to load')).toBe('Unable to load')
    expect(errorMessage(new Error('boom'))).toBe('boom')
  })
})
