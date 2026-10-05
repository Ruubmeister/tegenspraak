import { apiRequest, isApiError } from '../api/http'
import type { Player } from '../api/types'

export interface Credentials {
  username: string
  password: string
}

/** Returns null when nobody is logged in. Also makes the backend hand out the XSRF-TOKEN cookie. */
export async function fetchCurrentPlayer(): Promise<Player | null> {
  try {
    return await apiRequest<Player>('GET', '/api/auth/me')
  } catch (error) {
    if (isApiError(error, 401)) {
      return null
    }
    throw error
  }
}

export function login(credentials: Credentials): Promise<Player> {
  return apiRequest<Player>('POST', '/api/auth/login', credentials)
}

export function register(credentials: Credentials): Promise<Player> {
  return apiRequest<Player>('POST', '/api/auth/register', credentials)
}

export function logout(): Promise<void> {
  return apiRequest<void>('POST', '/api/auth/logout')
}
