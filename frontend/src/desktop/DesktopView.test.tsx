import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { CharacterSummary, Desktop, FileEntry } from '../api/types'
import { readChatDraft } from '../chat/chatDrafts'
import { jsonResponse, mockFetch, renderWithProviders, statusResponse } from '../test/testUtils'
import type { DesktopLocation } from './desktopApi'
import { DesktopView, type ItemSelection } from './DesktopView'

const location: DesktopLocation = { teamId: 'team-1', caseId: 'het-lek', desktopId: 'werkplek' }
const DESKTOP_PATH = '/api/teams/team-1/cases/het-lek/desktops/werkplek'

const lockedDiary: FileEntry = {
  id: 'dagboek',
  folder: 'Persoonlijk',
  name: 'dagboek.txt',
  content: null,
  lock: { hint: 'Wachtwoordhint: etentje (ddmm)' },
}

const desktop: Desktop = {
  id: 'werkplek',
  ownerName: 'Sanne Bakker',
  ownerType: 'person',
  description: 'Werklaptop',
  lock: null,
  apps: [
    {
      type: 'mail',
      id: 'mail',
      title: 'Mail',
      messages: [
        {
          id: 'inbox-1',
          folder: 'Inbox',
          from: 'pieter@example.nl',
          to: 'sanne@example.nl',
          subject: 'Bod',
          sentAt: '2026-03-11T16:02:00',
          body: 'We gaan voor 4.250.000.',
          lock: null,
        },
        {
          id: 'sent-1',
          folder: 'Verzonden',
          from: 'sanne@example.nl',
          to: 'privé@example.nl',
          subject: 'FW: Bod',
          sentAt: '2026-03-12T21:47:00',
          body: 'even thuis afmaken',
          lock: null,
        },
      ],
    },
    {
      type: 'files',
      id: 'bestanden',
      title: 'Bestanden',
      files: [
        { id: 'notities', folder: 'Documenten', name: 'notities.txt', content: 'bod definitief maken', lock: null },
        lockedDiary,
      ],
    },
  ],
}

const sanne: CharacterSummary = {
  id: 'sanne-bakker',
  name: 'Sanne Bakker',
  role: 'Officemanager',
  description: 'Werkt er al acht jaar.',
}

function renderDesktop(shownDesktop: Desktop, initialSelection?: ItemSelection) {
  return renderWithProviders(
    <DesktopView desktop={shownDesktop} location={location} contacts={[sanne]} initialSelection={initialSelection} />,
  )
}

function withDiary(diary: FileEntry): Desktop {
  return {
    ...desktop,
    apps: desktop.apps.map((app) =>
      app.type === 'files' ? { ...app, files: app.files.map((file) => (file.id === diary.id ? diary : file)) } : app,
    ),
  }
}

async function openDiary() {
  const user = userEvent.setup()
  await user.click(screen.getByRole('button', { name: 'Bestanden' }))
  await user.click(screen.getByRole('button', { name: /dagboek\.txt/ }))
  return user
}

