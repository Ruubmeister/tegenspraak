package nl.rubium.tegenspraak.chat.control;

import java.util.List;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;
import nl.rubium.tegenspraak.investigation.control.EvidenceItem;

/** A stored message with its attachments resolved to the mails and files of the case. */
public record ConversationMessage(ChatMessage message, List<EvidenceItem> attachments) {

    public ConversationMessage {
        attachments = List.copyOf(attachments);
    }
}
