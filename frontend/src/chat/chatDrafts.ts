import type { EvidenceItem } from '../api/types'
import { isSameEvidence } from './evidenceApi'

/**
 * Unsent messages per conversation, kept in session storage so a player can look up evidence elsewhere in the case
 * and come back to what they were typing. Items can also be attached from the computer while the chat is
 * closed. Storage may be unavailable (private mode, blocked site data); drafts are then simply not remembered.
 */
const DRAFT_KEY_PREFIX = 'tegenspraak.chat-draft'

/** Both must match the limits of the backend. */
export const MAX_ATTACHMENTS = 3
export const MAX_MESSAGE_LENGTH = 1000

export interface ConversationRef {
  teamId: string
  caseId: string
  characterId: string
}

export interface ChatDraft {
  text: string
  attachments: EvidenceItem[]
}

export const EMPTY_DRAFT: ChatDraft = { text: '', attachments: [] }

export type AddAttachmentOutcome = 'added' | 'already-attached' | 'full'

function draftKey({ teamId, caseId, characterId }: ConversationRef): string {
  return `${DRAFT_KEY_PREFIX}:${teamId}:${caseId}:${characterId}`
}

function isChatDraft(value: unknown): value is ChatDraft {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ChatDraft).text === 'string' &&
    Array.isArray((value as ChatDraft).attachments)
  )
}

export function readChatDraft(conversation: ConversationRef): ChatDraft {
  try {
    const stored = sessionStorage.getItem(draftKey(conversation))
    const parsed: unknown = stored === null ? null : JSON.parse(stored)
    return isChatDraft(parsed) ? parsed : EMPTY_DRAFT
  } catch {
    return EMPTY_DRAFT
  }
}

export function saveChatDraft(conversation: ConversationRef, draft: ChatDraft) {
  try {
    if (draft.text === '' && draft.attachments.length === 0) {
      sessionStorage.removeItem(draftKey(conversation))
    } else {
      sessionStorage.setItem(draftKey(conversation), JSON.stringify(draft))
    }
  } catch {
    // Not remembering the draft is acceptable
  }
}

export function addAttachmentToDraft(conversation: ConversationRef, evidence: EvidenceItem): AddAttachmentOutcome {
  const draft = readChatDraft(conversation)
  if (draft.attachments.some((attached) => isSameEvidence(attached, evidence))) {
    return 'already-attached'
  }
  if (draft.attachments.length >= MAX_ATTACHMENTS) {
    return 'full'
  }
  saveChatDraft(conversation, { ...draft, attachments: [...draft.attachments, evidence] })
  return 'added'
}

/** True when the draft holds exactly what was sent, so it can be cleared. */
export function isSentDraft(draft: ChatDraft, sent: ChatDraft): boolean {
  return (
    draft.text.trim() === sent.text &&
    draft.attachments.length === sent.attachments.length &&
    draft.attachments.every((attachment, index) => isSameEvidence(attachment, sent.attachments[index]))
  )
}

/** Clears the draft once it has been sent, unless the player has already started on a new message. */
export function clearSentChatDraft(conversation: ConversationRef, sent: ChatDraft) {
  if (isSentDraft(readChatDraft(conversation), sent)) {
    saveChatDraft(conversation, EMPTY_DRAFT)
  }
}
