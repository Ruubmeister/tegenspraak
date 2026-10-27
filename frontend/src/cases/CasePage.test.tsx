import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { CaseOverview, Team } from '../api/types'
import { FakeEventSource } from '../test/fakeEventSource'
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

function renderCasePage(initialPath = '/teams/team-1/zaken/het-lek', unreadCharacterIds: string[] = []) {
  mockFetch({
    'GET /api/teams/team-1': () => jsonResponse(speurneuzen),
    'GET /api/teams/team-1/cases/het-lek': () => jsonResponse(hetLek),
    'GET /api/teams/team-1/cases/het-lek/unread-conversations': () => jsonResponse(unreadCharacterIds),
  })
  renderWithProviders(
    <Routes>
      <Route path="/teams/:teamId/zaken/:caseId" element={<CasePage />}>
        <Route index element={<p>Briefingtekst</p>} />
        <Route path="chat/:characterId" element={<p>Gesprek geopend</p>} />
      </Route>
    </Routes>,
    initialPath,
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

  it('CasePage_givenUnreadConversation_expectContactAndMenuButtonMarked', async () => {
    renderCasePage('/teams/team-1/zaken/het-lek', ['sanne-bakker'])

    const contact = await screen.findByRole('link', { name: /Sanne Bakker/ })

    expect(await within(contact).findByRole('img', { name: 'Nieuw bericht' })).toBeInTheDocument()
    expect(within(screen.getByRole('button', { name: /Menu/ })).getByRole('img')).toBeInTheDocument()
  })

  it('CasePage_givenUnreadConversationOpen_expectNoMarker', async () => {
    renderCasePage('/teams/team-1/zaken/het-lek/chat/sanne-bakker', ['sanne-bakker'])

    expect(await screen.findByText('Gesprek geopend')).toBeInTheDocument()
    await vi.waitFor(() => expect(fetchedPaths()).toContain('/api/teams/team-1/cases/het-lek/unread-conversations'))
    expect(screen.queryByRole('img', { name: 'Nieuw bericht' })).not.toBeInTheDocument()
  })

  it('CasePage_givenOpened_expectTeamEventStreamOpened', async () => {
    renderCasePage()

    await screen.findByRole('button', { name: 'Menu' })

    expect(FakeEventSource.instances.map((source) => source.url)).toContain('/api/teams/team-1/events')
  })
})

function fetchedPaths(): string[] {
  return vi.mocked(fetch).mock.calls.map(([input]) => input.toString())
}
