import { useState } from 'react'
import type { MailApp, MailMessage } from '../../api/types'
import { folderNames } from './groupByFolder'

const dateTimeFormat = new Intl.DateTimeFormat('nl-NL', { dateStyle: 'medium', timeStyle: 'short' })

export function MailAppView({ app }: { app: MailApp }) {
  const folders = folderNames(app.messages)
  const [selectedFolder, setSelectedFolder] = useState(folders[0])
  const [selectedMessageId, setSelectedMessageId] = useState<string>()
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
              <span className="mail-list-subject">{message.subject}</span>
              <span className="mail-list-meta">
                {message.from} · {dateTimeFormat.format(new Date(message.sentAt))}
              </span>
            </button>
          </li>
        ))}
      </ul>
      <div className="app-reader">
        {selectedMessage ? <MailReader message={selectedMessage} /> : <p className="status">Selecteer een bericht.</p>}
      </div>
    </div>
  )
}

function MailReader({ message }: { message: MailMessage }) {
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
      <pre className="document-text">{message.body}</pre>
    </article>
  )
}
