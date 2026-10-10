import { useParams } from 'react-router'
import { useCurrentPlayer } from '../auth/useCurrentPlayer'
import { useCaseContext } from '../cases/caseContext'
import { useTeamId } from '../teams/teamsApi'
import { ChatPanel } from './ChatPanel'

export function ChatRoute() {
  const { characterId } = useParams()
  const teamId = useTeamId()
  const caseOverview = useCaseContext()
  const { data: player } = useCurrentPlayer()
  const character = caseOverview.characters.find((candidate) => candidate.id === characterId)

  if (!character) {
    return <p className="status error">Dit contact bestaat niet.</p>
  }
  // The key gives every contact its own draft and scroll state
  return (
    <ChatPanel
      key={character.id}
      teamId={teamId}
      caseId={caseOverview.id}
      character={character}
      currentUsername={player?.username ?? ''}
    />
  )
}
