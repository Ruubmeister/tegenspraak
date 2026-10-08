import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { jsonResponse, mockFetch, renderWithProviders, statusResponse } from '../test/testUtils'
import { AccountPage } from './AccountPage'

async function changePassword(currentPassword: string, newPassword: string, repeatedPassword = newPassword) {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('Huidig wachtwoord'), currentPassword)
  await user.type(screen.getByLabelText('Nieuw wachtwoord'), newPassword)
  await user.type(screen.getByLabelText('Herhaal nieuw wachtwoord'), repeatedPassword)
  await user.click(screen.getByRole('button', { name: 'Wachtwoord wijzigen' }))
}

describe('AccountPage', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('AccountPage_givenValidPasswords_expectChangeRequestSentAndConfirmation', async () => {
    const fetchMock = mockFetch({
      'GET /api/auth/me': () => jsonResponse({ username: 'rechercheur', role: 'PLAYER' }),
      'PUT /api/auth/password': () => statusResponse(204),
    })
    renderWithProviders(<AccountPage />)

    await changePassword('oud-wachtwoord', 'nieuw-wachtwoord')

    expect(await screen.findByRole('status')).toHaveTextContent('Je wachtwoord is gewijzigd.')
    const changeCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')
    expect(changeCall?.[1]?.body).toBe('{"currentPassword":"oud-wachtwoord","newPassword":"nieuw-wachtwoord"}')
  })

  it('AccountPage_givenDifferentRepeatedPassword_expectErrorAndNoRequest', async () => {
    const fetchMock = mockFetch({
      'GET /api/auth/me': () => jsonResponse({ username: 'rechercheur', role: 'PLAYER' }),
    })
    renderWithProviders(<AccountPage />)

    await changePassword('oud-wachtwoord', 'nieuw-wachtwoord', 'ander-wachtwoord')

    expect(screen.getByRole('alert')).toHaveTextContent('De nieuwe wachtwoorden zijn niet gelijk.')
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false)
  })

  it('AccountPage_givenWrongCurrentPassword_expectErrorMessage', async () => {
    mockFetch({
      'GET /api/auth/me': () => jsonResponse({ username: 'rechercheur', role: 'PLAYER' }),
      'PUT /api/auth/password': () => statusResponse(422),
    })
    renderWithProviders(<AccountPage />)

    await changePassword('fout-wachtwoord', 'nieuw-wachtwoord')

    expect(await screen.findByRole('alert')).toHaveTextContent('Je huidige wachtwoord klopt niet.')
  })
})
