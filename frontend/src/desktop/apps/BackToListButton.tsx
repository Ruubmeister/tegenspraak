interface BackToListButtonProps {
  label: string
  onClick: () => void
}

/** Returns from an opened mail or file to its list. Only shown on small screens, where the two do not fit side by side. */
export function BackToListButton({ label, onClick }: BackToListButtonProps) {
  return (
    <button type="button" className="app-back" onClick={onClick}>
      <span aria-hidden="true">&larr;</span> Terug naar {label}
    </button>
  )
}
