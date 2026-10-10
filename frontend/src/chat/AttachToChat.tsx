import { useState } from 'react'
import { Link } from 'react-router'
import type { CharacterSummary, EvidenceItem } from '../api/types'
import { chatPagePath } from '../cases/casePaths'
import { addAttachmentToDraft, MAX_ATTACHMENTS, type AddAttachmentOutcome } from './chatDrafts'

interface AttachToChatProps {
  teamId: string
  caseId: string
  evidence: EvidenceItem
  contacts: CharacterSummary[]
}

interface AttachResult {
  contact: CharacterSummary
  outcome: AddAttachmentOutcome
}

/** Adds the mail or file being read to the unsent message for a contact, to show it to them in the chat. */
export function AttachToChat({ teamId, caseId, evidence, contacts }: AttachToChatProps) {
  const [isChoosing, setChoosing] = useState(false)
  const [result, setResult] = useState<AttachResult>()

  if (contacts.length === 0) {
    return null
  }

  function attachFor(contact: CharacterSummary) {
    const outcome = addAttachmentToDraft({ teamId, caseId, characterId: contact.id }, evidence)
    setResult({ contact, outcome })
    setChoosing(false)
  }

  return (
    <div className="attach-to-chat">
      <button
        type="button"
        className="link-button"
        aria-expanded={isChoosing}
        onClick={() => setChoosing((choosing) => !choosing)}
      >
        Voeg toe aan bericht
      </button>
      {isChoosing && (
        <ul aria-label="Kies een contact">
          {contacts.map((contact) => (
            <li key={contact.id}>
              <button type="button" onClick={() => attachFor(contact)}>
                {contact.name}
              </button>
            </li>
          ))}
        </ul>
      )}
      {result && (
        <p role="status">
          {outcomeMessage(result)}{' '}
          <Link to={chatPagePath(teamId, caseId, result.contact.id)}>Naar gesprek met {result.contact.name}</Link>
        </p>
      )}
    </div>
  )
}

function outcomeMessage({ contact, outcome }: AttachResult): string {
  switch (outcome) {
    case 'added':
      return `Toegevoegd aan je bericht aan ${contact.name}.`
    case 'already-attached':
      return `Staat al in je bericht aan ${contact.name}.`
    case 'full':
      return `Je bericht aan ${contact.name} heeft al ${MAX_ATTACHMENTS} bijlagen.`
  }
}
