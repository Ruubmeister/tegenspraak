import { Link, NavLink, Outlet, useParams } from 'react-router'
import { useTeam, useTeamId } from '../teams/teamsApi'
import { useCaseOverview } from './casesApi'

/** Shell of a case a team is working on: the navigation on the left, the selected chat or desktop on the right. */
export function CasePage() {
  const { caseId = '' } = useParams()
  const teamId = useTeamId()
  const { data: caseOverview, isPending, isError } = useCaseOverview(caseId)
  // Loading the team also checks membership before the shared conversations are opened
  const team = useTeam(teamId)

  if (isPending || team.isPending) {
    return <p className="status">Zaak laden...</p>
  }
  if (team.isError) {
    return <p className="status error">Dit team bestaat niet of je bent geen lid.</p>
  }
  if (isError) {
    return <p className="status error">Deze zaak kon niet worden geladen.</p>
  }

  return (
    <div className="case-page">
      <nav className="case-nav" aria-label="Zaak">
        <Link to={`/teams/${teamId}`} className="case-nav-team">
          {team.data.name}
        </Link>
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
        <NavLink to="oplossen" className="case-nav-verdict">
          Zaak oplossen
        </NavLink>
      </nav>
      <section className="case-content">
        <Outlet context={caseOverview} />
      </section>
    </div>
  )
}
