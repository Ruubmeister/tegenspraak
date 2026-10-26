import type { DesktopApp } from '../api/types'

type AppType = DesktopApp['type']

/** One outline drawing per kind of app, on a 24 by 24 grid. */
const ICON_PATHS: Record<AppType, string> = {
  mail: 'M3 6h18v12H3z M3 6l9 7 9-7',
  files: 'M3 6a1 1 0 0 1 1-1h5l2 2h9a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1z',
  calendar: 'M4 6h16v14H4z M4 10h16 M8 3v5 M16 3v5 M8 14h2 M12 14h2 M16 14h0 M8 17h2 M12 17h2',
  photos: 'M3 5h18v14H3z M3 16l5-5 4 4 3-3 6 6 M15.5 9.5a1.5 1.5 0 1 0 0-.01',
  browser: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z M3 12h18 M12 3c3 3 3 15 0 18 M12 3c-3 3-3 15 0 18',
}

interface AppIconProps {
  type: AppType
  /** Smaller icons appear in the title bar and the taskbar. */
  size?: 'large' | 'small'
}

/** A colored tile with the drawing of an app, decorative next to the app title. */
export function AppIcon({ type, size = 'large' }: AppIconProps) {
  return (
    <span className={`app-icon app-icon-${type} app-icon-${size}`} aria-hidden="true">
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d={ICON_PATHS[type]} />
      </svg>
    </span>
  )
}
