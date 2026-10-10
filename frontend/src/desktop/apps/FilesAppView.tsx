import { useState } from 'react'
import type { FilesApp } from '../../api/types'
import { AttachToChat } from '../../chat/AttachToChat'
import type { AppViewProps } from './appViewProps'
import { LockIcon } from '../UnlockForm'
import { folderNames } from './groupByFolder'
import { LockedItem } from './LockedItem'

export function FilesAppView({ app, location, ownerName, contacts, initialItemId }: AppViewProps & { app: FilesApp }) {
  const [selectedFileId, setSelectedFileId] = useState(app.files.find((file) => file.id === initialItemId)?.id)
  const selectedFile = app.files.find((file) => file.id === selectedFileId)

  return (
    <div className="files-app">
      <div className="app-sidebar file-tree">
        {folderNames(app.files).map((folder) => (
          <section key={folder}>
            <h3>{folder}</h3>
            <ul>
              {app.files
                .filter((file) => file.folder === folder)
                .map((file) => (
                  <li key={file.id}>
                    <button
                      type="button"
                      className={file.id === selectedFileId ? 'active' : undefined}
                      onClick={() => setSelectedFileId(file.id)}
                    >
                      {file.name} {file.lock && <LockIcon />}
                    </button>
                  </li>
                ))}
            </ul>
          </section>
        ))}
      </div>
      <div className="app-reader">
        {selectedFile ? (
          <article>
            <h3>{selectedFile.name}</h3>
            {selectedFile.lock ? (
              <LockedItem
                key={selectedFile.id}
                location={location}
                appId={app.id}
                itemId={selectedFile.id}
                lock={selectedFile.lock}
                subject="dit bestand"
              />
            ) : (
              <>
                <AttachToChat
                  key={selectedFile.id}
                  teamId={location.teamId}
                  caseId={location.caseId}
                  evidence={{
                    desktopId: location.desktopId,
                    appId: app.id,
                    itemId: selectedFile.id,
                    kind: 'file',
                    title: selectedFile.name,
                    ownerName,
                  }}
                  contacts={contacts}
                />
                <pre className="document-text">{selectedFile.content}</pre>
              </>
            )}
          </article>
        ) : (
          <p className="status">Selecteer een bestand.</p>
        )}
      </div>
    </div>
  )
}
