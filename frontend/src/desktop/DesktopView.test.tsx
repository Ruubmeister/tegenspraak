import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { Desktop } from '../api/types'
import { DesktopView } from './DesktopView'

const desktop: Desktop = {
  id: 'werkplek',
  ownerName: 'Sanne Bakker',
  ownerType: 'person',
  description: 'Werklaptop',
  apps: [
    {
      type: 'mail',
      id: 'mail',
      title: 'Mail',
      messages: [
        {
          id: 'inbox-1',
          folder: 'Inbox',
          from: 'pieter@example.nl',
          to: 'sanne@example.nl',
          subject: 'Bod',
          sentAt: '2026-03-11T16:02:00',
          body: 'We gaan voor 4.250.000.',
        },
        {
          id: 'sent-1',
          folder: 'Verzonden',
          from: 'sanne@example.nl',
          to: 'privé@example.nl',
          subject: 'FW: Bod',
          sentAt: '2026-03-12T21:47:00',
          body: 'even thuis afmaken',
        },
      ],
    },
    {
      type: 'files',
      id: 'bestanden',
      title: 'Bestanden',
      files: [{ id: 'notities', folder: 'Documenten', name: 'notities.txt', content: 'bod definitief maken' }],
    },
  ],
}

describe('DesktopView', () => {
  it('DesktopView_givenMailApp_expectOnlyFirstFolderListed', () => {
    render(<DesktopView desktop={desktop} />)

    expect(screen.getByRole('button', { name: /^Bod/ })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /FW: Bod/ })).not.toBeInTheDocument()
  })

  it('DesktopView_givenOtherFolderAndMessageSelected_expectMessageBodyShown', async () => {
    const user = userEvent.setup()
    render(<DesktopView desktop={desktop} />)

    await user.click(screen.getByRole('button', { name: 'Verzonden' }))
    await user.click(screen.getByRole('button', { name: /FW: Bod/ }))

    expect(screen.getByText('even thuis afmaken')).toBeInTheDocument()
    expect(screen.getByText('privé@example.nl')).toBeInTheDocument()
  })

  it('DesktopView_givenFilesAppOpened_expectFileContentShownOnSelect', async () => {
    const user = userEvent.setup()
    render(<DesktopView desktop={desktop} />)

    await user.click(screen.getByRole('button', { name: 'Bestanden' }))
    await user.click(screen.getByRole('button', { name: 'notities.txt' }))

    expect(screen.getByText('bod definitief maken')).toBeInTheDocument()
  })
})
