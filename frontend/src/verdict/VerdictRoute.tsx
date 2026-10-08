import { useCaseContext } from '../cases/caseContext'
import { useTeamId } from '../teams/teamsApi'
import { VerdictPanel } from './VerdictPanel'

export function VerdictRoute() {
  const teamId = useTeamId()
  const caseOverview = useCaseContext()
  return <VerdictPanel teamId={teamId} caseId={caseOverview.id} />
}
