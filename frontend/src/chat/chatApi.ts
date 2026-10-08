import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiRequest } from '../api/http'
import type { ChatMessage } from '../api/types'
import { teamPath } from '../teams/teamsApi'

/** Teammates write to the same conversation, so it is refreshed regularly while open. */
const CONVERSATION_REFRESH_MS = 3_000

function messagesPath(teamId: string, caseId: string, characterId: string): string {
  return `${teamPath(teamId)}/cases/${encodeURIComponent(caseId)}/characters/${encodeURIComponent(characterId)}/messages`
}

function conversationQueryKey(teamId: string, caseId: string, characterId: string) {
  return ['conversation', teamId, caseId, characterId] as const
}

export function useConversation(teamId: string, caseId: string, characterId: string) {
  return useQuery({
    queryKey: conversationQueryKey(teamId, caseId, characterId),
    queryFn: () => apiRequest<ChatMessage[]>('GET', messagesPath(teamId, caseId, characterId)),
    refetchInterval: CONVERSATION_REFRESH_MS,
  })
}

/** Sends a message and refreshes the conversation, which then contains both the message and the reply. */
export function useSendMessage(teamId: string, caseId: string, characterId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (content: string) =>
      apiRequest<ChatMessage>('POST', messagesPath(teamId, caseId, characterId), { content }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: conversationQueryKey(teamId, caseId, characterId) }),
  })
}
