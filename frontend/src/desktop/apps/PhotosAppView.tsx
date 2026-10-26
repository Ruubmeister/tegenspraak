import { useState } from 'react'
import type { Photo, PhotosApp } from '../../api/types'
import { AttachToChat } from '../../chat/AttachToChat'
import { photoImagePath } from '../desktopApi'
import { LockIcon } from '../UnlockForm'
import type { AppViewProps } from './appViewProps'
import { BackToListButton } from './BackToListButton'
import { LockedItem } from './LockedItem'

const takenAtFormat = new Intl.DateTimeFormat('nl-NL', { dateStyle: 'long', timeStyle: 'short' })

/** Albums next to a grid of thumbnails; a selected photo opens large with its date and description. */
export function PhotosAppView({
  app,
  location,
  ownerName,
  contacts,
  initialItemId,
}: AppViewProps & { app: PhotosApp }) {
  const albums = [...new Set(app.photos.map((photo) => photo.album))]
  const initialPhoto = app.photos.find((photo) => photo.id === initialItemId)
  const [selectedAlbum, setSelectedAlbum] = useState(initialPhoto?.album ?? albums[0])
  const [selectedPhotoId, setSelectedPhotoId] = useState(initialPhoto?.id)
  const photosInAlbum = app.photos.filter((photo) => photo.album === selectedAlbum)
  const selectedPhoto = app.photos.find((photo) => photo.id === selectedPhotoId)

  return (
    <div className={selectedPhoto ? 'photos-app showing-item' : 'photos-app'}>
      <ul className="app-sidebar" aria-label="Albums">
        {albums.map((album) => (
          <li key={album}>
            <button
              type="button"
              className={album === selectedAlbum ? 'active' : undefined}
              onClick={() => {
                setSelectedAlbum(album)
                setSelectedPhotoId(undefined)
              }}
            >
              {album}
            </button>
          </li>
        ))}
      </ul>
      {selectedPhoto ? (
        <div className="app-reader photo-viewer">
          <BackToListButton label={selectedAlbum} onClick={() => setSelectedPhotoId(undefined)} />
          <article>
            <h3>{selectedPhoto.title}</h3>
            {selectedPhoto.takenAt && (
              <p className="photo-taken-at">{takenAtFormat.format(new Date(selectedPhoto.takenAt))}</p>
            )}
            {selectedPhoto.lock ? (
              <LockedItem
                key={selectedPhoto.id}
                location={location}
                appId={app.id}
                itemId={selectedPhoto.id}
                lock={selectedPhoto.lock}
                subject="deze foto"
              />
            ) : (
              <>
                <AttachToChat
                  key={selectedPhoto.id}
                  teamId={location.teamId}
                  caseId={location.caseId}
                  evidence={{
                    desktopId: location.desktopId,
                    appId: app.id,
                    itemId: selectedPhoto.id,
                    kind: 'photo',
                    title: selectedPhoto.title,
                    ownerName,
                  }}
                  contacts={contacts}
                />
                <figure>
                  <img src={photoImagePath(location, app.id, selectedPhoto.id)} alt={selectedPhoto.description ?? ''} />
                  <figcaption>{selectedPhoto.description}</figcaption>
                </figure>
              </>
            )}
          </article>
        </div>
      ) : (
        <ul className="photo-grid" aria-label="Foto's">
          {photosInAlbum.map((photo) => (
            <li key={photo.id}>
              <PhotoThumbnail
                photo={photo}
                imagePath={photoImagePath(location, app.id, photo.id)}
                onOpen={() => setSelectedPhotoId(photo.id)}
              />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

interface PhotoThumbnailProps {
  photo: Photo
  imagePath: string
  onOpen: () => void
}

/** A locked photo has no image yet, so it shows a lock in its place. */
function PhotoThumbnail({ photo, imagePath, onOpen }: PhotoThumbnailProps) {
  return (
    <button type="button" className="photo-thumbnail" onClick={onOpen}>
      {photo.lock ? (
        <span className="photo-thumbnail-locked" aria-hidden="true">
          <LockIcon />
        </span>
      ) : (
        <img src={imagePath} alt="" loading="lazy" />
      )}
      <span className="photo-thumbnail-title">
        {photo.title} {photo.lock && <LockIcon />}
      </span>
    </button>
  )
}
