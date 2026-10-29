package nl.rubium.tegenspraak.chat.control;

import java.util.UUID;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;

/** Identifies the shared conversation of a team with one character of a case. */
public record Conversation(UUID teamId, String caseId, String characterId) {

    public ChatMessage characterMessage(String content) {
        return ChatMessage.fromCharacter(teamId, caseId, characterId, content);
    }

    /** A game event shown in the conversation, such as an introduction; never part of the character's prompt. */
    public ChatMessage notice(String content) {
        return ChatMessage.notice(teamId, caseId, characterId, content);
    }
}
