import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { jsonResponse, mockFetch, renderWithProviders, statusResponse } from '../test/testUtils'
import { LoginPage } from './LoginPage'

function renderLoginPage() {
  renderWithProviders(
    <Routes>
      <Route path="/inloggen" element={<LoginPage />} />
      <Route path="/" element={<p>Zakenoverzicht</p>} />
    </Routes>,
    '/inloggen',
  )
}

async function submitCredentials(username: string, password: string) {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('Gebruikersnaam'), username)
  await user.type(screen.getByLabelText('Wachtwoord'), password)
  await user.click(screen.getByRole('button', { name: 'Inloggen' }))
}

describe('LoginPage', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('LoginPage_givenWrongCredentials_expectErrorMessage', async () => {
    mockFetch({
      'GET /api/auth/me': () => statusResponse(401),
      'POST /api/auth/login': () => statusResponse(401),
    })
    renderLoginPage()

    await submitCredentials('rechercheur', 'verkeerd-wachtwoord')

    expect(await screen.findByRole('alert')).toHaveTextContent('Onjuiste gebruikersnaam of wachtwoord.')
  })

  it('LoginPage_givenValidCredentials_expectNavigatedToCaseList', async () => {
    const fetchMock = mockFetch({
      'GET /api/auth/me': () => statusResponse(401),
      'POST /api/auth/login': () => jsonResponse({ username: 'rechercheur', role: 'PLAYER' }),
    })
    renderLoginPage()

    await submitCredentials('rechercheur', 'geheim-wachtwoord')

    expect(await screen.findByText('Zakenoverzicht')).toBeInTheDocument()
    const loginCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST')
    expect(loginCall?.[1]?.body).toBe('{"username":"rechercheur","password":"geheim-wachtwoord"}')
  })

  it('LoginPage_givenAlreadyLoggedIn_expectRedirectToCaseList', async () => {
    mockFetch({ 'GET /api/auth/me': () => jsonResponse({ username: 'rechercheur', role: 'PLAYER' }) })

    renderLoginPage()

    expect(await screen.findByText('Zakenoverzicht')).toBeInTheDocument()
  })
})
