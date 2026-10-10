import { useMutation, useMutationState, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiRequest } from '../api/http'
import type { ChatMessage, EvidenceItem } from '../api/types'
import { caseOverviewQueryKey } from '../cases/casesApi'
import { teamPath } from '../teams/teamsApi'
import { clearSentChatDraft } from './chatDrafts'

export interface SendMessageInput {
  content: string
  attachments: EvidenceItem[]
}

/** Teammates write to the same conversation, so it is refreshed regularly while open. */
const CONVERSATION_REFRESH_MS = 3_000

function messagesPath(teamId: string, caseId: string, characterId: string): string {
  return `${teamPath(teamId)}/cases/${encodeURIComponent(caseId)}/characters/${encodeURIComponent(characterId)}/messages`
}

function conversationQueryKey(teamId: string, caseId: string, characterId: string) {
  return ['conversation', teamId, caseId, characterId] as const
}

function sendMessageMutationKey(teamId: string, caseId: string, characterId: string) {
  return ['send-message', teamId, caseId, characterId] as const
}

export function useConversation(teamId: string, caseId: string, characterId: string) {
  return useQuery({
    queryKey: conversationQueryKey(teamId, caseId, characterId),
    queryFn: () => apiRequest<ChatMessage[]>('GET', messagesPath(teamId, caseId, characterId)),
    refetchInterval: CONVERSATION_REFRESH_MS,
  })
}

/**
 * Sends a message and refreshes the conversation, which then contains the message, the reply and any notices. The case
 * is refreshed too, because the character may have introduced a new contact. The stored draft is cleared here rather
 * than in the panel, so it also happens when the player has navigated away while the character was replying.
 */
export function useSendMessage(teamId: string, caseId: string, characterId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationKey: sendMessageMutationKey(teamId, caseId, characterId),
    mutationFn: ({ content, attachments }: SendMessageInput) =>
      apiRequest<ChatMessage>('POST', messagesPath(teamId, caseId, characterId), {
        content,
        attachments: attachments.map(({ desktopId, appId, itemId }) => ({ desktopId, appId, itemId })),
      }),
    onSuccess: (_reply, { content, attachments }) => {
      clearSentChatDraft({ teamId, caseId, characterId }, { text: content, attachments })
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: conversationQueryKey(teamId, caseId, characterId) }),
        queryClient.invalidateQueries({ queryKey: caseOverviewQueryKey(teamId, caseId) }),
      ])
    },
  })
}

/** Messages still waiting for a reply, also those sent before the player navigated away and back. */
export function usePendingMessages(teamId: string, caseId: string, characterId: string): SendMessageInput[] {
  return useMutationState({
    filters: { mutationKey: sendMessageMutationKey(teamId, caseId, characterId), status: 'pending' },
    select: (mutation) => mutation.state.variables as SendMessageInput,
  })
}
