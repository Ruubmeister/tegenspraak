import { afterEach, describe, expect, it, vi } from 'vitest'
import { jsonResponse, mockFetch, statusResponse } from '../test/testUtils'
import { ApiError, apiRequest } from './http'

describe('apiRequest', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('apiRequest_givenPostWithCsrfCookie_expectTokenEchoedInHeader', async () => {
    document.cookie = 'XSRF-TOKEN=abc-123; path=/'
    const fetchMock = mockFetch({ 'POST /api/test': () => jsonResponse({ ok: true }) })

    await apiRequest('POST', '/api/test', { value: 1 })

    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect(init.headers).toMatchObject({ 'X-XSRF-TOKEN': 'abc-123', 'Content-Type': 'application/json' })
    expect(init.body).toBe('{"value":1}')
  })

  it('apiRequest_givenGet_expectNoCsrfHeader', async () => {
    document.cookie = 'XSRF-TOKEN=abc-123; path=/'
    const fetchMock = mockFetch({ 'GET /api/test': () => jsonResponse([]) })

    await apiRequest('GET', '/api/test')

    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect(init.headers).not.toHaveProperty('X-XSRF-TOKEN')
  })

  it('apiRequest_givenErrorStatus_expectApiErrorWithStatus', async () => {
    mockFetch({ 'GET /api/test': () => statusResponse(404) })

    const request = apiRequest('GET', '/api/test')

    await expect(request).rejects.toBeInstanceOf(ApiError)
    await expect(request).rejects.toMatchObject({ status: 404 })
  })

  it('apiRequest_givenNoContentResponse_expectUndefined', async () => {
    mockFetch({ 'POST /api/auth/logout': () => statusResponse(204) })

    await expect(apiRequest('POST', '/api/auth/logout')).resolves.toBeUndefined()
  })
})
