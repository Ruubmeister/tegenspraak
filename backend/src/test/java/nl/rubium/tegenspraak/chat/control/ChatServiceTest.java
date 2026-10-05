package nl.rubium.tegenspraak.chat.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import nl.rubium.tegenspraak.casefile.control.CaseCatalog;
import nl.rubium.tegenspraak.casefile.control.CaseContentNotFoundException;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CaseManifest;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;
import nl.rubium.tegenspraak.chat.entity.ChatMessageRepository;
import nl.rubium.tegenspraak.chat.entity.MessageSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.domain.Limit;

class ChatServiceTest {

    private static final UUID PLAYER_ID = UUID.randomUUID();
    private static final String CASE_ID = "demo";
    private static final String CHARACTER_ID = "de-butler";

    private final CaseCatalog caseCatalog = mock(CaseCatalog.class);
    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ChatModel chatModel = mock(ChatModel.class);
    private final ChatService chatService =
            new ChatService(caseCatalog, new CharacterPromptBuilder(), chatMessageRepository, chatModel, 40);

    @BeforeEach
    void setUp() {
        CharacterProfile character =
                new CharacterProfile(CHARACTER_ID, "De Butler", "Butler", "Altijd aanwezig.", null, null, null, null);
        CaseDefinition caseDefinition = new CaseDefinition(
                CASE_ID, new CaseManifest("Demo", "Kort", "Intro", "Geheim"), List.of(character), List.of());
        when(caseCatalog.getCase(CASE_ID)).thenReturn(caseDefinition);
        when(caseCatalog.getCharacter(CASE_ID, CHARACTER_ID)).thenReturn(character);
    }

    @Test
    void sendMessage_givenPreviousConversation_expectSystemPromptHistoryAndNewMessageSentToModel() {
        givenStoredHistoryNewestFirst(
                message(MessageSender.CHARACTER, "Goedenavond."), message(MessageSender.PLAYER, "Hallo"));
        givenModelReplies("Ik was in de keuken.");

        chatService.sendMessage(PLAYER_ID, CASE_ID, CHARACTER_ID, "Waar was u?");

        List<Message> sent = capturePromptMessages();
        assertThat(sent)
                .extracting(Message::getMessageType)
                .containsExactly(MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT, MessageType.USER);
        assertThat(sent).extracting(Message::getText).endsWith("Hallo", "Goedenavond.", "Waar was u?");
    }

    @Test
    void sendMessage_givenModelReply_expectPlayerMessageAndReplyStored() {
        givenStoredHistoryNewestFirst();
        givenModelReplies("  Ik was in de keuken.  ");

        ChatMessage reply = chatService.sendMessage(PLAYER_ID, CASE_ID, CHARACTER_ID, "Waar was u?");

        assertThat(reply.getSender()).isEqualTo(MessageSender.CHARACTER);
        assertThat(reply.getContent()).isEqualTo("Ik was in de keuken.");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> saved = ArgumentCaptor.forClass(List.class);
        verify(chatMessageRepository).saveAll(saved.capture());
        assertThat(saved.getValue())
                .extracting(ChatMessage::getSender)
                .containsExactly(MessageSender.PLAYER, MessageSender.CHARACTER);
    }

    @Test
    void sendMessage_givenHistoryCutOffAtCharacterMessage_expectLeadingCharacterMessageDropped() {
        givenStoredHistoryNewestFirst(
                message(MessageSender.CHARACTER, "Tweede antwoord"),
                message(MessageSender.PLAYER, "Tweede vraag"),
                message(MessageSender.CHARACTER, "Eerste antwoord"));
        givenModelReplies("Ja.");

        chatService.sendMessage(PLAYER_ID, CASE_ID, CHARACTER_ID, "Derde vraag");

        assertThat(capturePromptMessages())
                .extracting(Message::getText)
                .doesNotContain("Eerste antwoord")
                .endsWith("Tweede vraag", "Tweede antwoord", "Derde vraag");
    }

    @Test
    void sendMessage_givenModelFails_expectCharacterUnavailableAndNothingStored() {
        givenStoredHistoryNewestFirst();
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("API down"));

        assertThatThrownBy(() -> chatService.sendMessage(PLAYER_ID, CASE_ID, CHARACTER_ID, "Hallo"))
                .isInstanceOf(CharacterUnavailableException.class);
        verify(chatMessageRepository, never()).saveAll(anyList());
    }

    @Test
    void sendMessage_givenBlankModelReply_expectCharacterUnavailableAndNothingStored() {
        givenStoredHistoryNewestFirst();
        givenModelReplies(" ");

        assertThatThrownBy(() -> chatService.sendMessage(PLAYER_ID, CASE_ID, CHARACTER_ID, "Hallo"))
                .isInstanceOf(CharacterUnavailableException.class);
        verify(chatMessageRepository, never()).saveAll(anyList());
    }

    @Test
    void sendMessage_givenUnknownCharacter_expectNotFoundAndModelNotCalled() {
        when(caseCatalog.getCharacter(CASE_ID, "onbekend")).thenThrow(new CaseContentNotFoundException("Unknown"));

        assertThatThrownBy(() -> chatService.sendMessage(PLAYER_ID, CASE_ID, "onbekend", "Hallo"))
                .isInstanceOf(CaseContentNotFoundException.class);
        verify(chatModel, never()).call(any(Prompt.class));
    }

    private void givenStoredHistoryNewestFirst(ChatMessage... messages) {
        when(chatMessageRepository.findByPlayerIdAndCaseIdAndCharacterIdOrderBySentAtDesc(
                        eq(PLAYER_ID), eq(CASE_ID), eq(CHARACTER_ID), any(Limit.class)))
                .thenReturn(List.of(messages));
    }

    private void givenModelReplies(String text) {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(text)))));
    }

    private List<Message> capturePromptMessages() {
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        return prompt.getValue().getInstructions();
    }

    private static ChatMessage message(MessageSender sender, String content) {
        return new ChatMessage(PLAYER_ID, CASE_ID, CHARACTER_ID, sender, content);
    }
}
