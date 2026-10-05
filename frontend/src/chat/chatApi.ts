import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiRequest } from '../api/http'
import type { ChatMessage } from '../api/types'

function messagesPath(caseId: string, characterId: string): string {
  return `/api/cases/${encodeURIComponent(caseId)}/characters/${encodeURIComponent(characterId)}/messages`
}

function conversationQueryKey(caseId: string, characterId: string) {
  return ['conversation', caseId, characterId] as const
}

export function useConversation(caseId: string, characterId: string) {
  return useQuery({
    queryKey: conversationQueryKey(caseId, characterId),
    queryFn: () => apiRequest<ChatMessage[]>('GET', messagesPath(caseId, characterId)),
  })
}

/** Sends a message and refreshes the conversation, which then contains both the message and the reply. */
export function useSendMessage(caseId: string, characterId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (content: string) => apiRequest<ChatMessage>('POST', messagesPath(caseId, characterId), { content }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: conversationQueryKey(caseId, characterId) }),
  })
}
