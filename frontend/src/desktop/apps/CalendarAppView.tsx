import { useState } from 'react'
import type { CalendarApp, CalendarEvent } from '../../api/types'
import { AttachToChat } from '../../chat/AttachToChat'
import { LockIcon } from '../UnlockForm'
import type { AppViewProps } from './appViewProps'
import { BackToListButton } from './BackToListButton'
import { LockedItem } from './LockedItem'

const monthFormat = new Intl.DateTimeFormat('nl-NL', { month: 'long', year: 'numeric' })
const dayFormat = new Intl.DateTimeFormat('nl-NL', { weekday: 'long', day: 'numeric', month: 'long' })
const timeFormat = new Intl.DateTimeFormat('nl-NL', { hour: '2-digit', minute: '2-digit' })
const fullDateFormat = new Intl.DateTimeFormat('nl-NL', { dateStyle: 'full' })
const WEEKDAYS = ['ma', 'di', 'wo', 'do', 'vr', 'za', 'zo']

/** A month is identified by the year and the zero based month, like Date does. */
interface Month {
  year: number
  month: number
}

/** A month grid with the events on their start day; selecting an event shows its details next to the grid. */
export function CalendarAppView({
  app,
  location,
  ownerName,
  contacts,
  initialItemId,
}: AppViewProps & { app: CalendarApp }) {
  const events = [...app.events].sort((first, second) => first.start.localeCompare(second.start))
  const initialEvent = events.find((event) => event.id === initialItemId)
  const [shownMonth, setShownMonth] = useState(() => monthOf(initialEvent ?? events.at(-1)))
  const [selectedEventId, setSelectedEventId] = useState(initialEvent?.id)
  const selectedEvent = events.find((event) => event.id === selectedEventId)

  return (
    <div className={selectedEvent ? 'calendar-app showing-item' : 'calendar-app'}>
      <div className="calendar-month">
        <header className="calendar-toolbar">
          <button type="button" aria-label="Vorige maand" onClick={() => setShownMonth(addMonths(shownMonth, -1))}>
            <span aria-hidden="true">&lsaquo;</span>
          </button>
          <h3 aria-live="polite">{monthFormat.format(new Date(shownMonth.year, shownMonth.month, 1))}</h3>
          <button type="button" aria-label="Volgende maand" onClick={() => setShownMonth(addMonths(shownMonth, 1))}>
            <span aria-hidden="true">&rsaquo;</span>
          </button>
        </header>
        <MonthGrid month={shownMonth} events={events} selectedEventId={selectedEventId} onSelect={setSelectedEventId} />
      </div>
      <div className="app-reader calendar-details">
        {selectedEvent ? (
          <article>
            <BackToListButton label={app.title} onClick={() => setSelectedEventId(undefined)} />
            <h3>{selectedEvent.title}</h3>
            <p className="calendar-event-when">{formatEventTime(selectedEvent)}</p>
            {selectedEvent.lock ? (
              <LockedItem
                key={selectedEvent.id}
                location={location}
                appId={app.id}
                itemId={selectedEvent.id}
                lock={selectedEvent.lock}
                subject="deze afspraak"
              />
            ) : (
              <>
                <EventDetails event={selectedEvent} />
                <AttachToChat
                  key={selectedEvent.id}
                  teamId={location.teamId}
                  caseId={location.caseId}
                  evidence={{
                    desktopId: location.desktopId,
                    appId: app.id,
                    itemId: selectedEvent.id,
                    kind: 'event',
                    title: selectedEvent.title,
                    ownerName,
                  }}
                  contacts={contacts}
                />
                {selectedEvent.notes && <p className="calendar-notes">{selectedEvent.notes}</p>}
              </>
            )}
          </article>
        ) : (
          <p className="status">Selecteer een afspraak.</p>
        )}
      </div>
    </div>
  )
}

