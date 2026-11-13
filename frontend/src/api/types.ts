// The JSON of the backend, under the names the frontend uses. The shapes are generated from the backend's OpenAPI
// spec into schema.ts; run "npm run generate:api" after the backend's OpenApiSpecIT updated openapi.json.

import type { components } from './schema'

type Schemas = components['schemas']

export type Player = Schemas['PlayerResponse']

export type PlayerRole = Player['role']

/** An account as shown in user management. Only the admin can fetch these. */
export type UserAccount = Schemas['UserResponse']

/** Only members receive a team, including the join code they can share. */
export type Team = Schemas['TeamResponse']

export type CaseSummary = Schemas['CaseSummaryResponse']

/** From easiest to hardest. */
export type CaseDifficulty = CaseSummary['difficulty']

export type CharacterSummary = Schemas['CharacterSummary']

/** Present while something is locked. The code itself never reaches the browser. */
export type Lock = Schemas['LockResponse']

export type DesktopSummary = Schemas['DesktopSummary']

export type OwnerType = DesktopSummary['ownerType']

export type CaseOverview = Schemas['CaseOverviewResponse']

/** The body is null while the mail is locked. */
export type MailMessage = Schemas['MailResponse']

export type MailApp = Schemas['MailAppResponse']

/** The content is null while the file is locked. */
export type FileEntry = Schemas['FileResponse']

export type FilesApp = Schemas['FilesAppResponse']

/** Location, attendees and notes are hidden while the event is locked. */
export type CalendarEvent = Schemas['EventResponse']

export type CalendarApp = Schemas['CalendarAppResponse']

/** The image itself is loaded from its own endpoint, which refuses it while the photo is locked. */
export type Photo = Schemas['PhotoResponse']

export type PhotosApp = Schemas['PhotosAppResponse']

/** A visit in the browsing history; the content is null while the page is locked. */
export type BrowserPage = Schemas['PageResponse']

export type BrowserApp = Schemas['BrowserAppResponse']

/** A locked desktop has no apps until the team enters its code. */
export type Desktop = Schemas['DesktopResponse']

/** Discriminated on "type". A new desktop app needs a case in DesktopAppView and an icon. */
export type DesktopApp = Desktop['apps'][number]

/** An opened desktop item that can be attached to a chat message. Carries the title only, never the content. */
export type EvidenceItem = Schemas['EvidenceResponse']

export type EvidenceKind = EvidenceItem['kind']

/** The conversation is shared by the team; authorName is the member who wrote a PLAYER message. */
export type ChatMessage = Schemas['ChatMessageResponse']

/** NOTICE messages are game events in the chat, such as a newly introduced contact. */
export type MessageSender = ChatMessage['sender']

export type VerdictQuestion = Schemas['QuestionResponse']

/** Only the number of correct answers is shown, not which answers were correct. */
export type VerdictAttempt = Schemas['AttemptResponse']

/** The epilogue reveals what really happened, so the backend only sends it once the case is solved or failed. */
export type Verdict = Schemas['VerdictResponse']

/**
 * Pushed by the backend over server-sent events when something changed for the team. It only says where to look; the
 * data itself is loaded through the regular endpoints. Not part of the OpenAPI spec, as EventSource is not a REST call.
 */
export type TeamEvent =
  | { type: 'CHAT'; caseId: string; characterId: string }
  | { type: 'CASE'; caseId: string; characterId: null }
  | { type: 'VERDICT'; caseId: string; characterId: null }
  | { type: 'MEMBERS'; caseId: null; characterId: null }
