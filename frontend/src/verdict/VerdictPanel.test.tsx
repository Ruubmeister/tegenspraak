import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Verdict, VerdictAttempt } from '../api/types'
import { jsonResponse, mockFetch, renderWithProviders, statusResponse } from '../test/testUtils'
import { VerdictPanel } from './VerdictPanel'

const VERDICT_PATH = '/api/teams/team-1/cases/het-lek/verdict'
const WHO = 'Wie lekte het bod?'
const HOW = 'Hoe lekte het bod?'

const openVerdict: Verdict = {
  questions: [
    { id: 'wie', question: WHO },
    { id: 'hoe', question: HOW },
  ],
  maxAttempts: 5,
  attemptsLeft: 5,
  solved: false,
  failed: false,
  attempts: [],
  epilogue: null,
}

function attempt(number: number, correctAnswers: number, answers: Record<string, string>): VerdictAttempt {
  return {
    number,
    submittedBy: 'anouk',
    submittedAt: '2026-10-26T12:00:00Z',
    correctAnswers,
    totalQuestions: 2,
    answers,
  }
}

async function typeAnswers(who: string, how: string) {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText(WHO), who)
  await user.type(screen.getByLabelText(HOW), how)
  return user
}

async function typeAndConfirm(who: string, how: string) {
  const user = await typeAnswers(who, how)
  await user.click(screen.getByRole('button', { name: 'Oplossing indienen' }))
  await user.click(screen.getByRole('button', { name: 'Ja, indienen' }))
}

