import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { BrowserApp } from '../../api/types'
import { renderWithProviders } from '../../test/testUtils'
import type { DesktopLocation } from '../desktopApi'
import { BrowserAppView } from './BrowserAppView'

const location: DesktopLocation = { teamId: 'team-1', caseId: 'het-lek', desktopId: 'werkplek' }

const browser: BrowserApp = {
  type: 'browser',
  id: 'browser',
  title: 'Browser',
  pages: [
    {
      id: 'nieuws',
      url: 'https://www.nieuws.nl/suikerfabriek',
      title: 'Suikerfabriek verkocht',
      visitedAt: '2026-03-11T08:15:00',
      content: 'De Oude Suikerfabriek is verkocht.',
      lock: null,
    },
    {
      id: 'zoekopdracht',
      url: 'https://www.zoeken.nl/?q=gmail+doorsturen',
      title: 'gmail doorsturen - Zoeken',
      visitedAt: '2026-03-12T21:40:00',
      content: 'Zo stuur je mail door naar je privéadres.',
      lock: null,
    },
    {
      id: 'bank',
      url: 'https://mijn.bank.nl/overzicht',
      title: 'Mijn bank',
      visitedAt: '2026-03-12T22:05:00',
      content: null,
      lock: { hint: 'Pincode' },
    },
  ],
}

function renderBrowser(initialItemId?: string) {
  return renderWithProviders(
    <BrowserAppView
      app={browser}
      location={location}
      ownerName="Sanne Bakker"
      contacts={[]}
      initialItemId={initialItemId}
    />,
  )
}

function historyTitles(): string[] {
  return screen
    .getAllByRole('button')
    .map((button) => button.querySelector('.browser-history-title')?.textContent?.trim())
    .filter((title): title is string => title !== undefined)
}

describe('BrowserAppView', () => {
  it('BrowserAppView_givenPages_expectHistoryGroupedByDayNewestFirst', () => {
    renderBrowser()

    expect(screen.getByRole('textbox', { name: 'Adres' })).toHaveValue('browser://geschiedenis')
    expect(screen.getAllByRole('heading', { level: 4 }).map((heading) => heading.textContent)).toEqual([
      'donderdag 12 maart 2026',
      'woensdag 11 maart 2026',
    ])
    expect(historyTitles()).toEqual(['Mijn bank', 'gmail doorsturen - Zoeken', 'Suikerfabriek verkocht'])
  })

  it('BrowserAppView_givenSearch_expectOnlyMatchingTitlesAndAddresses', async () => {
    const user = userEvent.setup()
    renderBrowser()

    await user.type(screen.getByRole('searchbox', { name: 'Zoeken in geschiedenis' }), 'nieuws.nl')

    expect(historyTitles()).toEqual(['Suikerfabriek verkocht'])
  })

  it('BrowserAppView_givenSearchWithoutMatches_expectEmptyMessage', async () => {
    const user = userEvent.setup()
    renderBrowser()

    await user.type(screen.getByRole('searchbox', { name: 'Zoeken in geschiedenis' }), 'bestaat niet')

    expect(screen.getByText("Geen bezochte pagina's gevonden.")).toBeInTheDocument()
  })

  it('BrowserAppView_givenVisitOpened_expectAddressAndPageText', async () => {
    const user = userEvent.setup()
    renderBrowser()

    await user.click(screen.getByRole('button', { name: /gmail doorsturen/ }))

    expect(screen.getByRole('textbox', { name: 'Adres' })).toHaveValue('https://www.zoeken.nl/?q=gmail+doorsturen')
    expect(screen.getByText('Zo stuur je mail door naar je privéadres.')).toBeInTheDocument()
  })

  it('BrowserAppView_givenBackAndForward_expectPreviousAndNextLocation', async () => {
    const user = userEvent.setup()
    renderBrowser()
    await user.click(screen.getByRole('button', { name: /Suikerfabriek verkocht/ }))

    await user.click(screen.getByRole('button', { name: 'Terug' }))
    expect(screen.getByRole('heading', { name: 'Geschiedenis' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Terug' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Vooruit' }))
    expect(screen.getByText('De Oude Suikerfabriek is verkocht.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Vooruit' })).toBeDisabled()
  })

  it('BrowserAppView_givenLockedPageOpened_expectUnlockFormInsteadOfText', async () => {
    const user = userEvent.setup()
    renderBrowser()

    await user.click(screen.getByRole('button', { name: /Mijn bank/ }))

    const page = screen.getByRole('article')
    expect(within(page).getByText('Deze pagina is vergrendeld.')).toBeInTheDocument()
    expect(within(page).getByText('Pincode')).toBeInTheDocument()
  })

  it('BrowserAppView_givenInitialItem_expectPageOpenWithHistoryBehindIt', async () => {
    const user = userEvent.setup()
    renderBrowser('nieuws')

    expect(screen.getByText('De Oude Suikerfabriek is verkocht.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Terug' }))

    expect(screen.getByRole('heading', { name: 'Geschiedenis' })).toBeInTheDocument()
  })
})
