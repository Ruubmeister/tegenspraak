import { useEffect, useState } from 'react'

const timeFormat = new Intl.DateTimeFormat('nl-NL', { hour: '2-digit', minute: '2-digit' })
const dateFormat = new Intl.DateTimeFormat('nl-NL', { day: 'numeric', month: 'short', year: 'numeric' })

/** Checking every few seconds keeps the minute within a few seconds of the real one. */
const CLOCK_REFRESH_MS = 5_000

/** The current time and date at the end of the taskbar, as on a real computer. */
export function TaskbarClock() {
  const [now, setNow] = useState(() => new Date())

  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), CLOCK_REFRESH_MS)
    return () => window.clearInterval(timer)
  }, [])

  return (
    <time className="taskbar-clock" dateTime={now.toISOString()}>
      <span>{timeFormat.format(now)}</span>
      <span>{dateFormat.format(now)}</span>
    </time>
  )
}