describe('VerdictPanel', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('VerdictPanel_givenOneAnswerEmpty_expectSubmitDisabled', async () => {
    mockFetch({ [`GET ${VERDICT_PATH}`]: () => jsonResponse(openVerdict) })
    renderWithProviders(<VerdictPanel teamId="team-1" caseId="het-lek" />)

    await userEvent.setup().type(await screen.findByLabelText(WHO), 'Dennis')

    expect(screen.getByRole('button', { name: 'Oplossing indienen' })).toBeDisabled()
  })

  it('VerdictPanel_givenNoAttempts_expectAllAttemptsLeftShown', async () => {
    mockFetch({ [`GET ${VERDICT_PATH}`]: () => jsonResponse(openVerdict) })
    renderWithProviders(<VerdictPanel teamId="team-1" caseId="het-lek" />)

    expect(await screen.findByText('Nog 5 van de 5 pogingen.')).toBeInTheDocument()
  })

  it('VerdictPanel_givenFirstSubmitClick_expectConfirmationAndNothingSent', async () => {
    const fetchMock = mockFetch({ [`GET ${VERDICT_PATH}`]: () => jsonResponse(openVerdict) })
    renderWithProviders(<VerdictPanel teamId="team-1" caseId="het-lek" />)
    await screen.findByText(WHO)

    const user = await typeAnswers('Dennis', 'Via privé-mail')
    await user.click(screen.getByRole('button', { name: 'Oplossing indienen' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Dit kost een poging; daarna zijn er nog 4 over.')
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false)
  })

  it('VerdictPanel_givenConfirmationCancelled_expectNothingSent', async () => {
    const fetchMock = mockFetch({ [`GET ${VERDICT_PATH}`]: () => jsonResponse(openVerdict) })
    renderWithProviders(<VerdictPanel teamId="team-1" caseId="het-lek" />)
    await screen.findByText(WHO)

    const user = await typeAnswers('Dennis', 'Via privé-mail')
    await user.click(screen.getByRole('button', { name: 'Oplossing indienen' }))
    await user.click(screen.getByRole('button', { name: 'Annuleren' }))

    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false)
  })

  it('VerdictPanel_givenWrongAnswer_expectScoreShownAndTypedAnswersSent', async () => {
    const fetchMock = mockFetch({
      [`GET ${VERDICT_PATH}`]: () => jsonResponse(openVerdict),
      [`POST ${VERDICT_PATH}`]: () =>
        jsonResponse({
          ...openVerdict,
          attemptsLeft: 4,
          attempts: [attempt(1, 1, { wie: 'Sanne', hoe: 'Via privé-mail' })],
        }),
      'GET /api/teams/team-1/solved-cases': () => jsonResponse([]),
    })
    renderWithProviders(<VerdictPanel teamId="team-1" caseId="het-lek" />)
    await screen.findByText(WHO)

    await typeAndConfirm('Sanne', 'Via privé-mail')

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Laatste poging door anouk: 1 van de 2 antwoorden kloppen.',
    )
    expect(screen.getByText('Nog 4 van de 5 pogingen.')).toBeInTheDocument()
    const postCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST')
    expect(JSON.parse(postCall?.[1]?.body as string)).toEqual({ answers: { wie: 'Sanne', hoe: 'Via privé-mail' } })
  })

  it('VerdictPanel_givenEarlierAttempt_expectFormPrefilledAndAttemptInHistory', async () => {
    mockFetch({
      [`GET ${VERDICT_PATH}`]: () =>
        jsonResponse({
          ...openVerdict,
          attemptsLeft: 4,
          attempts: [attempt(1, 1, { wie: 'Sanne', hoe: 'Via privé-mail' })],
        }),
    })
    renderWithProviders(<VerdictPanel teamId="team-1" caseId="het-lek" />)

    expect(await screen.findByLabelText(WHO)).toHaveValue('Sanne')
    expect(screen.getByText('Poging 1 door anouk: 1 van de 2 goed')).toBeInTheDocument()
  })

  it('VerdictPanel_givenCorrectAnswers_expectEpilogueShown', async () => {
    mockFetch({
      [`GET ${VERDICT_PATH}`]: () => jsonResponse(openVerdict),
      [`POST ${VERDICT_PATH}`]: () =>
        jsonResponse({
          ...openVerdict,
          attemptsLeft: 0,
          solved: true,
          attempts: [attempt(1, 2, { wie: 'Dennis', hoe: 'Via privé-mail' })],
          epilogue: 'Dennis zag het bod op de gedeelde laptop.',
        }),
      'GET /api/teams/team-1/solved-cases': () => jsonResponse(['het-lek']),
    })
    renderWithProviders(<VerdictPanel teamId="team-1" caseId="het-lek" />)
    await screen.findByText(WHO)

    await typeAndConfirm('Dennis', 'Via privé-mail')

    expect(await screen.findByRole('heading', { name: 'Zaak opgelost' })).toBeInTheDocument()
    expect(screen.getByText('Opgelost in 1 poging, door anouk.')).toBeInTheDocument()
    expect(screen.getByText('Dennis zag het bod op de gedeelde laptop.')).toBeInTheDocument()
  })

  it('VerdictPanel_givenAllAttemptsUsed_expectFailedWithEpilogueAndNoForm', async () => {
    mockFetch({
      [`GET ${VERDICT_PATH}`]: () =>
        jsonResponse({
          ...openVerdict,
          attemptsLeft: 0,
          failed: true,
          attempts: [1, 2, 3, 4, 5].map((number) => attempt(number, 1, { wie: 'Sanne', hoe: 'Schijf' })),
          epilogue: 'Dennis zag het bod op de gedeelde laptop.',
        }),
    })
    renderWithProviders(<VerdictPanel teamId="team-1" caseId="het-lek" />)

    expect(await screen.findByRole('heading', { name: 'Zaak niet opgelost' })).toBeInTheDocument()
    expect(screen.getByText('Dennis zag het bod op de gedeelde laptop.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Oplossing indienen' })).not.toBeInTheDocument()
  })

  it('VerdictPanel_givenGradingFails_expectNoAttemptUsedMessageAndAnswersKept', async () => {
    mockFetch({
      [`GET ${VERDICT_PATH}`]: () => jsonResponse(openVerdict),
      [`POST ${VERDICT_PATH}`]: () => statusResponse(502),
    })
    renderWithProviders(<VerdictPanel teamId="team-1" caseId="het-lek" />)
    await screen.findByText(WHO)

    await typeAndConfirm('Dennis', 'Via privé-mail')

    expect(await screen.findByRole('alert')).toHaveTextContent('Er is geen poging verbruikt')
    expect(screen.getByLabelText(WHO)).toHaveValue('Dennis')
  })

  it('VerdictPanel_givenTeammateSubmittedAtSameTime_expectConflictMessage', async () => {
    mockFetch({
      [`GET ${VERDICT_PATH}`]: () => jsonResponse(openVerdict),
      [`POST ${VERDICT_PATH}`]: () => statusResponse(409),
    })
    renderWithProviders(<VerdictPanel teamId="team-1" caseId="het-lek" />)
    await screen.findByText(WHO)

    await typeAndConfirm('Dennis', 'Via privé-mail')

    expect(await screen.findByRole('alert')).toHaveTextContent('Een teamgenoot heeft net een oplossing ingediend.')
  })
})
