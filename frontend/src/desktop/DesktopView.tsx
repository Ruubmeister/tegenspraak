import { useState, type ReactNode } from 'react'
import type { CharacterSummary, Desktop, DesktopApp } from '../api/types'
import { AppIcon } from './AppIcon'
import { BrowserAppView } from './apps/BrowserAppView'
import { CalendarAppView } from './apps/CalendarAppView'
import { FilesAppView } from './apps/FilesAppView'
import { MailAppView } from './apps/MailAppView'
import { PhotosAppView } from './apps/PhotosAppView'
import type { AppViewProps } from './apps/appViewProps'
import { useUnlockDesktop, type DesktopLocation } from './desktopApi'
import { TaskbarClock } from './TaskbarClock'
import { UnlockForm } from './UnlockForm'

/** An item to show right away, for example when following a chat attachment. */
export interface ItemSelection {
  appId: string
  itemId: string
}

interface DesktopViewProps {
  desktop: Desktop
  location: DesktopLocation
  /** The contacts an opened item can be attached for. */
  contacts: CharacterSummary[]
  initialSelection?: ItemSelection
}

/**
 * A virtual computer: a lock screen until the code is entered, then a wallpaper with app icons, one app window at a
 * time and a taskbar.
 */
export function DesktopView({ desktop, location, contacts, initialSelection }: DesktopViewProps) {
  return (
    <div className="desktop">
      {desktop.lock ? (
        <LockScreen desktop={desktop} location={location} />
      ) : (
        <OpenDesktop desktop={desktop} location={location} contacts={contacts} initialSelection={initialSelection} />
      )}
    </div>
  )
}

function LockScreen({ desktop, location }: { desktop: Desktop; location: DesktopLocation }) {
  const unlockDesktop = useUnlockDesktop(location)
  return (
    <div className="desktop-wallpaper lock-screen">
      <div className="lock-screen-card">
        <OwnerAvatar ownerName={desktop.ownerName} />
        <h2>{desktop.ownerName}</h2>
        <p>{desktop.description}</p>
        {desktop.lock && (
          <UnlockForm
            lock={desktop.lock}
            subject="deze computer"
            isPending={unlockDesktop.isPending}
            error={unlockDesktop.error}
            onSubmit={(code) => unlockDesktop.mutate(code)}
          />
        )}
      </div>
    </div>
  )
}

function OpenDesktop({ desktop, location, contacts, initialSelection }: DesktopViewProps) {
  const selectedApp = desktop.apps.find((app) => app.id === initialSelection?.appId)
  const [openAppId, setOpenAppId] = useState(selectedApp?.id)
  const [isMinimized, setMinimized] = useState(false)
  const [isMaximized, setMaximized] = useState(false)
  const openApp = desktop.apps.find((app) => app.id === openAppId)

  function launch(appId: string) {
    setOpenAppId(appId)
    setMinimized(false)
  }

  return (
    <div className="desktop-screen">
      <div className="desktop-wallpaper">
        <nav className="desktop-icons" aria-label="Programma's">
          {desktop.apps.length === 0 && <p className="status">Geen programma's.</p>}
          {desktop.apps.map((app) => (
            <button key={app.id} type="button" className="desktop-icon" onClick={() => launch(app.id)}>
              <AppIcon type={app.type} />
              <span>{app.title}</span>
            </button>
          ))}
        </nav>
        <div className="desktop-owner-widget">
          <h2>{desktop.ownerName}</h2>
          <p>{desktop.description}</p>
        </div>
        {openApp && (
          <AppWindow
            app={openApp}
            isMinimized={isMinimized}
            isMaximized={isMaximized}
            onMinimize={() => setMinimized(true)}
            onToggleMaximize={() => setMaximized(!isMaximized)}
            onClose={() => setOpenAppId(undefined)}
          >
            <DesktopAppView
              app={openApp}
              location={location}
              ownerName={desktop.ownerName}
              contacts={contacts}
              initialItemId={openApp.id === initialSelection?.appId ? initialSelection.itemId : undefined}
            />
          </AppWindow>
        )}
      </div>
      <footer className="taskbar">
        <span className="taskbar-owner">
          <OwnerAvatar ownerName={desktop.ownerName} />
          {desktop.ownerName}
        </span>
        {openApp && (
          <button
            type="button"
            className="taskbar-app"
            aria-label={`Venster ${openApp.title}`}
            aria-pressed={!isMinimized}
            onClick={() => setMinimized(!isMinimized)}
          >
            <AppIcon type={openApp.type} size="small" />
            {openApp.title}
          </button>
        )}
        <TaskbarClock />
      </footer>
    </div>
  )
}

interface AppWindowProps {
  app: DesktopApp
  isMinimized: boolean
  isMaximized: boolean
  onMinimize: () => void
  onToggleMaximize: () => void
  onClose: () => void
  children: ReactNode
}

/** A minimized window stays mounted, so the app keeps its selection when it is restored from the taskbar. */
function AppWindow({ app, isMinimized, isMaximized, onMinimize, onToggleMaximize, onClose, children }: AppWindowProps) {
  return (
    <section
      className={isMaximized ? 'app-window maximized' : 'app-window'}
      aria-label={app.title}
      hidden={isMinimized}
    >
      <header className="window-titlebar" onDoubleClick={onToggleMaximize}>
        <AppIcon type={app.type} size="small" />
        <span className="window-title">{app.title}</span>
        <div className="window-controls">
          <button type="button" aria-label="Minimaliseren" onClick={onMinimize}>
            <span aria-hidden="true">&#x2013;</span>
          </button>
          <button
            type="button"
            className="window-maximize"
            aria-label={isMaximized ? 'Verkleinen' : 'Maximaliseren'}
            onClick={onToggleMaximize}
          >
            <span aria-hidden="true">{isMaximized ? '❐' : '□'}</span>
          </button>
          <button type="button" className="window-close" aria-label="Sluiten" onClick={onClose}>
            <span aria-hidden="true">&times;</span>
          </button>
        </div>
      </header>
      <div className="window-body">{children}</div>
    </section>
  )
}

function OwnerAvatar({ ownerName }: { ownerName: string }) {
  const initials = ownerName
    .split(/\s+/)
    .filter((part) => /^\p{Lu}/u.test(part))
    .map((part) => part.charAt(0))
    .slice(0, 2)
    .join('')
  return (
    <span className="owner-avatar" aria-hidden="true">
      {initials || ownerName.charAt(0)}
    </span>
  )
}

function DesktopAppView({ app, ...props }: AppViewProps & { app: DesktopApp }) {
  switch (app.type) {
    case 'mail':
      return <MailAppView key={app.id} app={app} {...props} />
    case 'files':
      return <FilesAppView key={app.id} app={app} {...props} />
    case 'calendar':
      return <CalendarAppView key={app.id} app={app} {...props} />
    case 'photos':
      return <PhotosAppView key={app.id} app={app} {...props} />
    case 'browser':
      return <BrowserAppView key={app.id} app={app} {...props} />
  }
}
