package nl.rubium.tegenspraak.chat.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CaseFixtures;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.casefile.entity.ConsultationUnlock;
import nl.rubium.tegenspraak.casefile.entity.ContactConsultation;
import nl.rubium.tegenspraak.chat.entity.CharacterMoodRepository;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;
import nl.rubium.tegenspraak.chat.entity.ChatMessageRepository;
import nl.rubium.tegenspraak.chat.entity.ConsultationRequest;
import nl.rubium.tegenspraak.chat.entity.ConsultationRequestRepository;
import nl.rubium.tegenspraak.chat.entity.ConsultationStatus;
import nl.rubium.tegenspraak.chat.entity.MessageSender;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.FileEntry;
import nl.rubium.tegenspraak.desktop.entity.FilesApp;
import nl.rubium.tegenspraak.desktop.entity.Lock;
import nl.rubium.tegenspraak.desktop.entity.OwnerType;
import nl.rubium.tegenspraak.investigation.control.InvestigationService;
import nl.rubium.tegenspraak.investigation.control.TeamProgress;
import nl.rubium.tegenspraak.investigation.control.UnlockTarget;
import nl.rubium.tegenspraak.liveupdate.entity.TeamEvent;
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
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class ConsultationRelayTest {

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final String CASE_ID = "demo";
    private static final String CHARACTER_ID = "de-butler";
    private static final Instant NOW = Instant.parse("2026-10-27T20:00:00Z");

    private static final CharacterProfile COOK = new CharacterProfile(
            "de-kok", "De Kok", "Kok", "In de keuken.", null, null, null, null, false, null, null, null);
    private static final ContactConsultation SAFE_CODE = new ContactConsultation(
            "kluiscode",
            "de-kok",
            "De code van het dagboek",
            "De speler vraagt naar het dagboek.",
            "De code is 1234.",
            null,
            List.of(new ConsultationUnlock(null, "laptop-butler", "bestanden", "dagboek")));
    private static final CharacterProfile BUTLER = new CharacterProfile(
            CHARACTER_ID,
            "De Butler",
            "Butler",
            "Altijd aanwezig.",
            null,
            null,
            null,
            null,
            false,
            null,
            null,
            List.of(SAFE_CODE));
    private static final Desktop BUTLER_LAPTOP = new Desktop(
            "laptop-butler",
            "De Butler",
            OwnerType.PERSON,
            "Laptop",
            null,
            List.of(new FilesApp(
                    "bestanden",
                    "Bestanden",
                    List.of(new FileEntry(
                            "dagboek",
                            "Persoonlijk",
                            "dagboek.txt",
                            "Ik zag de tuinman.",
                            new Lock("1234", "Verjaardag"))))));
    private static final CaseDefinition CASE_DEFINITION = new CaseDefinition(
            CASE_ID, CaseFixtures.manifestWithSetting("Geheim"), List.of(BUTLER, COOK), List.of(BUTLER_LAPTOP));

    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final CharacterMoodRepository characterMoodRepository = mock(CharacterMoodRepository.class);
    private final ConsultationRequestRepository consultationRequestRepository =
            mock(ConsultationRequestRepository.class);
    private final ChatModel chatModel = mock(ChatModel.class);
    private final InvestigationService investigationService = mock(InvestigationService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final CharacterPromptBuilder characterPromptBuilder = new CharacterPromptBuilder();
    private final ConsultationRelay consultationRelay = new ConsultationRelay(
            characterPromptBuilder,
            new CharacterReplyModel(chatModel, ToolCallingManager.builder().build()),
            new ConversationMemory(
                    chatMessageRepository,
                    characterMoodRepository,
                    characterPromptBuilder,
                    new ChatProperties(40, "claude-haiku-5-5")),
            new ConversationLocks(),
            chatMessageRepository,
            consultationRequestRepository,
            investigationService,
            new TransactionTemplate(mock(PlatformTransactionManager.class)),
            eventPublisher);
    private final Set<String> unlockedTargets = new HashSet<>();

    @BeforeEach
    void setUp() {
        when(investigationService.getTeamProgress(TEAM_ID, CASE_ID))
                .thenAnswer(invocation -> new TeamProgress(CASE_DEFINITION, unlockedTargets));
        when(characterMoodRepository.findByTeamIdAndCaseIdAndCharacterId(TEAM_ID, CASE_ID, CHARACTER_ID))
                .thenReturn(Optional.empty());
    }

    @Test
    void relay_givenAnswer_expectAnswerSentAsLastMessageAndReplyStored() {
        givenStoredHistoryNewestFirst(
                ChatMessage.fromCharacter(TEAM_ID, CASE_ID, CHARACTER_ID, "Ik vraag het de kok."),
                ChatMessage.fromPlayer(TEAM_ID, CASE_ID, CHARACTER_ID, "anouk", "Code?", List.of()));
        givenModelReplies("De kok zegt dat het 1234 is.");
        ConsultationRequest request = pendingRequest();

        consultationRelay.relay(request, new ConsultationContact(SAFE_CODE, COOK), BUTLER, "Het is 1234.");

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        List<Message> sent = prompt.getValue().getInstructions();
        assertThat(sent)
                .extracting(Message::getMessageType)
                .containsExactly(MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT, MessageType.USER);
        assertThat(sent.getLast().getText())
                .contains("Je hebt antwoord gekregen van De Kok", "Weet jij de code?", "Het is 1234.");
        assertThat(savedMessages())
                .extracting(ChatMessage::getSender, ChatMessage::getContent)
                .startsWith(tuple(MessageSender.CHARACTER, "De kok zegt dat het 1234 is."));
        assertThat(request.getStatus()).isEqualTo(ConsultationStatus.ANSWERED);
        verify(consultationRequestRepository).save(request);
    }

    @Test
    void relay_givenLockedItemUnlock_expectItemUnlockedAndNoticeStored() {
        givenStoredHistoryNewestFirst();
        givenModelReplies("De kok zegt dat het 1234 is.");
        when(investigationService.unlockWithoutCode(
                        TEAM_ID, CASE_ID, UnlockTarget.item("laptop-butler", "bestanden", "dagboek")))
                .thenReturn(true);

        consultationRelay.relay(pendingRequest(), new ConsultationContact(SAFE_CODE, COOK), BUTLER, "Het is 1234.");

        assertThat(savedMessages())
                .extracting(ChatMessage::getSender, ChatMessage::getContent)
                .containsExactly(
                        tuple(MessageSender.CHARACTER, "De kok zegt dat het 1234 is."),
                        tuple(MessageSender.NOTICE, "\"dagboek.txt\" op de computer van De Butler is ontgrendeld."));
        verify(eventPublisher).publishEvent(TeamEvent.chat(TEAM_ID, CASE_ID, CHARACTER_ID));
        verify(eventPublisher).publishEvent(TeamEvent.caseProgress(TEAM_ID, CASE_ID));
    }

    @Test
    void relay_givenAlreadyUnlocked_expectNoUnlockNoticeAndNoCaseEvent() {
        givenStoredHistoryNewestFirst();
        givenModelReplies("De kok zegt dat het 1234 is.");

        consultationRelay.relay(pendingRequest(), new ConsultationContact(SAFE_CODE, COOK), BUTLER, "Het is 1234.");

        assertThat(savedMessages()).extracting(ChatMessage::getSender).containsExactly(MessageSender.CHARACTER);
        verify(eventPublisher, never()).publishEvent(TeamEvent.caseProgress(TEAM_ID, CASE_ID));
    }

    @Test
    void relay_givenModelFails_expectNothingStoredAndRequestStillPending() {
        givenStoredHistoryNewestFirst();
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("API down"));
        ConsultationRequest request = pendingRequest();

        assertThatThrownBy(() -> consultationRelay.relay(
                        request, new ConsultationContact(SAFE_CODE, COOK), BUTLER, "Het is 1234."))
                .isInstanceOf(CharacterUnavailableException.class);
        verify(chatMessageRepository, never()).saveAll(anyList());
        verify(investigationService, never()).unlockWithoutCode(any(), any(), any());
        assertThat(request.getStatus()).isEqualTo(ConsultationStatus.PENDING);
    }

    private static ConsultationRequest pendingRequest() {
        return new ConsultationRequest(
                TEAM_ID, CASE_ID, CHARACTER_ID, "kluiscode", "Weet jij de code?", NOW, NOW.plus(Duration.ofMinutes(2)));
    }

    private void givenStoredHistoryNewestFirst(ChatMessage... messages) {
        when(chatMessageRepository.findByTeamIdAndCaseIdAndCharacterIdOrderBySentAtDesc(
                        eq(TEAM_ID), eq(CASE_ID), eq(CHARACTER_ID), any(Limit.class)))
                .thenReturn(List.of(messages));
    }

    private void givenModelReplies(String text) {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(text)))));
    }

    private List<ChatMessage> savedMessages() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> saved = ArgumentCaptor.forClass(List.class);
        verify(chatMessageRepository).saveAll(saved.capture());
        return saved.getValue();
    }
}
