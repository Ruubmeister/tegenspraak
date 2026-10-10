import { useId, useLayoutEffect, useRef, useState, type FormEvent } from 'react'
import type { CharacterSummary, ChatMessage, EvidenceItem } from '../api/types'
import { AttachmentLinks, DraftAttachments } from './AttachmentChips'
import { AttachmentPicker } from './AttachmentPicker'
import { usePendingMessages, useConversation, useSendMessage } from './chatApi'
import {
  EMPTY_DRAFT,
  isSentDraft,
  MAX_MESSAGE_LENGTH,
  readChatDraft,
  saveChatDraft,
  type ChatDraft,
} from './chatDrafts'
import { isSameEvidence } from './evidenceApi'
import { MessageLengthCounter } from './MessageLengthCounter'

/** How close to the bottom the player must be for new messages to keep the conversation scrolled down. */
const FOLLOW_LATEST_THRESHOLD_PX = 48

interface ChatPanelProps {
  teamId: string
  caseId: string
  character: CharacterSummary
  currentUsername: string
}

/**
 * The team's shared conversation with a character. Messages of teammates appear through regular refreshes. The draft,
 * including up to three attached mails or files, is remembered per conversation, so the player can look up evidence
 * elsewhere and come back to it.
 */
export function ChatPanel({ teamId, caseId, character, currentUsername }: ChatPanelProps) {
  const conversation = { teamId, caseId, characterId: character.id }
  const { data: messages, isPending, isError } = useConversation(teamId, caseId, character.id)
  const sendMessage = useSendMessage(teamId, caseId, character.id)
  const pendingMessages = usePendingMessages(teamId, caseId, character.id)
  const isSending = pendingMessages.length > 0
  const [draft, setDraft] = useState(() => readChatDraft(conversation))
  const [isPickingAttachments, setPickingAttachments] = useState(false)
  // Text over the limit is kept rather than cut off, so a paste is never shortened without the player noticing
  const excessLength = draft.text.length - MAX_MESSAGE_LENGTH
  const isTooLong = excessLength > 0
  const lengthCounterId = useId()
  const messagesBox = useRef<HTMLDivElement>(null)
  const followLatest = useRef(true)

  // Only scrolls the messages box, and only while the player has not scrolled up to read earlier messages
  useLayoutEffect(() => {
    const box = messagesBox.current
    if (box && followLatest.current) {
      box.scrollTop = box.scrollHeight
    }
  }, [messages?.length, pendingMessages.length])

  function handleScroll() {
    const box = messagesBox.current
    if (box) {
      followLatest.current = box.scrollHeight - box.scrollTop - box.clientHeight <= FOLLOW_LATEST_THRESHOLD_PX
    }
  }

  function updateDraft(next: ChatDraft) {
    setDraft(next)
    saveChatDraft(conversation, next)
  }

  function toggleAttachment(evidence: EvidenceItem) {
    const isAttached = draft.attachments.some((attached) => isSameEvidence(attached, evidence))
    updateDraft({
      ...draft,
      attachments: isAttached
        ? draft.attachments.filter((attached) => !isSameEvidence(attached, evidence))
        : [...draft.attachments, evidence],
    })
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const sent: ChatDraft = { text: draft.text.trim(), attachments: draft.attachments }
    if (!sent.text || isSending || isTooLong) {
      return
    }
    followLatest.current = true
    setPickingAttachments(false)
    sendMessage.mutate(
      { content: sent.text, attachments: sent.attachments },
      { onSuccess: () => setDraft((current) => (isSentDraft(current, sent) ? EMPTY_DRAFT : current)) },
    )
  }

  return (
    <div className="chat">
      <header className="chat-header">
        <h2>{character.name}</h2>
        <p>{character.role}</p>
      </header>

      <div className="chat-messages" aria-live="polite" ref={messagesBox} onScroll={handleScroll}>
        {isPending && <p className="status">Gesprek laden...</p>}
        {isError && <p className="status error">Het gesprek kon niet worden geladen.</p>}
        {messages?.length === 0 && !isSending && <p className="status">Stuur {character.name} een bericht.</p>}
        {messages?.map((message) =>
          message.sender === 'NOTICE' ? (
            <p key={message.id} className="chat-notice" role="status">
              {message.content}
            </p>
          ) : (
            <ChatBubble
              key={message.id}
              message={message}
              characterName={character.name}
              currentUsername={currentUsername}
              teamId={teamId}
              caseId={caseId}
            />
          ),
        )}
        {isSending && (
          <>
            {pendingMessages.map((pending, index) => (
              <div key={index} className="chat-bubble player pending">
                <p>{pending.content}</p>
                <AttachmentLinks teamId={teamId} caseId={caseId} attachments={pending.attachments} />
              </div>
            ))}
            <p className="chat-typing">{character.name} is aan het typen...</p>
          </>
        )}
      </div>

      {sendMessage.isError && (
        <p className="error" role="alert">
          {character.name} reageert niet. Probeer het opnieuw.
        </p>
      )}
      {isPickingAttachments && (
        <AttachmentPicker
          teamId={teamId}
          caseId={caseId}
          selected={draft.attachments}
          onToggle={toggleAttachment}
          onClose={() => setPickingAttachments(false)}
        />
      )}
      {isTooLong && (
        <p className="error chat-too-long" role="alert">
          Je bericht is {excessLength} {excessLength === 1 ? 'teken' : 'tekens'} te lang. Een mail of bestand hoef je
          niet te plakken: voeg het toe als bijlage.
        </p>
      )}
      <DraftAttachments attachments={draft.attachments} onRemove={(attachment) => toggleAttachment(attachment)} />
      <form className="chat-input" onSubmit={handleSubmit}>
        <button
          type="button"
          className="chat-attach-button"
          aria-expanded={isPickingAttachments}
          onClick={() => setPickingAttachments((picking) => !picking)}
        >
          Bijlage
        </button>
        <div className="chat-input-field">
          <textarea
            aria-label={`Bericht aan ${character.name}`}
            aria-describedby={lengthCounterId}
            aria-invalid={isTooLong}
            value={draft.text}
            onChange={(event) => updateDraft({ ...draft, text: event.target.value })}
            onKeyDown={(event) => {
              if (event.key === 'Enter' && !event.shiftKey) {
                handleSubmit(event)
              }
            }}
            rows={2}
            placeholder="Typ een bericht..."
          />
          <MessageLengthCounter id={lengthCounterId} length={draft.text.length} />
        </div>
        <button type="submit" disabled={isSending || isTooLong || draft.text.trim() === ''}>
          Verstuur
        </button>
      </form>
    </div>
  )
}

interface ChatBubbleProps {
  message: ChatMessage
  characterName: string
  currentUsername: string
  teamId: string
  caseId: string
}

/** Own messages on the right; teammates and the character on the left, with their name. */
function ChatBubble({ message, characterName, currentUsername, teamId, caseId }: ChatBubbleProps) {
  const time = new Date(message.sentAt).toLocaleTimeString('nl-NL', { hour: '2-digit', minute: '2-digit' })
  const isOwn = message.sender === 'PLAYER' && message.authorName === currentUsername
  const bubbleKind = isOwn ? 'player' : message.sender === 'PLAYER' ? 'teammate' : 'character'
  const authorName = message.sender === 'PLAYER' ? message.authorName : characterName
  return (
    <div className={`chat-bubble ${bubbleKind}`}>
      {!isOwn && <span className="chat-author">{authorName}</span>}
      <p>{message.content}</p>
      <AttachmentLinks teamId={teamId} caseId={caseId} attachments={message.attachments} />
      <time dateTime={message.sentAt}>{time}</time>
    </div>
  )
}
