import { useState } from 'react'
import { useNavigate } from 'react-router'
import { CaseList } from '../cases/CaseList'
import { useLeaveTeam, useTeam, useTeamId } from './teamsApi'

export function TeamPage() {
  const teamId = useTeamId()
  const { data: team, isPending, isError } = useTeam(teamId)

  if (isPending) {
    return <p className="status">Team laden...</p>
  }
  if (isError) {
    return <p className="status error">Dit team bestaat niet of je bent geen lid.</p>
  }

  return (
    <main className="team-page">
      <header className="team-header">
        <h1>{team.name}</h1>
        <JoinCode joinCode={team.joinCode} />
        <p className="team-members">Leden: {team.members.join(', ')}</p>
        <LeaveTeamButton teamId={team.id} teamName={team.name} />
      </header>
      <h2>Zaken</h2>
      <CaseList teamId={team.id} />
    </main>
  )
}

function JoinCode({ joinCode }: { joinCode: string }) {
  const [copied, setCopied] = useState(false)

  async function copy() {
    await navigator.clipboard.writeText(joinCode)
    setCopied(true)
  }

  return (
    <p className="team-code">
      Teamcode: <code>{joinCode}</code>{' '}
      <button type="button" className="link-button" onClick={copy}>
        {copied ? 'Gekopieerd' : 'Kopiëren'}
      </button>
      <span className="team-code-hint">Deel deze code met wie mee wil spelen.</span>
    </p>
  )
}

function LeaveTeamButton({ teamId, teamName }: { teamId: string; teamName: string }) {
  const leaveTeam = useLeaveTeam(teamId)
  const navigate = useNavigate()

  function handleLeave() {
    const confirmed = window.confirm(
      `Wil je ${teamName} verlaten? Ben je het laatste lid, dan wordt het team met alle voortgang verwijderd.`,
    )
    if (confirmed) {
      leaveTeam.mutate(undefined, { onSuccess: () => navigate('/') })
    }
  }

  return (
    <button type="button" className="link-button" onClick={handleLeave} disabled={leaveTeam.isPending}>
      Team verlaten
    </button>
  )
}
