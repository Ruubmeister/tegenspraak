import { useState } from 'react'
import type { CharacterSummary, Desktop, DesktopApp, Lock } from '../api/types'
import { FilesAppView } from './apps/FilesAppView'
import { MailAppView } from './apps/MailAppView'
import type { AppViewProps } from './apps/appViewProps'
import { useUnlockDesktop, type DesktopLocation } from './desktopApi'
import { UnlockForm } from './UnlockForm'

/** A mail or file to show right away, for example when following a chat attachment. */
export interface ItemSelection {
  appId: string
  itemId: string
}

interface DesktopViewProps {
  desktop: Desktop
  location: DesktopLocation
  /** The contacts an opened mail or file can be attached for. */
  contacts: CharacterSummary[]
  initialSelection?: ItemSelection
}

/** A virtual computer: a dock with the installed apps and a window showing the selected one. */
export function DesktopView({ desktop, location, contacts, initialSelection }: DesktopViewProps) {
  return (
    <div className="desktop">
      <header className="desktop-header">
        <h2>{desktop.ownerName}</h2>
        <p>{desktop.description}</p>
      </header>
      {desktop.lock ? (
        <LockedDesktop lock={desktop.lock} location={location} />
      ) : (
        <OpenDesktop desktop={desktop} location={location} contacts={contacts} initialSelection={initialSelection} />
      )}
    </div>
  )
}

function LockedDesktop({ lock, location }: { lock: Lock; location: DesktopLocation }) {
  const unlockDesktop = useUnlockDesktop(location)
  return (
    <div className="desktop-body desktop-locked">
      <UnlockForm
        lock={lock}
        subject="deze computer"
        isPending={unlockDesktop.isPending}
        error={unlockDesktop.error}
        onSubmit={(code) => unlockDesktop.mutate(code)}
      />
    </div>
  )
}

function OpenDesktop({ desktop, location, contacts, initialSelection }: DesktopViewProps) {
  const selectedApp = desktop.apps.find((app) => app.id === initialSelection?.appId)
  const [openAppId, setOpenAppId] = useState((selectedApp ?? desktop.apps[0])?.id)
  const openApp = desktop.apps.find((app) => app.id === openAppId)

  return (
    <div className="desktop-body">
      <nav className="desktop-dock" aria-label="Programma's">
        {desktop.apps.map((app) => (
          <button
            key={app.id}
            type="button"
            className={app.id === openAppId ? 'active' : undefined}
            aria-pressed={app.id === openAppId}
            onClick={() => setOpenAppId(app.id)}
          >
            {app.title}
          </button>
        ))}
      </nav>
      <div className="desktop-window">
        {openApp ? (
          <DesktopAppView
            app={openApp}
            location={location}
            ownerName={desktop.ownerName}
            contacts={contacts}
            initialItemId={openApp.id === initialSelection?.appId ? initialSelection.itemId : undefined}
          />
        ) : (
          <p className="status">Geen programma's.</p>
        )}
      </div>
    </div>
  )
}

function DesktopAppView({ app, ...props }: AppViewProps & { app: DesktopApp }) {
  switch (app.type) {
    case 'mail':
      return <MailAppView key={app.id} app={app} {...props} />
    case 'files':
      return <FilesAppView key={app.id} app={app} {...props} />
  }
}
