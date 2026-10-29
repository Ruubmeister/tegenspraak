import type { CharacterSummary } from '../../api/types'
import type { DesktopLocation } from '../desktopApi'

/** What every desktop app view gets besides its app. */
export interface AppViewProps {
  location: DesktopLocation
  /** Owner of the computer, shown with an attached item. */
  ownerName: string
  /** The contacts an opened item can be attached for. */
  contacts: CharacterSummary[]
  /** Item to select when the app opens. */
  initialItemId?: string
}
