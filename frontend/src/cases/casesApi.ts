import { useQuery } from '@tanstack/react-query'
import { apiRequest } from '../api/http'
import type { CaseOverview, CaseSummary } from '../api/types'

export function useCases() {
  return useQuery({
    queryKey: ['cases'],
    queryFn: () => apiRequest<CaseSummary[]>('GET', '/api/cases'),
  })
}

export function useCaseOverview(caseId: string) {
  return useQuery({
    queryKey: ['cases', caseId],
    queryFn: () => apiRequest<CaseOverview>('GET', `/api/cases/${encodeURIComponent(caseId)}`),
  })
}
