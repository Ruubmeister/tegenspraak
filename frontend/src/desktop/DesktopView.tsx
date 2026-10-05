import { useState } from 'react'
import type { Desktop, DesktopApp } from '../api/types'
import { FilesAppView } from './apps/FilesAppView'
import { MailAppView } from './apps/MailAppView'

/** A virtual computer: a dock with the installed apps and a window showing the selected one. */
export function DesktopView({ desktop }: { desktop: Desktop }) {
  const [openAppId, setOpenAppId] = useState(desktop.apps[0]?.id)
  const openApp = desktop.apps.find((app) => app.id === openAppId)

  return (
    <div className="desktop">
      <header className="desktop-header">
        <h2>{desktop.ownerName}</h2>
        <p>{desktop.description}</p>
      </header>
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
          {openApp ? <DesktopAppView app={openApp} /> : <p className="status">Geen programma's.</p>}
        </div>
      </div>
    </div>
  )
}

function DesktopAppView({ app }: { app: DesktopApp }) {
  switch (app.type) {
    case 'mail':
      return <MailAppView key={app.id} app={app} />
    case 'files':
      return <FilesAppView key={app.id} app={app} />
  }
}
