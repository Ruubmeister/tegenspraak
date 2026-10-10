package nl.rubium.tegenspraak.chat.boundary;

import java.time.Instant;
import java.util.List;
import nl.rubium.tegenspraak.chat.control.ConversationMessage;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;
import nl.rubium.tegenspraak.chat.entity.MessageSender;
import nl.rubium.tegenspraak.investigation.boundary.EvidenceResponse;

/** authorName is the team member who wrote a player message, null for character messages. */
public record ChatMessageResponse(
        String id,
        MessageSender sender,
        String authorName,
        String content,
        Instant sentAt,
        List<EvidenceResponse> attachments) {

    static ChatMessageResponse from(ConversationMessage conversationMessage) {
        return from(
                conversationMessage.message(),
                conversationMessage.attachments().stream()
                        .map(EvidenceResponse::from)
                        .toList());
    }

    /** For a character reply, which never has attachments. */
    static ChatMessageResponse from(ChatMessage message) {
        return from(message, List.of());
    }

    private static ChatMessageResponse from(ChatMessage message, List<EvidenceResponse> attachments) {
        return new ChatMessageResponse(
                message.getId().toString(),
                message.getSender(),
                message.getAuthorName(),
                message.getContent(),
                message.getSentAt(),
                attachments);
    }
}
