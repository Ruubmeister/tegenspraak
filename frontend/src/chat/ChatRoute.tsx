import { useParams } from 'react-router'
import { useCaseContext } from '../cases/caseContext'
import { ChatPanel } from './ChatPanel'

export function ChatRoute() {
  const { characterId } = useParams()
  const caseOverview = useCaseContext()
  const character = caseOverview.characters.find((candidate) => candidate.id === characterId)

  if (!character) {
    return <p className="status error">Dit contact bestaat niet.</p>
  }
  // The key resets the draft and scroll state when switching between contacts
  return <ChatPanel key={character.id} caseId={caseOverview.id} character={character} />
}
