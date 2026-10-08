package nl.rubium.tegenspraak.chat.boundary;

import java.time.Instant;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;
import nl.rubium.tegenspraak.chat.entity.MessageSender;

/** authorName is the team member who wrote a player message, null for character messages. */
public record ChatMessageResponse(String id, MessageSender sender, String authorName, String content, Instant sentAt) {

    static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId().toString(),
                message.getSender(),
                message.getAuthorName(),
                message.getContent(),
                message.getSentAt());
    }
}
