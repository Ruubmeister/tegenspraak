import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Verdict } from '../api/types'
import { jsonResponse, mockFetch, renderWithProviders, statusResponse } from '../test/testUtils'
import { VerdictPanel } from './VerdictPanel'

const VERDICT_PATH = '/api/teams/team-1/cases/het-lek/verdict'

const openVerdict: Verdict = {
  questions: [
    {
      id: 'wie',
      question: 'Wie lekte het bod?',
      options: [
        { id: 'sanne', label: 'Sanne Bakker' },
        { id: 'dennis', label: 'Dennis de Vries' },
      ],
    },
    {
      id: 'hoe',
      question: 'Hoe lekte het bod?',
      options: [
        { id: 'mail', label: 'Via privé-mail' },
        { id: 'schijf', label: 'Via de gedeelde schijf' },
      ],
    },
  ],
  attempts: 0,
  solved: false,
  lastScore: null,
  epilogue: null,
}

async function answerAndSubmit(...optionLabels: string[]) {
  const user = userEvent.setup()
  for (const label of optionLabels) {
    await user.click(screen.getByLabelText(label))
  }
  await user.click(screen.getByRole('button', { name: 'Oplossing indienen' }))
}

describe('VerdictPanel', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('VerdictPanel_givenNotAllQuestionsAnswered_expectSubmitDisabled', async () => {
    mockFetch({ [`GET ${VERDICT_PATH}`]: () => jsonResponse(openVerdict) })
    renderWithProviders(<VerdictPanel teamId="team-1" caseId="het-lek" />)

    await userEvent.setup().click(await screen.findByLabelText('Dennis de Vries'))

    expect(screen.getByRole('button', { name: 'Oplossing indienen' })).toBeDisabled()
  })

  it('VerdictPanel_givenWrongAnswer_expectScoreShownAndAnswersSent', async () => {
    const fetchMock = mockFetch({
      [`GET ${VERDICT_PATH}`]: () => jsonResponse(openVerdict),
      [`POST ${VERDICT_PATH}`]: () =>
        jsonResponse({
          ...openVerdict,
          attempts: 1,
          lastScore: { correctAnswers: 1, totalQuestions: 2, submittedBy: 'anouk' },
        }),
      'GET /api/teams/team-1/solved-cases': () => jsonResponse([]),
    })
    renderWithProviders(<VerdictPanel teamId="team-1" caseId="het-lek" />)
    await screen.findByText('Wie lekte het bod?')

    await answerAndSubmit('Sanne Bakker', 'Via privé-mail')

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Laatste poging door anouk: 1 van de 2 antwoorden kloppen.',
    )
    const postCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST')
    expect(JSON.parse(postCall?.[1]?.body as string)).toEqual({ answers: { wie: 'sanne', hoe: 'mail' } })
  })

  it('VerdictPanel_givenCorrectAnswers_expectEpilogueShown', async () => {
    mockFetch({
      [`GET ${VERDICT_PATH}`]: () => jsonResponse(openVerdict),
      [`POST ${VERDICT_PATH}`]: () =>
        jsonResponse({
          ...openVerdict,
          attempts: 1,
          solved: true,
          lastScore: { correctAnswers: 2, totalQuestions: 2, submittedBy: 'anouk' },
          epilogue: 'Dennis zag het bod op de gedeelde laptop.',
        }),
      'GET /api/teams/team-1/solved-cases': () => jsonResponse(['het-lek']),
    })
    renderWithProviders(<VerdictPanel teamId="team-1" caseId="het-lek" />)
    await screen.findByText('Wie lekte het bod?')

    await answerAndSubmit('Dennis de Vries', 'Via privé-mail')

    expect(await screen.findByRole('heading', { name: 'Zaak opgelost' })).toBeInTheDocument()
    expect(screen.getByText('Opgelost in 1 poging, door anouk.')).toBeInTheDocument()
    expect(screen.getByText('Dennis zag het bod op de gedeelde laptop.')).toBeInTheDocument()
  })

  it('VerdictPanel_givenSubmitFails_expectErrorAndAnswersKept', async () => {
    mockFetch({
      [`GET ${VERDICT_PATH}`]: () => jsonResponse(openVerdict),
      [`POST ${VERDICT_PATH}`]: () => statusResponse(500),
    })
    renderWithProviders(<VerdictPanel teamId="team-1" caseId="het-lek" />)
    await screen.findByText('Wie lekte het bod?')

    await answerAndSubmit('Dennis de Vries', 'Via privé-mail')

    expect(await screen.findByRole('alert')).toHaveTextContent('Je oplossing kon niet worden verstuurd.')
    expect(screen.getByLabelText('Dennis de Vries')).toBeChecked()
  })
})
