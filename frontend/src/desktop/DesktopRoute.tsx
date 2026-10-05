import { useParams } from 'react-router'
import { useCaseContext } from '../cases/caseContext'
import { useDesktop } from './desktopApi'
import { DesktopView } from './DesktopView'

export function DesktopRoute() {
  const { desktopId = '' } = useParams()
  const caseOverview = useCaseContext()
  const { data: desktop, isPending, isError } = useDesktop(caseOverview.id, desktopId)

  if (isPending) {
    return <p className="status">Computer opstarten...</p>
  }
  if (isError) {
    return <p className="status error">Deze computer kon niet worden geopend.</p>
  }
  return <DesktopView key={desktop.id} desktop={desktop} />
}
