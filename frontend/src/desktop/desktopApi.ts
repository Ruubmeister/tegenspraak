import { useQuery } from '@tanstack/react-query'
import { apiRequest } from '../api/http'
import type { Desktop } from '../api/types'

export function useDesktop(caseId: string, desktopId: string) {
  return useQuery({
    queryKey: ['desktop', caseId, desktopId],
    queryFn: () =>
      apiRequest<Desktop>('GET', `/api/cases/${encodeURIComponent(caseId)}/desktops/${encodeURIComponent(desktopId)}`),
    staleTime: Infinity,
  })
}
