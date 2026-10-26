import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { PhotosApp } from '../../api/types'
import { renderWithProviders } from '../../test/testUtils'
import type { DesktopLocation } from '../desktopApi'
import { PhotosAppView } from './PhotosAppView'

const location: DesktopLocation = { teamId: 'team-1', caseId: 'het-lek', desktopId: 'werkplek' }
const IMAGE_PATH = '/api/teams/team-1/cases/het-lek/desktops/werkplek/apps/fotos/items'

const photos: PhotosApp = {
  type: 'photos',
  id: 'fotos',
  title: "Foto's",
  photos: [
    {
      id: 'strand',
      album: 'Vakantie',
      title: 'Strand Zandvoort',
      takenAt: '2025-07-04T15:30:00',
      description: 'Sanne en Dennis op het strand.',
      lock: null,
    },
    {
      id: 'kantoor',
      album: 'Werk',
      title: 'Borrel kantoor',
      takenAt: null,
      description: 'Het team met een taart.',
      lock: null,
    },
    {
      id: 'bewijs',
      album: 'Vakantie',
      title: 'Screenshot',
      takenAt: null,
      description: null,
      lock: { hint: 'Naam van de hond' },
    },
  ],
}

function renderPhotos(initialItemId?: string) {
  return renderWithProviders(
    <PhotosAppView
      app={photos}
      location={location}
      ownerName="Sanne Bakker"
      contacts={[]}
      initialItemId={initialItemId}
    />,
  )
}

describe('PhotosAppView', () => {
  it('PhotosAppView_givenAlbums_expectPhotosOfFirstAlbumOnly', () => {
    renderPhotos()

    const grid = screen.getByRole('list', { name: "Foto's" })
    expect(grid).toHaveTextContent('Strand Zandvoort')
    expect(grid).toHaveTextContent('Screenshot')
    expect(grid).not.toHaveTextContent('Borrel kantoor')
  })

  it('PhotosAppView_givenOtherAlbumSelected_expectItsPhotos', async () => {
    const user = userEvent.setup()
    renderPhotos()

    await user.click(screen.getByRole('button', { name: 'Werk' }))

    expect(screen.getByRole('list', { name: "Foto's" })).toHaveTextContent('Borrel kantoor')
  })

  it('PhotosAppView_givenPhotoOpened_expectImageFromImageEndpointDateAndDescription', async () => {
    const user = userEvent.setup()
    renderPhotos()

    await user.click(screen.getByRole('button', { name: 'Strand Zandvoort' }))

    expect(screen.getByRole('img', { name: 'Sanne en Dennis op het strand.' })).toHaveAttribute(
      'src',
      `${IMAGE_PATH}/strand/image`,
    )
    expect(screen.getByText('4 juli 2025 om 15:30')).toBeInTheDocument()
  })

  it('PhotosAppView_givenLockedPhotoOpened_expectUnlockFormAndNoImage', async () => {
    const user = userEvent.setup()
    renderPhotos()

    await user.click(screen.getByRole('button', { name: /Screenshot/ }))

    expect(screen.getByText('Deze foto is vergrendeld.')).toBeInTheDocument()
    expect(screen.queryByRole('figure')).not.toBeInTheDocument()
  })

  it('PhotosAppView_givenOpenedPhotoAndBackClicked_expectAlbumGridAgain', async () => {
    const user = userEvent.setup()
    renderPhotos('kantoor')

    await user.click(screen.getByRole('button', { name: /Terug naar Werk/ }))

    expect(screen.getByRole('list', { name: "Foto's" })).toHaveTextContent('Borrel kantoor')
  })
})
