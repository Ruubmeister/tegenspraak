import { useParams, useSearchParams } from 'react-router'
import { useCaseContext } from '../cases/caseContext'
import { useTeamId } from '../teams/teamsApi'
import { useDesktop } from './desktopApi'
import { DesktopView } from './DesktopView'

export function DesktopRoute() {
  const { desktopId = '' } = useParams()
  const [searchParams] = useSearchParams()
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
  // A link to an attachment selects its item; the key reopens the computer when that selection changes
  const appId = searchParams.get('app') ?? undefined
  const itemId = searchParams.get('item') ?? undefined
  return (
    <DesktopView
      key={`${desktop.id}/${appId}/${itemId}`}
      desktop={desktop}
      location={location}
      contacts={caseOverview.characters}
      initialSelection={appId && itemId ? { appId, itemId } : undefined}
    />
  )
}
