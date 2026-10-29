import { Link } from 'react-router'
import type { EvidenceItem } from '../api/types'
import { evidencePagePath } from '../cases/casePaths'
import { evidenceKindLabel } from './evidenceApi'

interface AttachmentLinksProps {
  teamId: string
  caseId: string
  attachments: EvidenceItem[]
}

/** Attachments of a sent message; each opens the item on its computer. */
export function AttachmentLinks({ teamId, caseId, attachments }: AttachmentLinksProps) {
  if (attachments.length === 0) {
    return null
  }
  return (
    <ul className="attachment-list" aria-label="Bijlagen">
      {attachments.map((attachment) => (
        <li key={`${attachment.desktopId}/${attachment.appId}/${attachment.itemId}`}>
          <Link
            className="attachment-chip"
            to={evidencePagePath(teamId, caseId, attachment)}
            title={`${attachment.title} (${attachment.ownerName})`}
          >
            <span className="attachment-kind">{evidenceKindLabel(attachment.kind)}</span> {attachment.title}
          </Link>
        </li>
      ))}
    </ul>
  )
}

interface DraftAttachmentsProps {
  attachments: EvidenceItem[]
  onRemove: (attachment: EvidenceItem) => void
}

/** Attachments of the message being written, each with a button to remove it. */
export function DraftAttachments({ attachments, onRemove }: DraftAttachmentsProps) {
  if (attachments.length === 0) {
    return null
  }
  return (
    <ul className="attachment-list draft-attachments" aria-label="Bijlagen bij je bericht">
      {attachments.map((attachment) => (
        <li key={`${attachment.desktopId}/${attachment.appId}/${attachment.itemId}`} className="attachment-chip">
          <span className="attachment-kind">{evidenceKindLabel(attachment.kind)}</span> {attachment.title}
          <button
            type="button"
            className="attachment-remove"
            aria-label={`Verwijder bijlage ${attachment.title}`}
            onClick={() => onRemove(attachment)}
          >
            ×
          </button>
        </li>
      ))}
    </ul>
  )
}
