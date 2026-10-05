package nl.rubium.tegenspraak.chat.boundary;

import java.time.Instant;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;
import nl.rubium.tegenspraak.chat.entity.MessageSender;

public record ChatMessageResponse(String id, MessageSender sender, String content, Instant sentAt) {

    static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId().toString(), message.getSender(), message.getContent(), message.getSentAt());
    }
}
