import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { CharacterSummary, ChatMessage } from '../api/types'
import { jsonResponse, mockFetch, renderWithProviders, statusResponse } from '../test/testUtils'
import { ChatPanel } from './ChatPanel'

const MESSAGES_PATH = '/api/cases/het-lek/characters/sanne-bakker/messages'

const sanne: CharacterSummary = {
  id: 'sanne-bakker',
  name: 'Sanne Bakker',
  role: 'Officemanager',
  description: 'Werkt er al acht jaar.',
}

function message(id: string, sender: ChatMessage['sender'], content: string): ChatMessage {
  return { id, sender, content, sentAt: '2026-10-05T10:00:00Z' }
}

async function sendMessage(text: string) {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('Bericht aan Sanne Bakker'), text)
  await user.click(screen.getByRole('button', { name: 'Verstuur' }))
}

describe('ChatPanel', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('ChatPanel_givenExistingConversation_expectMessagesShown', async () => {
    mockFetch({
      [`GET ${MESSAGES_PATH}`]: () =>
        jsonResponse([message('1', 'PLAYER', 'Hallo Sanne'), message('2', 'CHARACTER', 'Hoi, zeg het maar.')]),
    })

    renderWithProviders(<ChatPanel caseId="het-lek" character={sanne} />)

    expect(await screen.findByText('Hallo Sanne')).toBeInTheDocument()
    expect(screen.getByText('Hoi, zeg het maar.')).toBeInTheDocument()
  })

  it('ChatPanel_givenSentMessage_expectReplyShownAndDraftCleared', async () => {
    const conversation: ChatMessage[] = []
    mockFetch({
      [`GET ${MESSAGES_PATH}`]: () => jsonResponse(conversation),
      [`POST ${MESSAGES_PATH}`]: () => {
        conversation.push(message('1', 'PLAYER', 'Waar was je?'), message('2', 'CHARACTER', 'Thuis.'))
        return jsonResponse(conversation[1])
      },
    })
    renderWithProviders(<ChatPanel caseId="het-lek" character={sanne} />)
    await screen.findByText('Stuur Sanne Bakker een bericht.')

    await sendMessage('Waar was je?')

    expect(await screen.findByText('Thuis.')).toBeInTheDocument()
    expect(screen.getByLabelText('Bericht aan Sanne Bakker')).toHaveValue('')
  })

  it('ChatPanel_givenCharacterUnavailable_expectErrorAndDraftKept', async () => {
    mockFetch({
      [`GET ${MESSAGES_PATH}`]: () => jsonResponse([]),
      [`POST ${MESSAGES_PATH}`]: () => statusResponse(502),
    })
    renderWithProviders(<ChatPanel caseId="het-lek" character={sanne} />)
    await screen.findByText('Stuur Sanne Bakker een bericht.')

    await sendMessage('Hallo?')

    expect(await screen.findByRole('alert')).toHaveTextContent('Sanne Bakker reageert niet.')
    expect(screen.getByLabelText('Bericht aan Sanne Bakker')).toHaveValue('Hallo?')
  })
})
