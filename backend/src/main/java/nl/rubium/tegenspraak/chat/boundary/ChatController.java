package nl.rubium.tegenspraak.chat.boundary;

import jakarta.validation.Valid;
import java.util.List;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import nl.rubium.tegenspraak.chat.control.ChatService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cases/{caseId}/characters/{characterId}/messages")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public List<ChatMessageResponse> getConversation(
            @PathVariable String caseId,
            @PathVariable String characterId,
            @AuthenticationPrincipal PlayerPrincipal player) {
        return chatService.getConversation(player.getId(), caseId, characterId).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @PostMapping
    public ChatMessageResponse sendMessage(
            @PathVariable String caseId,
            @PathVariable String characterId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal PlayerPrincipal player) {
        return ChatMessageResponse.from(
                chatService.sendMessage(player.getId(), caseId, characterId, request.content()));
    }
}
