import { describe, expect, it } from 'vitest'
import type { EvidenceItem } from '../api/types'
import { addAttachmentToDraft, clearSentChatDraft, readChatDraft, saveChatDraft } from './chatDrafts'

const conversation = { teamId: 'team-1', caseId: 'het-lek', characterId: 'sanne-bakker' }

function mail(itemId: string): EvidenceItem {
  return { desktopId: 'werkplek', appId: 'mail', itemId, kind: 'mail', title: itemId, ownerName: 'Sanne Bakker' }
}

describe('chatDrafts', () => {
  it('addAttachmentToDraft_givenSameMailTwice_expectAttachedOnce', () => {
    addAttachmentToDraft(conversation, mail('bod'))

    expect(addAttachmentToDraft(conversation, mail('bod'))).toBe('already-attached')
    expect(readChatDraft(conversation).attachments).toHaveLength(1)
  })

  it('addAttachmentToDraft_givenThreeAttached_expectFullAndNotAdded', () => {
    ;['a', 'b', 'c'].forEach((itemId) => addAttachmentToDraft(conversation, mail(itemId)))

    expect(addAttachmentToDraft(conversation, mail('d'))).toBe('full')
    expect(readChatDraft(conversation).attachments.map((attached) => attached.itemId)).toEqual(['a', 'b', 'c'])
  })

  it('addAttachmentToDraft_givenTypedText_expectTextKept', () => {
    saveChatDraft(conversation, { text: 'Wat is dit?', attachments: [] })

    addAttachmentToDraft(conversation, mail('bod'))

    expect(readChatDraft(conversation).text).toBe('Wat is dit?')
  })

  it('clearSentChatDraft_givenNewAttachmentAddedWhileSending_expectDraftKept', () => {
    saveChatDraft(conversation, { text: 'Wat is dit?', attachments: [mail('bod'), mail('fw')] })

    clearSentChatDraft(conversation, { text: 'Wat is dit?', attachments: [mail('bod')] })

    expect(readChatDraft(conversation).attachments).toHaveLength(2)
  })

  it('readChatDraft_givenCorruptStoredValue_expectEmptyDraft', () => {
    sessionStorage.setItem('tegenspraak.chat-draft:team-1:het-lek:sanne-bakker', 'geen json')

    expect(readChatDraft(conversation)).toEqual({ text: '', attachments: [] })
  })
})
