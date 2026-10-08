import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { UserAccount } from '../api/types'
import { jsonResponse, mockFetch, renderWithProviders, statusResponse } from '../test/testUtils'
import { RequireAdmin } from './RequireAdmin'
import { UserAdminPage } from './UserAdminPage'

const USERS_PATH = '/api/admin/users'

const admin: UserAccount = { id: 'a1', username: 'admin', role: 'ADMIN', createdAt: '2026-10-01T10:00:00Z' }
const player: UserAccount = { id: 'p1', username: 'rechercheur', role: 'PLAYER', createdAt: '2026-10-02T10:00:00Z' }

function renderAdminPage() {
  renderWithProviders(
    <Routes>
      <Route
        path="/beheer/gebruikers"
        element={
          <RequireAdmin>
            <UserAdminPage />
          </RequireAdmin>
        }
      />
      <Route path="/" element={<p>Zakenoverzicht</p>} />
    </Routes>,
    '/beheer/gebruikers',
  )
}

function rowOf(username: string) {
  return screen.getByRole('row', { name: new RegExp(username) })
}

describe('UserAdminPage', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('UserAdminPage_givenPlayer_expectRedirectToCaseList', async () => {
    mockFetch({ 'GET /api/auth/me': () => jsonResponse({ username: 'rechercheur', role: 'PLAYER' }) })

    renderAdminPage()

    expect(await screen.findByText('Zakenoverzicht')).toBeInTheDocument()
  })

  it('UserAdminPage_givenUsers_expectActionsOnlyForPlayers', async () => {
    mockFetch({
      'GET /api/auth/me': () => jsonResponse({ username: 'admin', role: 'ADMIN' }),
      [`GET ${USERS_PATH}`]: () => jsonResponse([admin, player]),
    })
    renderAdminPage()

    await screen.findByRole('cell', { name: 'rechercheur' })

    expect(within(rowOf('admin')).queryByRole('button')).not.toBeInTheDocument()
    expect(within(rowOf('rechercheur')).getByRole('button', { name: 'Verwijderen' })).toBeInTheDocument()
  })

  it('UserAdminPage_givenNewPlayer_expectCreateRequestSent', async () => {
    const fetchMock = mockFetch({
      'GET /api/auth/me': () => jsonResponse({ username: 'admin', role: 'ADMIN' }),
      [`GET ${USERS_PATH}`]: () => jsonResponse([admin]),
      [`POST ${USERS_PATH}`]: () => jsonResponse(player, 201),
    })
    renderAdminPage()
    const user = userEvent.setup()

    await user.type(await screen.findByLabelText('Gebruikersnaam'), 'rechercheur')
    await user.type(screen.getByLabelText('Wachtwoord'), 'geheim-wachtwoord')
    await user.click(screen.getByRole('button', { name: 'Speler aanmaken' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Speler rechercheur is aangemaakt.')
    const createCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST')
    expect(createCall?.[1]?.body).toBe('{"username":"rechercheur","password":"geheim-wachtwoord"}')
  })

  it('UserAdminPage_givenTakenUsername_expectErrorMessage', async () => {
    mockFetch({
      'GET /api/auth/me': () => jsonResponse({ username: 'admin', role: 'ADMIN' }),
      [`GET ${USERS_PATH}`]: () => jsonResponse([admin]),
      [`POST ${USERS_PATH}`]: () => statusResponse(409),
    })
    renderAdminPage()
    const user = userEvent.setup()

    await user.type(await screen.findByLabelText('Gebruikersnaam'), 'admin')
    await user.type(screen.getByLabelText('Wachtwoord'), 'geheim-wachtwoord')
    await user.click(screen.getByRole('button', { name: 'Speler aanmaken' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Deze gebruikersnaam is al bezet.')
  })

  it('UserAdminPage_givenConfirmedDelete_expectDeleteRequestSent', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const fetchMock = mockFetch({
      'GET /api/auth/me': () => jsonResponse({ username: 'admin', role: 'ADMIN' }),
      [`GET ${USERS_PATH}`]: () => jsonResponse([admin, player]),
      [`DELETE ${USERS_PATH}/p1`]: () => statusResponse(204),
    })
    renderAdminPage()

    await screen.findByRole('cell', { name: 'rechercheur' })
    await userEvent.setup().click(within(rowOf('rechercheur')).getByRole('button', { name: 'Verwijderen' }))

    expect(fetchMock.mock.calls.some(([path, init]) => init?.method === 'DELETE' && path === `${USERS_PATH}/p1`)).toBe(
      true,
    )
  })

  it('UserAdminPage_givenCancelledDelete_expectNoRequest', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    const fetchMock = mockFetch({
      'GET /api/auth/me': () => jsonResponse({ username: 'admin', role: 'ADMIN' }),
      [`GET ${USERS_PATH}`]: () => jsonResponse([admin, player]),
    })
    renderAdminPage()

    await screen.findByRole('cell', { name: 'rechercheur' })
    await userEvent.setup().click(within(rowOf('rechercheur')).getByRole('button', { name: 'Verwijderen' }))

    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'DELETE')).toBe(false)
  })

  it('UserAdminPage_givenPasswordReset_expectResetRequestSent', async () => {
    const fetchMock = mockFetch({
      'GET /api/auth/me': () => jsonResponse({ username: 'admin', role: 'ADMIN' }),
      [`GET ${USERS_PATH}`]: () => jsonResponse([admin, player]),
      [`PUT ${USERS_PATH}/p1/password`]: () => statusResponse(204),
    })
    renderAdminPage()
    const user = userEvent.setup()

    await screen.findByRole('cell', { name: 'rechercheur' })
    await user.click(within(rowOf('rechercheur')).getByRole('button', { name: 'Wachtwoord resetten' }))
    await user.type(screen.getByLabelText('Nieuw wachtwoord voor rechercheur'), 'nieuw-wachtwoord')
    await user.click(screen.getByRole('button', { name: 'Opslaan' }))

    const resetCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')
    expect(resetCall?.[1]?.body).toBe('{"newPassword":"nieuw-wachtwoord"}')
    expect(screen.queryByLabelText('Nieuw wachtwoord voor rechercheur')).not.toBeInTheDocument()
  })
})
