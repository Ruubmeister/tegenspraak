import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { isApiError } from '../api/http'
import type { Team } from '../api/types'
import { useCreateTeam, useJoinTeam, useTeams } from './teamsApi'

/** Start page: the player's teams, plus starting a new team or joining one with a code. */
export function TeamListPage() {
  const { data: teams, isPending, isError } = useTeams()

  return (
    <main className="team-list">
      <h1>Rechercheteams</h1>
      <p>Los zaken op met je team. Speel je alleen? Maak dan een team voor jezelf.</p>
      {isPending && <p className="status">Teams laden...</p>}
      {isError && <p className="status error">De teams konden niet worden geladen.</p>}
      {teams?.length === 0 && <p className="status">Je zit nog niet in een team.</p>}
      {teams && teams.length > 0 && (
        <ul>
          {teams.map((team) => (
            <li key={team.id}>
              <Link to={`/teams/${team.id}`} className="case-card">
                <h2>{team.name}</h2>
                <p>{team.members.join(', ')}</p>
              </Link>
            </li>
          ))}
        </ul>
      )}
      <div className="team-actions">
        <CreateTeamForm />
        <JoinTeamForm />
      </div>
    </main>
  )
}

function useOpenTeam() {
  const navigate = useNavigate()
  return (team: Team) => navigate(`/teams/${team.id}`)
}

function CreateTeamForm() {
  const createTeam = useCreateTeam()
  const openTeam = useOpenTeam()
  const [name, setName] = useState('')

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    createTeam.mutate(name.trim(), { onSuccess: openTeam })
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <h2>Nieuw team</h2>
      <label>
        Teamnaam
        <input value={name} onChange={(event) => setName(event.target.value)} required maxLength={50} />
      </label>
      {createTeam.isError && (
        <p className="error" role="alert">
          Het team kon niet worden aangemaakt.
        </p>
      )}
      <button type="submit" disabled={createTeam.isPending}>
        Team aanmaken
      </button>
    </form>
  )
}

function JoinTeamForm() {
  const joinTeam = useJoinTeam()
  const openTeam = useOpenTeam()
  const [joinCode, setJoinCode] = useState('')

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    joinTeam.mutate(joinCode.trim(), { onSuccess: openTeam })
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <h2>Meedoen met een team</h2>
      <label>
        Teamcode
        <input
          value={joinCode}
          onChange={(event) => setJoinCode(event.target.value)}
          placeholder="ABCD-EFGH"
          autoComplete="off"
          required
          maxLength={20}
        />
      </label>
      {joinTeam.isError && (
        <p className="error" role="alert">
          {isApiError(joinTeam.error, 404) ? 'Er is geen team met deze code.' : 'Meedoen is mislukt.'}
        </p>
      )}
      <button type="submit" disabled={joinTeam.isPending}>
        Meedoen
      </button>
    </form>
  )
}
