package nl.rubium.tegenspraak.chat.boundary;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import nl.rubium.tegenspraak.chat.control.Conversation;
import nl.rubium.tegenspraak.chat.control.ConversationReadService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Read state of the current player, so the case navigation can mark conversations with new messages. */
@RestController
@RequestMapping("/api/teams/{teamId}/cases/{caseId}")
public class ConversationReadController {

    private final ConversationReadService conversationReadService;

    public ConversationReadController(ConversationReadService conversationReadService) {
        this.conversationReadService = conversationReadService;
    }

    /** Ids of the characters whose conversation has messages the player has not read yet. */
    @GetMapping("/unread-conversations")
    public List<String> getUnreadConversations(
            @PathVariable UUID teamId, @PathVariable String caseId, @AuthenticationPrincipal PlayerPrincipal player) {
        return conversationReadService.getUnreadCharacterIds(teamId, player.getId(), caseId);
    }

    @PutMapping("/characters/{characterId}/messages/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @PathVariable UUID teamId,
            @PathVariable String caseId,
            @PathVariable String characterId,
            @Valid @RequestBody MarkReadRequest request,
            @AuthenticationPrincipal PlayerPrincipal player) {
        conversationReadService.markRead(
                player.getId(), new Conversation(teamId, caseId, characterId), request.messageId());
    }
}
