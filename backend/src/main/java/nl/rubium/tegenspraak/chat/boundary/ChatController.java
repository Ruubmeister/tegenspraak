package nl.rubium.tegenspraak.chat.boundary;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import nl.rubium.tegenspraak.chat.control.ChatService;
import nl.rubium.tegenspraak.chat.control.Conversation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The conversation is shared by all members of the team. */
@RestController
@RequestMapping("/api/teams/{teamId}/cases/{caseId}/characters/{characterId}/messages")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public List<ChatMessageResponse> getConversation(
            @PathVariable UUID teamId,
            @PathVariable String caseId,
            @PathVariable String characterId,
            @AuthenticationPrincipal PlayerPrincipal player) {
        Conversation conversation = new Conversation(teamId, caseId, characterId);
        return chatService.getConversation(player.getId(), conversation).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @PostMapping
    public ChatMessageResponse sendMessage(
            @PathVariable UUID teamId,
            @PathVariable String caseId,
            @PathVariable String characterId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal PlayerPrincipal player) {
        Conversation conversation = new Conversation(teamId, caseId, characterId);
        return ChatMessageResponse.from(chatService.sendMessage(
                player.getId(), player.getUsername(), conversation, request.content(), request.attachmentRefs()));
    }
}
