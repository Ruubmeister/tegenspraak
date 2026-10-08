import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiRequest } from '../api/http'
import type { UserAccount } from '../api/types'
import type { Credentials } from '../auth/authApi'

const USERS_PATH = '/api/admin/users'
const USERS_QUERY_KEY = ['admin', 'users'] as const

function userPath(userId: string): string {
  return `${USERS_PATH}/${encodeURIComponent(userId)}`
}

export function useUsers() {
  return useQuery({
    queryKey: USERS_QUERY_KEY,
    queryFn: () => apiRequest<UserAccount[]>('GET', USERS_PATH),
  })
}

export function useCreateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (credentials: Credentials) => apiRequest<UserAccount>('POST', USERS_PATH, credentials),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: USERS_QUERY_KEY }),
  })
}

export function useResetPassword(userId: string) {
  return useMutation({
    mutationFn: (newPassword: string) => apiRequest<void>('PUT', `${userPath(userId)}/password`, { newPassword }),
  })
}

export function useDeleteUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (userId: string) => apiRequest<void>('DELETE', userPath(userId)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: USERS_QUERY_KEY }),
  })
}
