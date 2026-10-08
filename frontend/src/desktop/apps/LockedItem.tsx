import type { Lock } from '../../api/types'
import { useUnlockItem, type DesktopLocation } from '../desktopApi'
import { UnlockForm } from '../UnlockForm'

interface LockedItemProps {
  location: DesktopLocation
  appId: string
  itemId: string
  lock: Lock
  subject: string
}

/** Takes the place of a mail body or file content until the team enters the code. */
export function LockedItem({ location, appId, itemId, lock, subject }: LockedItemProps) {
  const unlockItem = useUnlockItem(location)
  return (
    <UnlockForm
      lock={lock}
      subject={subject}
      isPending={unlockItem.isPending}
      error={unlockItem.error}
      onSubmit={(code) => unlockItem.mutate({ appId, itemId, code })}
    />
  )
}
