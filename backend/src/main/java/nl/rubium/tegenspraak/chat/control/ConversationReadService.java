package nl.rubium.tegenspraak.chat.control;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import nl.rubium.tegenspraak.casefile.control.CaseContentNotFoundException;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;
import nl.rubium.tegenspraak.chat.entity.ChatMessageRepository;
import nl.rubium.tegenspraak.chat.entity.ConversationReadRepository;
import nl.rubium.tegenspraak.investigation.control.InvestigationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Which conversations have messages a player has not read yet. Every team member reads at their own pace. */
@Service
public class ConversationReadService {

    private final ConversationReadRepository conversationReadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final InvestigationService investigationService;

    public ConversationReadService(
            ConversationReadRepository conversationReadRepository,
            ChatMessageRepository chatMessageRepository,
            InvestigationService investigationService) {
        this.conversationReadRepository = conversationReadRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.investigationService = investigationService;
    }

    /** Only characters the team can reach; the others have no conversation yet. */
    public List<String> getUnreadCharacterIds(UUID teamId, UUID playerId, String caseId) {
        Set<String> availableIds =
                investigationService.getProgress(teamId, playerId, caseId).availableCharacters().stream()
                        .map(CharacterProfile::id)
                        .collect(Collectors.toSet());
        return conversationReadRepository.findUnreadCharacterIds(playerId, teamId, caseId).stream()
                .filter(availableIds::contains)
                .sorted()
                .toList();
    }

    /**
     * Marks the conversation as read up to the given message, which the player has seen. Taking the message's own time
     * rather than the current time keeps a reply that arrived meanwhile unread.
     */
    @Transactional
    public void markRead(UUID playerId, Conversation conversation, UUID messageId) {
        investigationService.requireAvailableCharacter(
                conversation.teamId(), playerId, conversation.caseId(), conversation.characterId());
        ChatMessage message = chatMessageRepository
                .findById(messageId)
                .filter(candidate -> isInConversation(candidate, conversation))
                .orElseThrow(
                        () -> new CaseContentNotFoundException("No message " + messageId + " in this conversation"));
        conversationReadRepository.markReadUntil(
                UUID.randomUUID(),
                playerId,
                conversation.teamId(),
                conversation.caseId(),
                conversation.characterId(),
                message.getSentAt());
    }

    private static boolean isInConversation(ChatMessage message, Conversation conversation) {
        return message.getTeamId().equals(conversation.teamId())
                && message.getCaseId().equals(conversation.caseId())
                && message.getCharacterId().equals(conversation.characterId());
    }
}
