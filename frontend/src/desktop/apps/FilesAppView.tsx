import { useState } from 'react'
import type { FilesApp } from '../../api/types'
import { folderNames } from './groupByFolder'

export function FilesAppView({ app }: { app: FilesApp }) {
  const [selectedFileId, setSelectedFileId] = useState<string>()
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
                      {file.name}
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
            <pre className="document-text">{selectedFile.content}</pre>
          </article>
        ) : (
          <p className="status">Selecteer een bestand.</p>
        )}
      </div>
    </div>
  )
}
