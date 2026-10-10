import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { CharacterSummary, ChatMessage, EvidenceItem } from '../api/types'
import { jsonResponse, mockFetch, renderWithProviders, statusResponse } from '../test/testUtils'
import { ChatPanel } from './ChatPanel'
import { addAttachmentToDraft } from './chatDrafts'

const MESSAGES_PATH = '/api/teams/team-1/cases/het-lek/characters/sanne-bakker/messages'
const EVIDENCE_PATH = '/api/teams/team-1/cases/het-lek/evidence'

function evidence(itemId: string, title: string): EvidenceItem {
  return { desktopId: 'werkplek', appId: 'mail', itemId, kind: 'mail', title, ownerName: 'Sanne Bakker' }
}

const bidMail = evidence('bod', 'Bod Oude Suikerfabriek')
const allEvidence = [bidMail, evidence('etentje', 'zaterdag'), evidence('fw', 'FW: Bod'), evidence('kort', 'Kort')]

const sanne: CharacterSummary = {
  id: 'sanne-bakker',
  name: 'Sanne Bakker',
  role: 'Officemanager',
  description: 'Werkt er al acht jaar.',
}

function playerMessage(id: string, authorName: string, content: string): ChatMessage {
  return { id, sender: 'PLAYER', authorName, content, sentAt: '2026-10-05T10:00:00Z', attachments: [] }
}

function characterMessage(id: string, content: string): ChatMessage {
  return { id, sender: 'CHARACTER', authorName: null, content, sentAt: '2026-10-05T10:01:00Z', attachments: [] }
}

function renderChatPanel() {
  return renderWithProviders(<ChatPanel teamId="team-1" caseId="het-lek" character={sanne} currentUsername="anouk" />)
}

