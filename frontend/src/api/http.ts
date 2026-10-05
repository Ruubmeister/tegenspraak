export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'

function readCookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((cookie) => cookie.startsWith(`${name}=`))
    ?.substring(name.length + 1)
}

/**
 * Calls the backend with the session cookie. State changing requests echo the XSRF-TOKEN cookie in a header, which
 * is how Spring Security verifies they come from this app.
 */
export async function apiRequest<T>(method: HttpMethod, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  const csrfToken = readCookie(CSRF_COOKIE)
  if (method !== 'GET' && csrfToken) {
    headers[CSRF_HEADER] = decodeURIComponent(csrfToken)
  }

  const response = await fetch(path, {
    method,
    headers,
    credentials: 'same-origin',
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (!response.ok) {
    throw new ApiError(response.status, `${method} ${path} failed with status ${response.status}`)
  }
  if (response.status === 204 || response.headers.get('Content-Length') === '0') {
    return undefined as T
  }
  return (await response.json()) as T
}

export function isApiError(error: unknown, status: number): boolean {
  return error instanceof ApiError && error.status === status
}
