import { useQuery } from '@tanstack/react-query'
import { fetchCurrentPlayer } from './authApi'

export const currentPlayerQueryKey = ['currentPlayer'] as const

export function useCurrentPlayer() {
  return useQuery({
    queryKey: currentPlayerQueryKey,
    queryFn: fetchCurrentPlayer,
    staleTime: Infinity,
  })
}
