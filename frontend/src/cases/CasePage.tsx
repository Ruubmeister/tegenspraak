import { useState } from 'react'
import { Link, NavLink, Outlet, useMatch, useParams } from 'react-router'
import { useUnreadConversations } from '../chat/chatApi'
import { UnreadDot } from '../chat/UnreadDot'
import { useTeamEvents } from '../teams/teamEvents'
import { useTeam, useTeamId } from '../teams/teamsApi'
import { LockIcon } from '../desktop/UnlockForm'
import { useCaseOverview } from './casesApi'

/**
 * Shell of a case a team is working on: the navigation on the left, the selected chat or desktop on the right. On small
 * screens the navigation is a menu that opens over the content and closes once something is picked. Contacts with
 * messages the player has not read are marked, also on the menu button; the open chat never is.
 */
export function CasePage() {
  const { caseId = '' } = useParams()
  const [isMenuOpen, setMenuOpen] = useState(false)
  const teamId = useTeamId()
  const { data: caseOverview, isPending, isError } = useCaseOverview(teamId, caseId)
  // Loading the team also checks membership before the shared conversations are opened
  const team = useTeam(teamId)
  const { data: unreadCharacterIds = [] } = useUnreadConversations(teamId, caseId)
  const openCharacterId = useMatch('/teams/:teamId/zaken/:caseId/chat/:characterId')?.params.characterId
  useTeamEvents(teamId)

  if (isPending || team.isPending) {
    return <p className="status">Zaak laden...</p>
  }
  if (team.isError) {
    return <p className="status error">Dit team bestaat niet of je bent geen lid.</p>
  }
  if (isError) {
    return <p className="status error">Deze zaak kon niet worden geladen.</p>
  }

  const closeMenu = () => setMenuOpen(false)
  const isUnread = (characterId: string) => characterId !== openCharacterId && unreadCharacterIds.includes(characterId)
  const hasUnread = caseOverview.characters.some((character) => isUnread(character.id))

  return (
    <div className="case-page" onKeyDown={(event) => event.key === 'Escape' && closeMenu()}>
      <div className="case-bar">
        <span className="case-bar-title">{caseOverview.title}</span>
        <button
          type="button"
          className="case-bar-menu"
          aria-expanded={isMenuOpen}
          aria-controls="case-nav"
          onClick={() => setMenuOpen((open) => !open)}
        >
          {isMenuOpen ? 'Sluiten' : 'Menu'}
          {!isMenuOpen && hasUnread && <UnreadDot />}
        </button>
      </div>
      <nav id="case-nav" className={isMenuOpen ? 'case-nav open' : 'case-nav'} aria-label="Zaak">
        <Link to={`/teams/${teamId}`} className="case-nav-team">
          {team.data.name}
        </Link>
        <h1>{caseOverview.title}</h1>
        <NavLink to="" end onClick={closeMenu}>
          Briefing
        </NavLink>
        <h2>Contacten</h2>
        {caseOverview.characters.map((character) => (
          <NavLink key={character.id} to={`chat/${character.id}`} onClick={closeMenu}>
            <span className="nav-title">
              {character.name}
              {isUnread(character.id) && <UnreadDot />}
            </span>
            <span className="nav-subtitle">{character.role}</span>
          </NavLink>
        ))}
        <h2>Computers</h2>
        {caseOverview.desktops.map((desktop) => (
          <NavLink key={desktop.id} to={`computer/${desktop.id}`} onClick={closeMenu}>
            <span className="nav-title">
              {desktop.ownerName} {desktop.lock && <LockIcon />}
            </span>
            <span className="nav-subtitle">{desktop.ownerType === 'person' ? 'Persoon' : 'Organisatie'}</span>
          </NavLink>
        ))}
        <NavLink to="oplossen" className="case-nav-verdict" onClick={closeMenu}>
          Zaak oplossen
        </NavLink>
      </nav>
      <section className="case-content">
        <Outlet context={caseOverview} />
      </section>
    </div>
  )
}
