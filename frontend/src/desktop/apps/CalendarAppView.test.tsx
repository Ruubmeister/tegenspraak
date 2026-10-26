import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { CalendarApp, CharacterSummary } from '../../api/types'
import { readChatDraft } from '../../chat/chatDrafts'
import { renderWithProviders } from '../../test/testUtils'
import type { DesktopLocation } from '../desktopApi'
import { CalendarAppView } from './CalendarAppView'

const location: DesktopLocation = { teamId: 'team-1', caseId: 'het-lek', desktopId: 'werkplek' }

const sanne: CharacterSummary = { id: 'sanne-bakker', name: 'Sanne Bakker', role: 'Officemanager', description: '' }

const calendar: CalendarApp = {
  type: 'calendar',
  id: 'agenda',
  title: 'Agenda',
  events: [
    {
      id: 'etentje',
      title: 'Etentje ouders Dennis',
      start: '2026-03-14T19:00:00',
      end: '2026-03-14T22:00:00',
      location: 'Haarlem',
      attendees: ['Dennis', 'Ouders'],
      notes: 'Wijn meenemen.',
      lock: null,
    },
    {
      id: 'kapper',
      title: 'Kapper',
      start: '2026-02-03T09:00:00',
      end: '2026-02-03T09:30:00',
      location: null,
      attendees: [],
      notes: null,
      lock: null,
    },
    {
      id: 'geheim',
      title: 'Afspraak P.',
      start: '2026-03-12T21:00:00',
      end: '2026-03-12T22:00:00',
      location: null,
      attendees: [],
      notes: null,
      lock: { hint: 'Zijn verjaardag' },
    },
  ],
}

function renderCalendar(initialItemId?: string) {
  return renderWithProviders(
    <CalendarAppView
      app={calendar}
      location={location}
      ownerName="Sanne Bakker"
      contacts={[sanne]}
      initialItemId={initialItemId}
    />,
  )
}

describe('CalendarAppView', () => {
  it('CalendarAppView_givenEvents_expectMonthOfLatestEventWithItsEvents', () => {
    renderCalendar()

    expect(screen.getByRole('heading', { name: 'maart 2026' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Etentje ouders Dennis/ })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Kapper/ })).not.toBeInTheDocument()
  })

  it('CalendarAppView_givenPreviousMonthClicked_expectEventsOfThatMonth', async () => {
    const user = userEvent.setup()
    renderCalendar()

    await user.click(screen.getByRole('button', { name: 'Vorige maand' }))

    expect(screen.getByRole('heading', { name: 'februari 2026' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Kapper/ })).toBeInTheDocument()
  })

  it('CalendarAppView_givenMonthWithoutEvents_expectEmptyMessage', async () => {
    const user = userEvent.setup()
    renderCalendar()

    await user.click(screen.getByRole('button', { name: 'Volgende maand' }))

    expect(screen.getByText('Geen afspraken deze maand.')).toBeInTheDocument()
  })

  it('CalendarAppView_givenEventSelected_expectTimeLocationAttendeesAndNotes', async () => {
    const user = userEvent.setup()
    renderCalendar()

    await user.click(screen.getByRole('button', { name: /Etentje ouders Dennis/ }))

    expect(screen.getByText('zaterdag 14 maart 2026, 19:00 - 22:00')).toBeInTheDocument()
    expect(screen.getByText('Haarlem')).toBeInTheDocument()
    expect(screen.getByText('Dennis, Ouders')).toBeInTheDocument()
    expect(screen.getByText('Wijn meenemen.')).toBeInTheDocument()
  })

  it('CalendarAppView_givenLockedEventSelected_expectUnlockFormAndNoAttachButton', async () => {
    const user = userEvent.setup()
    renderCalendar()

    await user.click(screen.getByRole('button', { name: /Afspraak P\./ }))

    expect(screen.getByText('Deze afspraak is vergrendeld.')).toBeInTheDocument()
    expect(screen.getByText('Zijn verjaardag')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Voeg toe aan bericht' })).not.toBeInTheDocument()
  })

  it('CalendarAppView_givenInitialItemInOtherMonth_expectThatMonthAndEventShown', () => {
    renderCalendar('kapper')

    expect(screen.getByRole('heading', { name: 'februari 2026' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Kapper' })).toBeInTheDocument()
  })

  it('CalendarAppView_givenEventAttachedForContact_expectEventInDraft', async () => {
    const user = userEvent.setup()
    renderCalendar('etentje')

    await user.click(screen.getByRole('button', { name: 'Voeg toe aan bericht' }))
    await user.click(screen.getByRole('button', { name: 'Sanne Bakker' }))

    expect(readChatDraft({ teamId: 'team-1', caseId: 'het-lek', characterId: 'sanne-bakker' }).attachments).toEqual([
      {
        desktopId: 'werkplek',
        appId: 'agenda',
        itemId: 'etentje',
        kind: 'event',
        title: 'Etentje ouders Dennis',
        ownerName: 'Sanne Bakker',
      },
    ])
  })
})
