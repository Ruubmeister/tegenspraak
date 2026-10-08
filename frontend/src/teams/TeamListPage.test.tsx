import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Team } from '../api/types'
import { jsonResponse, mockFetch, renderWithProviders, statusResponse } from '../test/testUtils'
import { TeamListPage } from './TeamListPage'

const speurneuzen: Team = { id: 'team-1', name: 'De Speurneuzen', joinCode: 'ABCD-EFGH', members: ['anouk', 'bram'] }

function renderTeamListPage() {
  renderWithProviders(
    <Routes>
      <Route path="/" element={<TeamListPage />} />
      <Route path="/teams/:teamId" element={<p>Teampagina</p>} />
    </Routes>,
  )
}

describe('TeamListPage', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('TeamListPage_givenTeams_expectTeamsWithMembersLinked', async () => {
    mockFetch({ 'GET /api/teams': () => jsonResponse([speurneuzen]) })

    renderTeamListPage()

    const link = await screen.findByRole('link', { name: /De Speurneuzen/ })
    expect(link).toHaveAttribute('href', '/teams/team-1')
    expect(link).toHaveTextContent('anouk, bram')
  })

  it('TeamListPage_givenNewTeamName_expectTeamCreatedAndOpened', async () => {
    const fetchMock = mockFetch({
      'GET /api/teams': () => jsonResponse([]),
      'POST /api/teams': () => jsonResponse(speurneuzen, 201),
    })
    renderTeamListPage()
    const user = userEvent.setup()

    await user.type(await screen.findByLabelText('Teamnaam'), 'De Speurneuzen')
    await user.click(screen.getByRole('button', { name: 'Team aanmaken' }))

    expect(await screen.findByText('Teampagina')).toBeInTheDocument()
    const createCall = fetchMock.mock.calls.find(([path, init]) => path === '/api/teams' && init?.method === 'POST')
    expect(createCall?.[1]?.body).toBe('{"name":"De Speurneuzen"}')
  })

  it('TeamListPage_givenJoinCode_expectTeamJoinedAndOpened', async () => {
    const fetchMock = mockFetch({
      'GET /api/teams': () => jsonResponse([]),
      'POST /api/teams/join': () => jsonResponse(speurneuzen),
    })
    renderTeamListPage()
    const user = userEvent.setup()

    await user.type(await screen.findByLabelText('Teamcode'), 'abcd-efgh')
    await user.click(screen.getByRole('button', { name: 'Meedoen' }))

    expect(await screen.findByText('Teampagina')).toBeInTheDocument()
    const joinCall = fetchMock.mock.calls.find(([path]) => path === '/api/teams/join')
    expect(joinCall?.[1]?.body).toBe('{"joinCode":"abcd-efgh"}')
  })

  it('TeamListPage_givenUnknownJoinCode_expectErrorMessage', async () => {
    mockFetch({
      'GET /api/teams': () => jsonResponse([]),
      'POST /api/teams/join': () => statusResponse(404),
    })
    renderTeamListPage()
    const user = userEvent.setup()

    await user.type(await screen.findByLabelText('Teamcode'), 'AAAA-AAAA')
    await user.click(screen.getByRole('button', { name: 'Meedoen' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Er is geen team met deze code.')
  })
})
