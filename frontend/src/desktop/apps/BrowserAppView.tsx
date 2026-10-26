import { useState } from 'react'
import type { BrowserApp, BrowserPage } from '../../api/types'
import { AttachToChat } from '../../chat/AttachToChat'
import { LockIcon } from '../UnlockForm'
import type { AppViewProps } from './appViewProps'
import { LockedItem } from './LockedItem'

const HISTORY = 'history'
const HISTORY_ADDRESS = 'browser://geschiedenis'
const dayFormat = new Intl.DateTimeFormat('nl-NL', { dateStyle: 'full' })
const timeFormat = new Intl.DateTimeFormat('nl-NL', { hour: '2-digit', minute: '2-digit' })

interface Navigation {
  /** Each entry is the history page or the id of a visited page. */
  entries: string[]
  position: number
}

/**
 * A browser that opens on its history. Pages from the history open with their text, and back and forward move through
 * what the player opened, like in a real browser.
 */
export function BrowserAppView({
  app,
  location,
  ownerName,
  contacts,
  initialItemId,
}: AppViewProps & { app: BrowserApp }) {
  const initialPage = app.pages.find((page) => page.id === initialItemId)
  const [navigation, setNavigation] = useState<Navigation>(
    initialPage ? { entries: [HISTORY, initialPage.id], position: 1 } : { entries: [HISTORY], position: 0 },
  )
  const [search, setSearch] = useState('')
  const current = navigation.entries[navigation.position]
  const currentPage = app.pages.find((page) => page.id === current)

  function open(target: string) {
    if (target === current) {
      return
    }
    const entries = [...navigation.entries.slice(0, navigation.position + 1), target]
    setNavigation({ entries, position: entries.length - 1 })
  }

  function move(step: number) {
    setNavigation({ ...navigation, position: navigation.position + step })
  }

  return (
    <div className="browser-app">
      <div className="browser-tabs">
        <span className="browser-tab">{currentPage ? currentPage.title : 'Geschiedenis'}</span>
      </div>
      <div className="browser-toolbar">
        <button type="button" aria-label="Terug" disabled={navigation.position === 0} onClick={() => move(-1)}>
          <span aria-hidden="true">&larr;</span>
        </button>
        <button
          type="button"
          aria-label="Vooruit"
          disabled={navigation.position === navigation.entries.length - 1}
          onClick={() => move(1)}
        >
          <span aria-hidden="true">&rarr;</span>
        </button>
        <button type="button" aria-label="Geschiedenis" onClick={() => open(HISTORY)}>
          <span aria-hidden="true">&#x21BA;</span>
        </button>
        <input
          className="browser-address"
          aria-label="Adres"
          value={currentPage ? currentPage.url : HISTORY_ADDRESS}
          readOnly
        />
      </div>
      <div className="browser-viewport">
        {currentPage ? (
          <article className="browser-page">
            <h3>{currentPage.title}</h3>
            <p className="browser-visited-at">
              Bezocht op {dayFormat.format(new Date(currentPage.visitedAt))} om{' '}
              {timeFormat.format(new Date(currentPage.visitedAt))}
            </p>
            {currentPage.lock ? (
              <LockedItem
                key={currentPage.id}
                location={location}
                appId={app.id}
                itemId={currentPage.id}
                lock={currentPage.lock}
                subject="deze pagina"
              />
            ) : (
              <>
                <AttachToChat
                  key={currentPage.id}
                  teamId={location.teamId}
                  caseId={location.caseId}
                  evidence={{
                    desktopId: location.desktopId,
                    appId: app.id,
                    itemId: currentPage.id,
                    kind: 'webpage',
                    title: currentPage.title,
                    ownerName,
                  }}
                  contacts={contacts}
                />
                <div className="browser-page-text">{currentPage.content}</div>
              </>
            )}
          </article>
        ) : (
          <BrowserHistory pages={app.pages} search={search} onSearch={setSearch} onOpen={open} />
        )}
      </div>
    </div>
  )
}

interface BrowserHistoryProps {
  pages: BrowserPage[]
  search: string
  onSearch: (search: string) => void
  onOpen: (pageId: string) => void
}

/** Visits grouped by day, most recent first, filtered on title and address. */
function BrowserHistory({ pages, search, onSearch, onOpen }: BrowserHistoryProps) {
  const query = search.trim().toLowerCase()
  const visits = pages
    .filter((page) => `${page.title} ${page.url}`.toLowerCase().includes(query))
    .sort((first, second) => second.visitedAt.localeCompare(first.visitedAt))
  const days = [...new Set(visits.map((page) => dayFormat.format(new Date(page.visitedAt))))]

  return (
    <section className="browser-history">
      <h3>Geschiedenis</h3>
      <input
        type="search"
        className="browser-history-search"
        aria-label="Zoeken in geschiedenis"
        placeholder="Zoeken in geschiedenis"
        value={search}
        onChange={(event) => onSearch(event.target.value)}
      />
      {visits.length === 0 && <p className="status">Geen bezochte pagina's gevonden.</p>}
      {days.map((day) => (
        <section key={day} className="browser-history-day">
          <h4>{day}</h4>
          <ul>
            {visits
              .filter((page) => dayFormat.format(new Date(page.visitedAt)) === day)
              .map((page) => (
                <li key={page.id}>
                  <button type="button" onClick={() => onOpen(page.id)}>
                    <span className="browser-history-time">{timeFormat.format(new Date(page.visitedAt))}</span>
                    <span className="browser-history-favicon" aria-hidden="true">
                      {hostName(page.url).charAt(0).toUpperCase()}
                    </span>
                    <span className="browser-history-title">
                      {page.title} {page.lock && <LockIcon />}
                    </span>
                    <span className="browser-history-host">{hostName(page.url)}</span>
                  </button>
                </li>
              ))}
          </ul>
        </section>
      ))}
    </section>
  )
}

/** The host without "www.", or the whole address when it is no valid URL. */
function hostName(url: string): string {
  try {
    return new URL(url).hostname.replace(/^www\./, '')
  } catch {
    return url
  }
}