interface MonthGridProps {
  month: Month
  events: CalendarEvent[]
  selectedEventId?: string
  onSelect: (eventId: string) => void
}

/** Days with events have the class has-events, so small screens can show them as an agenda list instead. */
function MonthGrid({ month, events, selectedEventId, onSelect }: MonthGridProps) {
  const days = gridDays(month)
  const hasEventsInMonth = days.some((day) => day.inMonth && eventsOn(events, day.key).length > 0)
  return (
    <>
      <ol className="calendar-grid" aria-label="Dagen">
        {WEEKDAYS.map((weekday) => (
          <li key={weekday} className="calendar-weekday" aria-hidden="true">
            {weekday}
          </li>
        ))}
        {days.map((day) => {
          const dayEvents = eventsOn(events, day.key)
          const classNames = ['calendar-day']
          if (!day.inMonth) {
            classNames.push('outside-month')
          }
          if (day.inMonth && dayEvents.length > 0) {
            classNames.push('has-events')
          }
          return (
            <li key={day.key} className={classNames.join(' ')}>
              <span className="calendar-day-number" aria-label={dayFormat.format(day.date)}>
                {day.date.getDate()}
              </span>
              <ul>
                {dayEvents.map((event) => (
                  <li key={event.id}>
                    <button
                      type="button"
                      className={event.id === selectedEventId ? 'calendar-event active' : 'calendar-event'}
                      onClick={() => onSelect(event.id)}
                    >
                      <span className="calendar-event-time">{timeFormat.format(new Date(event.start))}</span>{' '}
                      {event.title} {event.lock && <LockIcon />}
                    </button>
                  </li>
                ))}
              </ul>
            </li>
          )
        })}
      </ol>
      {!hasEventsInMonth && <p className="status calendar-empty">Geen afspraken deze maand.</p>}
    </>
  )
}

function EventDetails({ event }: { event: CalendarEvent }) {
  if (!event.location && event.attendees.length === 0) {
    return null
  }
  return (
    <dl className="mail-headers">
      {event.location && (
        <>
          <dt>Locatie</dt>
          <dd>{event.location}</dd>
        </>
      )}
      {event.attendees.length > 0 && (
        <>
          <dt>Deelnemers</dt>
          <dd>{event.attendees.join(', ')}</dd>
        </>
      )}
    </dl>
  )
}

function formatEventTime(event: CalendarEvent): string {
  const start = new Date(event.start)
  const end = new Date(event.end)
  const startText = `${fullDateFormat.format(start)}, ${timeFormat.format(start)}`
  if (dateKey(start) === dateKey(end)) {
    return `${startText} - ${timeFormat.format(end)}`
  }
  return `${startText} - ${fullDateFormat.format(end)}, ${timeFormat.format(end)}`
}

/** The month of an event, or the current month for an empty calendar. */
function monthOf(event: CalendarEvent | undefined): Month {
  const date = event ? new Date(event.start) : new Date()
  return { year: date.getFullYear(), month: date.getMonth() }
}

function addMonths({ year, month }: Month, count: number): Month {
  const date = new Date(year, month + count, 1)
  return { year: date.getFullYear(), month: date.getMonth() }
}

interface GridDay {
  key: string
  date: Date
  inMonth: boolean
}

/** Whole weeks from Monday, covering every day of the month. */
function gridDays({ year, month }: Month): GridDay[] {
  const first = new Date(year, month, 1)
  const daysBefore = (first.getDay() + 6) % 7
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const cellCount = Math.ceil((daysBefore + daysInMonth) / 7) * 7
  return Array.from({ length: cellCount }, (_, index) => {
    const date = new Date(year, month, index - daysBefore + 1)
    return { key: dateKey(date), date, inMonth: date.getMonth() === month }
  })
}

function eventsOn(events: CalendarEvent[], key: string): CalendarEvent[] {
  return events.filter((event) => dateKey(new Date(event.start)) === key)
}

function dateKey(date: Date): string {
  return `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`
}