describe('DesktopView', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('DesktopView_givenMailApp_expectOnlyFirstFolderListed', () => {
    renderDesktop(desktop)

    expect(screen.getByRole('button', { name: /^Bod/ })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /FW: Bod/ })).not.toBeInTheDocument()
  })

  it('DesktopView_givenOtherFolderAndMessageSelected_expectMessageBodyShown', async () => {
    const user = userEvent.setup()
    renderDesktop(desktop)

    await user.click(screen.getByRole('button', { name: 'Verzonden' }))
    await user.click(screen.getByRole('button', { name: /FW: Bod/ }))

    expect(screen.getByText('even thuis afmaken')).toBeInTheDocument()
    expect(screen.getByText('privé@example.nl')).toBeInTheDocument()
  })

  it('DesktopView_givenFilesAppOpened_expectFileContentShownOnSelect', async () => {
    const user = userEvent.setup()
    renderDesktop(desktop)

    await user.click(screen.getByRole('button', { name: 'Bestanden' }))
    await user.click(screen.getByRole('button', { name: 'notities.txt' }))

    expect(screen.getByText('bod definitief maken')).toBeInTheDocument()
  })

  it('DesktopView_givenLockedFile_expectHintInsteadOfContent', async () => {
    renderDesktop(desktop)

    await openDiary()

    expect(screen.getByText('Dit bestand is vergrendeld.')).toBeInTheDocument()
    expect(screen.getByText('Wachtwoordhint: etentje (ddmm)')).toBeInTheDocument()
  })

  it('DesktopView_givenCorrectCode_expectUnlockRequestSent', async () => {
    const fetchMock = mockFetch({
      [`POST ${DESKTOP_PATH}/apps/bestanden/items/dagboek/unlock`]: () =>
        jsonResponse(withDiary({ ...lockedDiary, content: 'Bod is binnen!', lock: null })),
      'GET /api/teams/team-1/cases/het-lek': () => jsonResponse({}),
    })
    renderDesktop(desktop)
    const user = await openDiary()

    await user.type(screen.getByLabelText('Code'), ' 1403 ')
    await user.click(screen.getByRole('button', { name: 'Ontgrendelen' }))

    const unlockCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST')
    expect(unlockCall?.[1]?.body).toBe('{"code":"1403"}')
  })

  it('DesktopView_givenWrongCode_expectErrorMessage', async () => {
    mockFetch({ [`POST ${DESKTOP_PATH}/apps/bestanden/items/dagboek/unlock`]: () => statusResponse(422) })
    renderDesktop(desktop)
    const user = await openDiary()

    await user.type(screen.getByLabelText('Code'), '1203')
    await user.click(screen.getByRole('button', { name: 'Ontgrendelen' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Deze code klopt niet.')
  })

  it('DesktopView_givenTooManyWrongCodes_expectWaitMessage', async () => {
    mockFetch({ [`POST ${DESKTOP_PATH}/apps/bestanden/items/dagboek/unlock`]: () => statusResponse(429) })
    renderDesktop(desktop)
    const user = await openDiary()

    await user.type(screen.getByLabelText('Code'), '1203')
    await user.click(screen.getByRole('button', { name: 'Ontgrendelen' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Te veel foute codes.')
  })

  it('DesktopView_givenLockedDesktop_expectUnlockFormAndNoApps', () => {
    renderDesktop({ ...desktop, lock: { hint: 'Het jaartal' }, apps: [] })

    expect(screen.getByText('Deze computer is vergrendeld.')).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: "Programma's" })).not.toBeInTheDocument()
  })

  it('DesktopView_givenInitialSelectionInOtherFolder_expectMailOpened', () => {
    renderDesktop(desktop, { appId: 'mail', itemId: 'sent-1' })

    expect(screen.getByText('even thuis afmaken')).toBeInTheDocument()
  })

  it('DesktopView_givenInitialSelectionInFilesApp_expectFileOpened', () => {
    renderDesktop(desktop, { appId: 'bestanden', itemId: 'notities' })

    expect(screen.getByText('bod definitief maken')).toBeInTheDocument()
  })

  it('DesktopView_givenMailAttachedForContact_expectMailInDraftOfThatChat', async () => {
    const user = userEvent.setup()
    renderDesktop(desktop)
    await user.click(screen.getByRole('button', { name: /^Bod/ }))

    await user.click(screen.getByRole('button', { name: 'Voeg toe aan bericht' }))
    await user.click(screen.getByRole('button', { name: 'Sanne Bakker' }))

    expect(screen.getByRole('status')).toHaveTextContent('Toegevoegd aan je bericht aan Sanne Bakker.')
    expect(screen.getByRole('link', { name: 'Naar gesprek met Sanne Bakker' })).toHaveAttribute(
      'href',
      '/teams/team-1/zaken/het-lek/chat/sanne-bakker',
    )
    expect(readChatDraft({ teamId: 'team-1', caseId: 'het-lek', characterId: 'sanne-bakker' }).attachments).toEqual([
      {
        desktopId: 'werkplek',
        appId: 'mail',
        itemId: 'inbox-1',
        kind: 'mail',
        title: 'Bod',
        ownerName: 'Sanne Bakker',
      },
    ])
  })

  it('DesktopView_givenLockedFile_expectNoAttachButton', async () => {
    renderDesktop(desktop)

    await openDiary()

    expect(screen.queryByRole('button', { name: 'Voeg toe aan bericht' })).not.toBeInTheDocument()
  })
})
