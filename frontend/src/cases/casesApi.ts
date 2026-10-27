import { useQuery } from '@tanstack/react-query'
import { apiRequest } from '../api/http'
import type { CaseOverview, CaseSummary } from '../api/types'
import { teamPath } from '../teams/teamsApi'

/** Live updates arrive through useTeamEvents; this only catches up while the event stream is down. */
const CASE_REFRESH_MS = 60_000

export function caseOverviewQueryKey(teamId: string, caseId: string) {
  return ['cases', teamId, caseId] as const
}

export function useCases() {
  return useQuery({
    queryKey: ['cases'],
    queryFn: () => apiRequest<CaseSummary[]>('GET', '/api/cases'),
  })
}

/** What this team can see of the case: introduced contacts only, locked desktops with their hint. */
export function useCaseOverview(teamId: string, caseId: string) {
  return useQuery({
    queryKey: caseOverviewQueryKey(teamId, caseId),
    queryFn: () => apiRequest<CaseOverview>('GET', `${teamPath(teamId)}/cases/${encodeURIComponent(caseId)}`),
    refetchInterval: CASE_REFRESH_MS,
  })
}
