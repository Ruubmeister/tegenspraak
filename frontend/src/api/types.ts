// Mirrors the JSON returned by the backend.

export type PlayerRole = 'PLAYER' | 'ADMIN'

export interface Player {
  username: string
  role: PlayerRole
}

/** An account as shown in user management. Only the admin can fetch these. */
export interface UserAccount {
  id: string
  username: string
  role: PlayerRole
  createdAt: string
}

/** Only members receive a team, including the join code they can share. */
export interface Team {
  id: string
  name: string
  joinCode: string
  members: string[]
}

export interface CaseSummary {
  id: string
  title: string
  summary: string
}

export interface CharacterSummary {
  id: string
  name: string
  role: string
  description: string
}

export type OwnerType = 'person' | 'organisation'

/** Present while something is locked. The code itself never reaches the browser. */
export interface Lock {
  hint: string
}

export interface DesktopSummary {
  id: string
  ownerName: string
  ownerType: OwnerType
  lock: Lock | null
}

export interface CaseOverview {
  id: string
  title: string
  introduction: string
  characters: CharacterSummary[]
  desktops: DesktopSummary[]
}

export interface MailMessage {
  id: string
  folder: string
  from: string
  to: string
  subject: string
  sentAt: string
  /** Null while the mail is locked. */
  body: string | null
  lock: Lock | null
}

export interface MailApp {
  type: 'mail'
  id: string
  title: string
  messages: MailMessage[]
}

export interface FileEntry {
  id: string
  folder: string
  name: string
  /** Null while the file is locked. */
  content: string | null
  lock: Lock | null
}

export interface FilesApp {
  type: 'files'
  id: string
  title: string
  files: FileEntry[]
}

/** Discriminated on "type". A new desktop app needs a member here and a case in DesktopAppView. */
export type DesktopApp = MailApp | FilesApp

/** A locked desktop has no apps until the team enters its code. */
export interface Desktop {
  id: string
  ownerName: string
  ownerType: OwnerType
  description: string
  lock: Lock | null
  apps: DesktopApp[]
}

/** NOTICE messages are game events in the chat, such as a newly introduced contact. */
export type MessageSender = 'PLAYER' | 'CHARACTER' | 'NOTICE'

export type EvidenceKind = 'mail' | 'file'

/** An opened mail or file that can be attached to a chat message. Carries the title only, never the content. */
export interface EvidenceItem {
  desktopId: string
  appId: string
  itemId: string
  kind: EvidenceKind
  title: string
  ownerName: string
}

/** The conversation is shared by the team; authorName is the member who wrote a PLAYER message. */
export interface ChatMessage {
  id: string
  sender: MessageSender
  authorName: string | null
  content: string
  sentAt: string
  /** Mails and files the author attached; empty for character messages and notices. */
  attachments: EvidenceItem[]
}

export interface VerdictOption {
  id: string
  label: string
}

export interface VerdictQuestion {
  id: string
  question: string
  options: VerdictOption[]
}

export interface VerdictScore {
  correctAnswers: number
  totalQuestions: number
  submittedBy: string
}

/** The epilogue reveals what really happened, so the backend only sends it once the case is solved. */
export interface Verdict {
  questions: VerdictQuestion[]
  attempts: number
  solved: boolean
  lastScore: VerdictScore | null
  epilogue: string | null
}