/** Renders the panel so it can be closed and reopened, like navigating to another contact and back. */
function renderClosableChatPanel() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const tree = (panelOpen: boolean) => (
    <QueryClientProvider client={queryClient}>
      {panelOpen && <ChatPanel teamId="team-1" caseId="het-lek" character={sanne} currentUsername="anouk" />}
    </QueryClientProvider>
  )
  const { rerender } = render(tree(true))
  return {
    close: () => rerender(tree(false)),
    reopen: () => rerender(tree(true)),
  }
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

  it('ChatPanel_givenEnterOnTouchScreen_expectNewLineInsteadOfSend', async () => {
    vi.stubGlobal('matchMedia', (query: string) => ({ matches: query === '(pointer: coarse)' }))
    const fetchMock = mockFetch({ [`GET ${MESSAGES_PATH}`]: () => jsonResponse([]) })
    const user = userEvent.setup()
    renderChatPanel()
    await screen.findByText('Stuur Sanne Bakker een bericht.')

    await user.type(screen.getByLabelText('Bericht aan Sanne Bakker'), 'Eerste regel{Enter}Tweede regel')

    expect(screen.getByLabelText('Bericht aan Sanne Bakker')).toHaveValue('Eerste regel\nTweede regel')
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false)
  })

  it('ChatPanel_givenNoticeInConversation_expectNoticeShownAsEvent', async () => {
    mockFetch({
      [`GET ${MESSAGES_PATH}`]: () =>
        jsonResponse([
          characterMessage('1', 'Hier is zijn nummer.'),
          {
            id: '2',
            sender: 'NOTICE',
            authorName: null,
            content: 'Sanne Bakker heeft je in contact gebracht met Dennis de Vries.',
            sentAt: '2026-10-05T10:02:00Z',
            attachments: [],
          },
        ]),
    })

    renderChatPanel()

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Sanne Bakker heeft je in contact gebracht met Dennis de Vries.',
    )
  })

  it('ChatPanel_givenUnsentDraftAndPanelReopened_expectDraftRestored', async () => {
    mockFetch({ [`GET ${MESSAGES_PATH}`]: () => jsonResponse([]) })
    const panel = renderClosableChatPanel()
    await screen.findByText('Stuur Sanne Bakker een bericht.')
    await userEvent.setup().type(screen.getByLabelText('Bericht aan Sanne Bakker'), 'Waar was je dinsdag?')

    panel.close()
    panel.reopen()

    expect(screen.getByLabelText('Bericht aan Sanne Bakker')).toHaveValue('Waar was je dinsdag?')
  })

  it('ChatPanel_givenReplyArrivesWhilePanelClosed_expectPendingShownAndDraftClearedOnReturn', async () => {
    const conversation: ChatMessage[] = []
    let deliverReply = () => {}
    mockFetch({
      [`GET ${MESSAGES_PATH}`]: () => jsonResponse(conversation),
      [`POST ${MESSAGES_PATH}`]: () =>
        new Promise<Response>((resolve) => {
          deliverReply = () => {
            conversation.push(playerMessage('1', 'anouk', 'Waar was je?'), characterMessage('2', 'Thuis.'))
            resolve(jsonResponse(conversation[1]))
          }
        }),
    })
    const panel = renderClosableChatPanel()
    await screen.findByText('Stuur Sanne Bakker een bericht.')
    await sendMessage('Waar was je?')

    panel.close()
    panel.reopen()

    expect(screen.getByText('Sanne Bakker is aan het typen...')).toBeInTheDocument()
    expect(screen.getByText('Waar was je?', { selector: '.chat-bubble p' }).closest('.chat-bubble')).toHaveClass(
      'pending',
    )

    panel.close()
    deliverReply()
    await vi.waitFor(() => expect(sessionStorage.length).toBe(0))
    panel.reopen()

    expect(await screen.findByText('Thuis.')).toBeInTheDocument()
    expect(screen.getByLabelText('Bericht aan Sanne Bakker')).toHaveValue('')
  })

  it('ChatPanel_givenAttachmentPicked_expectReferenceSentWithMessage', async () => {
    const fetchMock = mockFetch({
      [`GET ${MESSAGES_PATH}`]: () => jsonResponse([]),
      [`GET ${EVIDENCE_PATH}`]: () => jsonResponse(allEvidence),
      [`POST ${MESSAGES_PATH}`]: () => jsonResponse(characterMessage('2', 'Dat is het bod.')),
    })
    const user = userEvent.setup()
    renderChatPanel()
    await screen.findByText('Stuur Sanne Bakker een bericht.')

    await user.click(screen.getByRole('button', { name: 'Bijlage' }))
    await user.click(await screen.findByRole('checkbox', { name: /Bod Oude Suikerfabriek/ }))
    await sendMessage('Wat is dit?')

    await vi.waitFor(() => {
      const sendCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST')
      expect(JSON.parse(String(sendCall?.[1]?.body))).toEqual({
        content: 'Wat is dit?',
        attachments: [{ desktopId: 'werkplek', appId: 'mail', itemId: 'bod' }],
      })
    })
  })

  it('ChatPanel_givenThreeAttachmentsPicked_expectOtherOptionsDisabled', async () => {
    mockFetch({
      [`GET ${MESSAGES_PATH}`]: () => jsonResponse([]),
      [`GET ${EVIDENCE_PATH}`]: () => jsonResponse(allEvidence),
    })
    const user = userEvent.setup()
    renderChatPanel()
    await user.click(await screen.findByRole('button', { name: 'Bijlage' }))

    await user.click(await screen.findByRole('checkbox', { name: /Bod Oude Suikerfabriek/ }))
    await user.click(screen.getByRole('checkbox', { name: /zaterdag/ }))
    await user.click(screen.getByRole('checkbox', { name: /FW: Bod/ }))

    expect(screen.getByRole('checkbox', { name: /Kort/ })).toBeDisabled()
    expect(screen.getByRole('checkbox', { name: /FW: Bod/ })).toBeEnabled()
  })

  it('ChatPanel_givenAttachmentAddedFromComputer_expectShownInDraftAndRemovable', async () => {
    mockFetch({ [`GET ${MESSAGES_PATH}`]: () => jsonResponse([]) })
    addAttachmentToDraft({ teamId: 'team-1', caseId: 'het-lek', characterId: 'sanne-bakker' }, bidMail)
    const user = userEvent.setup()
    renderChatPanel()

    await user.click(screen.getByRole('button', { name: 'Verwijder bijlage Bod Oude Suikerfabriek' }))

    expect(screen.queryByText('Bod Oude Suikerfabriek')).not.toBeInTheDocument()
  })

  it('ChatPanel_givenMessageWithAttachment_expectLinkToMailOnComputer', async () => {
    mockFetch({
      [`GET ${MESSAGES_PATH}`]: () =>
        jsonResponse([{ ...playerMessage('1', 'bram', 'Kijk hier eens naar'), attachments: [bidMail] }]),
    })

    renderChatPanel()

    expect(await screen.findByRole('link', { name: /Bod Oude Suikerfabriek/ })).toHaveAttribute(
      'href',
      '/teams/team-1/zaken/het-lek/computer/werkplek?app=mail&item=bod',
    )
  })

  it('ChatPanel_givenTypedText_expectCharacterCountShown', async () => {
    mockFetch({ [`GET ${MESSAGES_PATH}`]: () => jsonResponse([]) })
    renderChatPanel()

    await userEvent.setup().type(screen.getByLabelText('Bericht aan Sanne Bakker'), 'Hallo Sanne')

    expect(screen.getByText('11 / 1000 tekens')).not.toHaveClass('near')
  })

  it('ChatPanel_givenTextNearLimit_expectCounterHighlighted', async () => {
    mockFetch({ [`GET ${MESSAGES_PATH}`]: () => jsonResponse([]) })
    const user = userEvent.setup()
    renderChatPanel()

    await user.click(screen.getByLabelText('Bericht aan Sanne Bakker'))
    await user.paste('x'.repeat(950))

    expect(screen.getByText('950 / 1000 tekens')).toHaveClass('near')
    expect(screen.getByRole('button', { name: 'Verstuur' })).toBeEnabled()
  })

  it('ChatPanel_givenPastedTextOverLimit_expectFullTextKeptAndSendBlocked', async () => {
    const fetchMock = mockFetch({ [`GET ${MESSAGES_PATH}`]: () => jsonResponse([]) })
    const user = userEvent.setup()
    renderChatPanel()
    const textarea = screen.getByLabelText('Bericht aan Sanne Bakker')

    await user.click(textarea)
    await user.paste('x'.repeat(1050))
    await user.keyboard('{Enter}')

    expect(textarea).toHaveValue('x'.repeat(1050))
    expect(textarea).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByRole('alert')).toHaveTextContent('Je bericht is 50 tekens te lang.')
    expect(screen.getByText('1050 / 1000 tekens')).toHaveClass('over')
    expect(screen.getByRole('button', { name: 'Verstuur' })).toBeDisabled()
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false)
  })
})
