import { NavLink, Outlet, useParams } from 'react-router'
import { useCaseOverview } from './casesApi'

/** Shell of a running case: the navigation on the left, the selected chat or desktop on the right. */
export function CasePage() {
  const { caseId = '' } = useParams()
  const { data: caseOverview, isPending, isError } = useCaseOverview(caseId)

  if (isPending) {
    return <p className="status">Zaak laden...</p>
  }
  if (isError) {
    return <p className="status error">Deze zaak kon niet worden geladen.</p>
  }

  return (
    <div className="case-page">
      <nav className="case-nav" aria-label="Zaak">
        <h1>{caseOverview.title}</h1>
        <NavLink to="" end>
          Briefing
        </NavLink>
        <h2>Contacten</h2>
        {caseOverview.characters.map((character) => (
          <NavLink key={character.id} to={`chat/${character.id}`}>
            <span className="nav-title">{character.name}</span>
            <span className="nav-subtitle">{character.role}</span>
          </NavLink>
        ))}
        <h2>Computers</h2>
        {caseOverview.desktops.map((desktop) => (
          <NavLink key={desktop.id} to={`computer/${desktop.id}`}>
            <span className="nav-title">{desktop.ownerName}</span>
            <span className="nav-subtitle">{desktop.ownerType === 'person' ? 'Persoon' : 'Organisatie'}</span>
          </NavLink>
        ))}
      </nav>
      <section className="case-content">
        <Outlet context={caseOverview} />
      </section>
    </div>
  )
}
