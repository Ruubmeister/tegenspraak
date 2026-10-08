import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiRequest } from '../api/http'
import type { Verdict } from '../api/types'
import { teamPath } from '../teams/teamsApi'

/** Any teammate can submit, so an open verdict is refreshed to show their attempts. */
const VERDICT_REFRESH_MS = 5_000

function solvedCasesQueryKey(teamId: string) {
  return ['solved-cases', teamId] as const
}

function verdictPath(teamId: string, caseId: string): string {
  return `${teamPath(teamId)}/cases/${encodeURIComponent(caseId)}/verdict`
}

function verdictQueryKey(teamId: string, caseId: string) {
  return ['verdict', teamId, caseId] as const
}

export function useVerdict(teamId: string, caseId: string) {
  return useQuery({
    queryKey: verdictQueryKey(teamId, caseId),
    queryFn: () => apiRequest<Verdict>('GET', verdictPath(teamId, caseId)),
    refetchInterval: VERDICT_REFRESH_MS,
  })
}

export function useSolvedCaseIds(teamId: string) {
  return useQuery({
    queryKey: solvedCasesQueryKey(teamId),
    queryFn: () => apiRequest<string[]>('GET', `${teamPath(teamId)}/solved-cases`),
  })
}

/** Submits the chosen option per question. The response is the updated verdict, including the new score. */
export function useSubmitVerdict(teamId: string, caseId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (answers: Record<string, string>) =>
      apiRequest<Verdict>('POST', verdictPath(teamId, caseId), { answers }),
    onSuccess: (verdict) => {
      queryClient.setQueryData(verdictQueryKey(teamId, caseId), verdict)
      return queryClient.invalidateQueries({ queryKey: solvedCasesQueryKey(teamId) })
    },
  })
}
