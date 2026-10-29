import type { EvidenceItem } from '../api/types'
import { MAX_ATTACHMENTS } from './chatDrafts'
import { evidenceKindLabel, isSameEvidence, useEvidence } from './evidenceApi'

interface AttachmentPickerProps {
  teamId: string
  caseId: string
  selected: EvidenceItem[]
  onToggle: (evidence: EvidenceItem) => void
  onClose: () => void
}

/** Lists the items the team has opened, grouped per computer, to attach to the message. */
export function AttachmentPicker({ teamId, caseId, selected, onToggle, onClose }: AttachmentPickerProps) {
  const { data: evidence, isPending, isError } = useEvidence(teamId, caseId)
  const isFull = selected.length >= MAX_ATTACHMENTS

  return (
    <div className="attachment-picker" role="group" aria-label="Bijlagen kiezen">
      <div className="attachment-picker-header">
        <span>
          Bijlagen ({selected.length}/{MAX_ATTACHMENTS})
        </span>
        <button type="button" className="link-button" onClick={onClose}>
          Sluiten
        </button>
      </div>
      {isPending && <p className="status">Laden...</p>}
      {isError && <p className="status error">De mails en bestanden konden niet worden geladen.</p>}
      {evidence?.length === 0 && <p className="status">Er zijn nog geen mails of bestanden om bij te voegen.</p>}
      {groupByOwner(evidence ?? []).map(([ownerName, items]) => (
        <fieldset key={ownerName}>
          <legend>{ownerName}</legend>
          {items.map((item) => {
            const isSelected = selected.some((attached) => isSameEvidence(attached, item))
            return (
              <label key={`${item.desktopId}/${item.appId}/${item.itemId}`}>
                <input
                  type="checkbox"
                  checked={isSelected}
                  disabled={!isSelected && isFull}
                  onChange={() => onToggle(item)}
                />
                <span className="attachment-kind">{evidenceKindLabel(item.kind)}</span> {item.title}
              </label>
            )
          })}
        </fieldset>
      ))}
    </div>
  )
}

/** Keeps the order of the case files: computers first to last, and their items in order. */
function groupByOwner(evidence: EvidenceItem[]): [string, EvidenceItem[]][] {
  const groups = new Map<string, EvidenceItem[]>()
  for (const item of evidence) {
    groups.set(item.ownerName, [...(groups.get(item.ownerName) ?? []), item])
  }
  return [...groups.entries()]
}
