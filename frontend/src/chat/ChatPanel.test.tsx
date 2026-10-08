import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { CharacterSummary, ChatMessage } from '../api/types'
import { jsonResponse, mockFetch, renderWithProviders, statusResponse } from '../test/testUtils'
import { ChatPanel } from './ChatPanel'

const MESSAGES_PATH = '/api/teams/team-1/cases/het-lek/characters/sanne-bakker/messages'

const sanne: CharacterSummary = {
  id: 'sanne-bakker',
  name: 'Sanne Bakker',
  role: 'Officemanager',
  description: 'Werkt er al acht jaar.',
}

function playerMessage(id: string, authorName: string, content: string): ChatMessage {
  return { id, sender: 'PLAYER', authorName, content, sentAt: '2026-10-05T10:00:00Z' }
}

function characterMessage(id: string, content: string): ChatMessage {
  return { id, sender: 'CHARACTER', authorName: null, content, sentAt: '2026-10-05T10:01:00Z' }
}

function renderChatPanel() {
  return renderWithProviders(<ChatPanel teamId="team-1" caseId="het-lek" character={sanne} currentUsername="anouk" />)
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
        jsonResponse([playerMessage('1', 'anouk', 'Hallo Sanne'), characterMessage('2', 'Hoi, zeg het maar.')]),
    })

    renderChatPanel()

    expect(await screen.findByText('Hallo Sanne')).toBeInTheDocument()
    expect(screen.getByText('Hoi, zeg het maar.')).toBeInTheDocument()
  })

  it('ChatPanel_givenTeammateMessage_expectTeammateNameShownButNotOwnName', async () => {
    mockFetch({
      [`GET ${MESSAGES_PATH}`]: () =>
        jsonResponse([playerMessage('1', 'anouk', 'Wat weet je?'), playerMessage('2', 'bram', 'En over het bod?')]),
    })

    renderChatPanel()

    const teammateBubble = (await screen.findByText('En over het bod?')).closest('.chat-bubble')
    expect(teammateBubble).toHaveClass('teammate')
    expect(teammateBubble).toHaveTextContent('bram')
    const ownBubble = screen.getByText('Wat weet je?').closest('.chat-bubble')
    expect(ownBubble).toHaveClass('player')
    expect(ownBubble).not.toHaveTextContent('anouk')
  })

  it('ChatPanel_givenSentMessage_expectReplyShownAndDraftCleared', async () => {
    const conversation: ChatMessage[] = []
    mockFetch({
      [`GET ${MESSAGES_PATH}`]: () => jsonResponse(conversation),
      [`POST ${MESSAGES_PATH}`]: () => {
        conversation.push(playerMessage('1', 'anouk', 'Waar was je?'), characterMessage('2', 'Thuis.'))
        return jsonResponse(conversation[1])
      },
    })
    renderChatPanel()
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
    renderChatPanel()
    await screen.findByText('Stuur Sanne Bakker een bericht.')

    await sendMessage('Hallo?')

    expect(await screen.findByRole('alert')).toHaveTextContent('Sanne Bakker reageert niet.')
    expect(screen.getByLabelText('Bericht aan Sanne Bakker')).toHaveValue('Hallo?')
  })
})
