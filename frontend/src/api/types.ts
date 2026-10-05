// Mirrors the JSON returned by the backend.

export interface Player {
  username: string
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

export interface DesktopSummary {
  id: string
  ownerName: string
  ownerType: OwnerType
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
  body: string
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
  content: string
}

export interface FilesApp {
  type: 'files'
  id: string
  title: string
  files: FileEntry[]
}

/** Discriminated on "type". A new desktop app needs a member here and a case in DesktopAppView. */
export type DesktopApp = MailApp | FilesApp

export interface Desktop {
  id: string
  ownerName: string
  ownerType: OwnerType
  description: string
  apps: DesktopApp[]
}

export type MessageSender = 'PLAYER' | 'CHARACTER'

export interface ChatMessage {
  id: string
  sender: MessageSender
  content: string
  sentAt: string
}
