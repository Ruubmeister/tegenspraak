package nl.rubium.tegenspraak.chat.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.chat.entity.CharacterMood;
import nl.rubium.tegenspraak.chat.entity.CharacterMoodRepository;
import nl.rubium.tegenspraak.chat.entity.ChatAttachment;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;
import nl.rubium.tegenspraak.chat.entity.ChatMessageRepository;
import nl.rubium.tegenspraak.chat.entity.MessageSender;
import nl.rubium.tegenspraak.desktop.entity.ItemRef;
import nl.rubium.tegenspraak.investigation.control.EvidenceItem;
import nl.rubium.tegenspraak.investigation.control.TeamProgress;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

/**
 * What a character remembers of its conversation with a team when it writes a message: the latest messages, the
 * evidence it was shown and how it feels about the team. Used both for replies to the team and for answers the
 * character passes on from a contact.
 */
@Component
class ConversationMemory {

    private final ChatMessageRepository chatMessageRepository;
    private final CharacterMoodRepository characterMoodRepository;
    private final CharacterPromptBuilder characterPromptBuilder;
    private final int maxHistoryMessages;

    ConversationMemory(
            ChatMessageRepository chatMessageRepository,
            CharacterMoodRepository characterMoodRepository,
            CharacterPromptBuilder characterPromptBuilder,
            ChatProperties chatProperties) {
        this.chatMessageRepository = chatMessageRepository;
        this.characterMoodRepository = characterMoodRepository;
        this.characterPromptBuilder = characterPromptBuilder;
        this.maxHistoryMessages = chatProperties.maxHistoryMessages();
    }

    /** Latest messages in chronological order, starting with a player message as the LLM expects. */
    List<ChatMessage> recentHistory(Conversation conversation) {
        List<ChatMessage> history =
                new ArrayList<>(chatMessageRepository.findByTeamIdAndCaseIdAndCharacterIdOrderBySentAtDesc(
                        conversation.teamId(),
                        conversation.caseId(),
                        conversation.characterId(),
                        Limit.of(maxHistoryMessages)));
        Collections.reverse(history);
        history.removeIf(message -> message.getSender() == MessageSender.NOTICE);
        while (!history.isEmpty() && history.getFirst().getSender() != MessageSender.PLAYER) {
            history.removeFirst();
        }
        return history;
    }

    /** Items shown to the character earlier in this conversation or with the new message. */
    Set<ItemRef> shownEvidence(Conversation conversation, List<ChatAttachment> newAttachments) {
        List<ChatAttachment> earlierAttachments = chatMessageRepository.findAttachmentsInConversation(
                conversation.teamId(), conversation.caseId(), conversation.characterId());
        return Stream.concat(earlierAttachments.stream(), newAttachments.stream())
                .map(ChatAttachment::ref)
                .collect(Collectors.toSet());
    }

    /** The stored mood, or the character's starting mood while the team has not talked to it yet. */
    CharacterMood currentMood(Conversation conversation, CharacterProfile character) {
        return characterMoodRepository
                .findByTeamIdAndCaseIdAndCharacterId(
                        conversation.teamId(), conversation.caseId(), conversation.characterId())
                .orElseGet(() -> CharacterMood.start(
                        conversation.teamId(),
                        conversation.caseId(),
                        conversation.characterId(),
                        character.startingMood()));
    }

    /** The system prompt followed by the history; the caller adds the message the character responds to. */
    List<Message> prompt(String systemPrompt, List<ChatMessage> history, TeamProgress progress) {
        List<Message> prompt = new ArrayList<>();
        prompt.add(new SystemMessage(systemPrompt));
        history.forEach(message -> prompt.add(toPromptMessage(message, progress)));
        return prompt;
    }

    /** Player messages carry the author's name, so the character can tell the team members apart. */
    Message toPromptMessage(ChatMessage message, TeamProgress progress) {
        return switch (message.getSender()) {
            case PLAYER ->
                new UserMessage(characterPromptBuilder.formatPlayerMessage(
                        message.getAuthorName(), message.getContent(), resolveAttachments(message, progress)));
            case CHARACTER -> new AssistantMessage(message.getContent());
            case NOTICE -> throw new IllegalArgumentException("Notices are not part of the prompt");
        };
    }

    /** Unlocks are never undone, so stored attachments stay readable; anything else is left out. */
    static List<EvidenceItem> resolveAttachments(ChatMessage message, TeamProgress progress) {
        return message.getAttachments().stream()
                .map(attachment -> progress.findOpenEvidence(attachment.ref()))
                .flatMap(Optional::stream)
                .toList();
    }
}
