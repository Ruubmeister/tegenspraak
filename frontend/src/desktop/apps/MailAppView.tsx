import { useState } from 'react'
import type { CharacterSummary, MailApp, MailMessage } from '../../api/types'
import { AttachToChat } from '../../chat/AttachToChat'
import type { DesktopLocation } from '../desktopApi'
import type { AppViewProps } from './appViewProps'
import { LockIcon } from '../UnlockForm'
import { folderNames } from './groupByFolder'
import { LockedItem } from './LockedItem'

const dateTimeFormat = new Intl.DateTimeFormat('nl-NL', { dateStyle: 'medium', timeStyle: 'short' })

export function MailAppView({ app, location, ownerName, contacts, initialItemId }: AppViewProps & { app: MailApp }) {
  const folders = folderNames(app.messages)
  const initialMessage = app.messages.find((message) => message.id === initialItemId)
  const [selectedFolder, setSelectedFolder] = useState(initialMessage?.folder ?? folders[0])
  const [selectedMessageId, setSelectedMessageId] = useState(initialMessage?.id)
  const messagesInFolder = app.messages.filter((message) => message.folder === selectedFolder)
  const selectedMessage = app.messages.find((message) => message.id === selectedMessageId)

  return (
    <div className="mail-app">
      <ul className="app-sidebar" aria-label="Mappen">
        {folders.map((folder) => (
          <li key={folder}>
            <button
              type="button"
              className={folder === selectedFolder ? 'active' : undefined}
              onClick={() => {
                setSelectedFolder(folder)
                setSelectedMessageId(undefined)
              }}
            >
              {folder}
            </button>
          </li>
        ))}
      </ul>
      <ul className="mail-list" aria-label="Berichten">
        {messagesInFolder.map((message) => (
          <li key={message.id}>
            <button
              type="button"
              className={message.id === selectedMessageId ? 'active' : undefined}
              onClick={() => setSelectedMessageId(message.id)}
            >
              <span className="mail-list-subject">
                {message.subject} {message.lock && <LockIcon />}
              </span>
              <span className="mail-list-meta">
                {message.from} · {dateTimeFormat.format(new Date(message.sentAt))}
              </span>
            </button>
          </li>
        ))}
      </ul>
      <div className="app-reader">
        {selectedMessage ? (
          <MailReader
            message={selectedMessage}
            appId={app.id}
            location={location}
            ownerName={ownerName}
            contacts={contacts}
          />
        ) : (
          <p className="status">Selecteer een bericht.</p>
        )}
      </div>
    </div>
  )
}

interface MailReaderProps {
  message: MailMessage
  appId: string
  location: DesktopLocation
  ownerName: string
  contacts: CharacterSummary[]
}

function MailReader({ message, appId, location, ownerName, contacts }: MailReaderProps) {
  return (
    <article>
      <h3>{message.subject}</h3>
      <dl className="mail-headers">
        <dt>Van</dt>
        <dd>{message.from}</dd>
        <dt>Aan</dt>
        <dd>{message.to}</dd>
        <dt>Datum</dt>
        <dd>{dateTimeFormat.format(new Date(message.sentAt))}</dd>
      </dl>
      {message.lock ? (
        <LockedItem
          key={message.id}
          location={location}
          appId={appId}
          itemId={message.id}
          lock={message.lock}
          subject="deze mail"
        />
      ) : (
        <>
          <AttachToChat
            key={message.id}
            teamId={location.teamId}
            caseId={location.caseId}
            evidence={{
              desktopId: location.desktopId,
              appId,
              itemId: message.id,
              kind: 'mail',
              title: message.subject,
              ownerName,
            }}
            contacts={contacts}
          />
          <pre className="document-text">{message.body}</pre>
        </>
      )}
    </article>
  )
}
