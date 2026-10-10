import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { CaseOverview, Team } from '../api/types'
import { jsonResponse, mockFetch, renderWithProviders } from '../test/testUtils'
import { CasePage } from './CasePage'

const speurneuzen: Team = { id: 'team-1', name: 'De Speurneuzen', joinCode: 'ABCD-EFGH', members: ['anouk'] }

const hetLek: CaseOverview = {
  id: 'het-lek',
  title: 'Het lek bij Van Dijk',
  introduction: 'Een bod lekte uit.',
  characters: [
    { id: 'sanne-bakker', name: 'Sanne Bakker', role: 'Officemanager', description: 'Acht jaar in dienst.' },
  ],
  desktops: [],
}

function renderCasePage() {
  mockFetch({
    'GET /api/teams/team-1': () => jsonResponse(speurneuzen),
    'GET /api/teams/team-1/cases/het-lek': () => jsonResponse(hetLek),
  })
  renderWithProviders(
    <Routes>
      <Route path="/teams/:teamId/zaken/:caseId" element={<CasePage />}>
        <Route index element={<p>Briefingtekst</p>} />
        <Route path="chat/:characterId" element={<p>Gesprek geopend</p>} />
      </Route>
    </Routes>,
    '/teams/team-1/zaken/het-lek',
  )
}

describe('CasePage', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('CasePage_givenMenuButtonClicked_expectNavigationMarkedOpen', async () => {
    const user = userEvent.setup()
    renderCasePage()
    const menuButton = await screen.findByRole('button', { name: 'Menu' })

    await user.click(menuButton)

    expect(screen.getByRole('button', { name: 'Sluiten' })).toHaveAttribute('aria-expanded', 'true')
    expect(screen.getByRole('navigation', { name: 'Zaak' })).toHaveClass('open')
  })

  it('CasePage_givenContactPickedFromOpenMenu_expectMenuClosedAndChatShown', async () => {
    const user = userEvent.setup()
    renderCasePage()
    await user.click(await screen.findByRole('button', { name: 'Menu' }))

    await user.click(screen.getByRole('link', { name: /Sanne Bakker/ }))

    expect(screen.getByText('Gesprek geopend')).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Zaak' })).not.toHaveClass('open')
    expect(screen.getByRole('button', { name: 'Menu' })).toHaveAttribute('aria-expanded', 'false')
  })

  it('CasePage_givenEscapePressedInOpenMenu_expectMenuClosed', async () => {
    const user = userEvent.setup()
    renderCasePage()
    await user.click(await screen.findByRole('button', { name: 'Menu' }))

    await user.keyboard('{Escape}')

    expect(screen.getByRole('navigation', { name: 'Zaak' })).not.toHaveClass('open')
  })
})
