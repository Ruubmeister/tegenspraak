package nl.rubium.tegenspraak.chat.boundary;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import nl.rubium.tegenspraak.chat.control.ChatService;
import nl.rubium.tegenspraak.desktop.entity.ItemRef;
import org.jspecify.annotations.Nullable;

/** Attachments are optional; text is always required, so the character knows what the team asks. */
public record SendMessageRequest(
        @NotBlank @Size(max = 1000) String content,
        @Nullable @Size(max = ChatService.MAX_ATTACHMENTS) List<@Valid AttachmentRequest> attachments) {

    List<ItemRef> attachmentRefs() {
        return attachments == null
                ? List.of()
                : attachments.stream().map(AttachmentRequest::toRef).toList();
    }
}
