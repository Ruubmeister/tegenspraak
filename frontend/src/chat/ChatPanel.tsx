import { useEffect, useRef, useState, type FormEvent } from 'react'
import type { CharacterSummary, ChatMessage } from '../api/types'
import { useConversation, useSendMessage } from './chatApi'

const MAX_MESSAGE_LENGTH = 1000

interface ChatPanelProps {
  caseId: string
  character: CharacterSummary
}

export function ChatPanel({ caseId, character }: ChatPanelProps) {
  const { data: messages, isPending, isError } = useConversation(caseId, character.id)
  const sendMessage = useSendMessage(caseId, character.id)
  const [draft, setDraft] = useState('')
  const endOfMessages = useRef<HTMLDivElement>(null)

  useEffect(() => {
    endOfMessages.current?.scrollIntoView?.({ behavior: 'smooth' })
  }, [messages, sendMessage.isPending])

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const content = draft.trim()
    if (!content || sendMessage.isPending) {
      return
    }
    sendMessage.mutate(content, { onSuccess: () => setDraft('') })
  }

  return (
    <div className="chat">
      <header className="chat-header">
        <h2>{character.name}</h2>
        <p>{character.role}</p>
      </header>

      <div className="chat-messages" aria-live="polite">
        {isPending && <p className="status">Gesprek laden...</p>}
        {isError && <p className="status error">Het gesprek kon niet worden geladen.</p>}
        {messages?.length === 0 && <p className="status">Stuur {character.name} een bericht.</p>}
        {messages?.map((message) => (
          <ChatBubble key={message.id} message={message} />
        ))}
        {sendMessage.isPending && (
          <>
            <div className="chat-bubble player pending">{sendMessage.variables}</div>
            <p className="chat-typing">{character.name} is aan het typen...</p>
          </>
        )}
        <div ref={endOfMessages} />
      </div>

      {sendMessage.isError && (
        <p className="error" role="alert">
          {character.name} reageert niet. Probeer het opnieuw.
        </p>
      )}
      <form className="chat-input" onSubmit={handleSubmit}>
        <textarea
          aria-label={`Bericht aan ${character.name}`}
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
              handleSubmit(event)
            }
          }}
          maxLength={MAX_MESSAGE_LENGTH}
          rows={2}
          placeholder="Typ een bericht..."
        />
        <button type="submit" disabled={sendMessage.isPending || draft.trim() === ''}>
          Verstuur
        </button>
      </form>
    </div>
  )
}

function ChatBubble({ message }: { message: ChatMessage }) {
  const time = new Date(message.sentAt).toLocaleTimeString('nl-NL', { hour: '2-digit', minute: '2-digit' })
  return (
    <div className={`chat-bubble ${message.sender === 'PLAYER' ? 'player' : 'character'}`}>
      <p>{message.content}</p>
      <time dateTime={message.sentAt}>{time}</time>
    </div>
  )
}
