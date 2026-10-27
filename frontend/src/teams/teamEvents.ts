import { useQueryClient, type QueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import type { TeamEvent } from '../api/types'
import { caseOverviewQueryKey } from '../cases/casesApi'
import { conversationQueryKey, unreadConversationsQueryKey } from '../chat/chatApi'
import { evidenceQueryKey } from '../chat/evidenceApi'
import { caseDesktopsQueryKey } from '../desktop/desktopApi'
import { solvedCasesQueryKey, verdictQueryKey } from '../verdict/verdictApi'
import { teamPath, teamQueryKey } from './teamsApi'

/**
 * Listens to the team's server-sent events while mounted and reloads what each event points to. The browser
 * reconnects by itself when the stream drops; events sent meanwhile are lost, so everything of the team is reloaded
 * after a reconnect.
 */
export function useTeamEvents(teamId: string) {
  const queryClient = useQueryClient()

  useEffect(() => {
    const source = new EventSource(`${teamPath(teamId)}/events`)
    let hasConnected = false
    source.onopen = () => {
      if (hasConnected) {
        // Every team scoped query key has the team id second
        void queryClient.invalidateQueries({ predicate: (query) => query.queryKey[1] === teamId })
      }
      hasConnected = true
    }
    source.onmessage = (message: MessageEvent<string>) => {
      void reloadForEvent(queryClient, teamId, JSON.parse(message.data) as TeamEvent)
    }
    return () => source.close()
  }, [queryClient, teamId])
}

/** Only queries on screen refetch right away; the others are marked stale and refetch when they are shown again. */
export function reloadForEvent(queryClient: QueryClient, teamId: string, event: TeamEvent): Promise<unknown> {
  const invalidate = (queryKey: readonly unknown[]) => queryClient.invalidateQueries({ queryKey })
  switch (event.type) {
    case 'CHAT':
      return Promise.all([
        invalidate(conversationQueryKey(teamId, event.caseId, event.characterId)),
        invalidate(unreadConversationsQueryKey(teamId, event.caseId)),
      ])
    case 'CASE':
      return Promise.all([
        invalidate(caseOverviewQueryKey(teamId, event.caseId)),
        invalidate(caseDesktopsQueryKey(teamId, event.caseId)),
        invalidate(evidenceQueryKey(teamId, event.caseId)),
      ])
    case 'VERDICT':
      return Promise.all([invalidate(verdictQueryKey(teamId, event.caseId)), invalidate(solvedCasesQueryKey(teamId))])
    case 'MEMBERS':
      return invalidate(teamQueryKey(teamId))
  }
}
