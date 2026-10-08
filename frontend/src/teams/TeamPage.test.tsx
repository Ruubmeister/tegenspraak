import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Team } from '../api/types'
import { jsonResponse, mockFetch, renderWithProviders, statusResponse } from '../test/testUtils'
import { TeamPage } from './TeamPage'

const speurneuzen: Team = { id: 'team-1', name: 'De Speurneuzen', joinCode: 'ABCD-EFGH', members: ['anouk', 'bram'] }

function renderTeamPage() {
  renderWithProviders(
    <Routes>
      <Route path="/teams/:teamId" element={<TeamPage />} />
      <Route path="/" element={<p>Teamoverzicht</p>} />
    </Routes>,
    '/teams/team-1',
  )
}

function mockTeamRequests(extraHandlers = {}) {
  return mockFetch({
    'GET /api/teams/team-1': () => jsonResponse(speurneuzen),
    'GET /api/cases': () => jsonResponse([{ id: 'het-lek', title: 'Het lek', summary: 'Een bod lekte uit.' }]),
    'GET /api/teams/team-1/solved-cases': () => jsonResponse(['het-lek']),
    ...extraHandlers,
  })
}

describe('TeamPage', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('TeamPage_givenTeam_expectCodeMembersAndTeamCasesShown', async () => {
    mockTeamRequests()

    renderTeamPage()

    expect(await screen.findByText('ABCD-EFGH')).toBeInTheDocument()
    expect(screen.getByText('Leden: anouk, bram')).toBeInTheDocument()
    const caseLink = await screen.findByRole('link', { name: /Het lek/ })
    expect(caseLink).toHaveAttribute('href', '/teams/team-1/zaken/het-lek')
    expect(caseLink).toHaveTextContent('Opgelost')
  })

  it('TeamPage_givenNonMember_expectErrorMessage', async () => {
    mockFetch({ 'GET /api/teams/team-1': () => statusResponse(404) })

    renderTeamPage()

    expect(await screen.findByText('Dit team bestaat niet of je bent geen lid.')).toBeInTheDocument()
  })

  it('TeamPage_givenConfirmedLeave_expectLeaveRequestAndBackToTeams', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const fetchMock = mockTeamRequests({ 'DELETE /api/teams/team-1/members/me': () => statusResponse(204) })
    renderTeamPage()

    await userEvent.setup().click(await screen.findByRole('button', { name: 'Team verlaten' }))

    expect(await screen.findByText('Teamoverzicht')).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'DELETE')).toBe(true)
  })
})
