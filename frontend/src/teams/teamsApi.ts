import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router'
import { apiRequest } from '../api/http'
import type { Team } from '../api/types'

const TEAMS_QUERY_KEY = ['teams'] as const
const MEMBERS_REFRESH_MS = 10_000

export function teamPath(teamId: string): string {
  return `/api/teams/${encodeURIComponent(teamId)}`
}

/** The team of the current route. Every route below /teams/:teamId has it. */
export function useTeamId(): string {
  const { teamId = '' } = useParams()
  return teamId
}

export function useTeams() {
  return useQuery({
    queryKey: TEAMS_QUERY_KEY,
    queryFn: () => apiRequest<Team[]>('GET', '/api/teams'),
  })
}

/** Refreshes now and then, so new members show up without reloading. */
export function useTeam(teamId: string) {
  return useQuery({
    queryKey: [...TEAMS_QUERY_KEY, teamId],
    queryFn: () => apiRequest<Team>('GET', teamPath(teamId)),
    refetchInterval: MEMBERS_REFRESH_MS,
  })
}

export function useCreateTeam() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => apiRequest<Team>('POST', '/api/teams', { name }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: TEAMS_QUERY_KEY }),
  })
}

export function useJoinTeam() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (joinCode: string) => apiRequest<Team>('POST', '/api/teams/join', { joinCode }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: TEAMS_QUERY_KEY }),
  })
}

export function useLeaveTeam(teamId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => apiRequest<void>('DELETE', `${teamPath(teamId)}/members/me`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: TEAMS_QUERY_KEY }),
  })
}
