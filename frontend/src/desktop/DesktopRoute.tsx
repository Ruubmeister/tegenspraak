import { useParams } from 'react-router'
import { useCaseContext } from '../cases/caseContext'
import { useTeamId } from '../teams/teamsApi'
import { useDesktop } from './desktopApi'
import { DesktopView } from './DesktopView'

export function DesktopRoute() {
  const { desktopId = '' } = useParams()
  const teamId = useTeamId()
  const caseOverview = useCaseContext()
  const location = { teamId, caseId: caseOverview.id, desktopId }
  const { data: desktop, isPending, isError } = useDesktop(location)

  if (isPending) {
    return <p className="status">Computer opstarten...</p>
  }
  if (isError) {
    return <p className="status error">Deze computer kon niet worden geopend.</p>
  }
  return <DesktopView key={desktop.id} desktop={desktop} location={location} />
}
