import type { CharacterSummary } from '../../api/types'
import type { DesktopLocation } from '../desktopApi'

/** What every desktop app view gets besides its app. */
export interface AppViewProps {
  location: DesktopLocation
  /** Owner of the computer, shown with an attached mail or file. */
  ownerName: string
  /** The contacts an opened mail or file can be attached for. */
  contacts: CharacterSummary[]
  /** Item to select when the app opens. */
  initialItemId?: string
}
