package nl.rubium.tegenspraak.chat.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import nl.rubium.tegenspraak.casefile.control.CaseCatalog;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CaseFixtures;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.casefile.entity.ContactConsultation;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;
import nl.rubium.tegenspraak.chat.entity.ChatMessageRepository;
import nl.rubium.tegenspraak.chat.entity.ConsultationRequest;
import nl.rubium.tegenspraak.chat.entity.ConsultationRequestRepository;
import nl.rubium.tegenspraak.chat.entity.ConsultationStatus;
import nl.rubium.tegenspraak.chat.entity.MessageSender;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class ConsultationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-27T20:00:00Z");
    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final String CASE_ID = "demo";

    private static final ContactConsultation SAFE_CODE = new ContactConsultation(
            "kluiscode",
            "de-kok",
            "De code van de kluis",
            "De speler vraagt naar de kluis.",
            "De code is 1234.",
            null,
            null);
    private static final CharacterProfile COOK = new CharacterProfile(
            "de-kok", "De Kok", "Kok", "In de keuken.", "Opgewekt", null, null, null, false, null, null, null);
    private static final CharacterProfile BUTLER = new CharacterProfile(
            "de-butler",
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
    private static final CaseDefinition CASE_DEFINITION =
            new CaseDefinition(CASE_ID, CaseFixtures.manifestWithSetting("Geheim"), List.of(BUTLER, COOK), List.of());

    private final ConsultationRequestRepository consultationRequestRepository =
            mock(ConsultationRequestRepository.class);
    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final CaseCatalog caseCatalog = mock(CaseCatalog.class);
    private final ChatModel chatModel = mock(ChatModel.class);
    private final ConsultationRelay consultationRelay = mock(ConsultationRelay.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ConsultationService consultationService = new ConsultationService(
            consultationRequestRepository,
            chatMessageRepository,
            caseCatalog,
            new CharacterPromptBuilder(),
            new CharacterReplyModel(chatModel, ToolCallingManager.builder().build()),
            consultationRelay,
            new ConsultationSchedule(
                    Clock.fixed(NOW, ZoneOffset.UTC),
                    Duration.ofMinutes(2),
                    Duration.ofMinutes(5),
                    Duration.ofMinutes(1),
                    3),
            new TransactionTemplate(mock(PlatformTransactionManager.class)),
            eventPublisher);

    @BeforeEach
    void setUp() {
        when(caseCatalog.allCases()).thenReturn(List.of(CASE_DEFINITION));
    }

    @Test
    void answerDueConsultations_givenNothingDue_expectModelNotCalled() {
        givenDueRequests();

        consultationService.answerDueConsultations();

        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void answerDueConsultations_givenDueRequest_expectContactAnswersWithPersonaAndAnswerOnly() {
        givenDueRequests(request("kluiscode", 0));
        givenModelReplies("Hoi! Het is 1234.");

        consultationService.answerDueConsultations();

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        List<Message> messages = prompt.getValue().getInstructions();
        assertThat(messages).extracting(Message::getMessageType).containsExactly(MessageType.SYSTEM, MessageType.USER);
        assertThat(messages.getFirst().getText())
                .contains("Je speelt het personage De Kok", "De Butler (Butler) stuurt je een berichtje")
                .contains("De code is 1234.", "Opgewekt")
                .doesNotContain("Geheim");
        assertThat(messages.getLast().getText()).isEqualTo("<bericht>\nWeet jij de code?\n</bericht>");
    }

    @Test
    void answerDueConsultations_givenContactAnswers_expectAnswerRelayedByAsker() {
        ConsultationRequest request = request("kluiscode", 0);
        givenDueRequests(request);
        givenModelReplies("Hoi! Het is 1234.");

        consultationService.answerDueConsultations();

        verify(consultationRelay).relay(request, new ConsultationContact(SAFE_CODE, COOK), BUTLER, "Hoi! Het is 1234.");
    }

    @Test
    void answerDueConsultations_givenContactFailsFirstTime_expectRetryScheduledAfterRetryDelay() {
        ConsultationRequest request = request("kluiscode", 0);
        givenDueRequests(request);
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("API down"));

        consultationService.answerDueConsultations();

        assertThat(request.getFailedAttempts()).isEqualTo(1);
        assertThat(request.getDueAt()).isEqualTo(NOW.plus(Duration.ofMinutes(1)));
        verify(consultationRequestRepository).save(request);
        verify(consultationRelay, never()).relay(any(), any(), any(), anyString());
    }

    @Test
    void answerDueConsultations_givenRelayFails_expectRetryScheduled() {
        ConsultationRequest request = request("kluiscode", 1);
        givenDueRequests(request);
        givenModelReplies("Hoi! Het is 1234.");
        when(consultationRelay.relay(any(), any(), any(), anyString()))
                .thenThrow(new CharacterUnavailableException("Empty reply"));

        consultationService.answerDueConsultations();

        assertThat(request.getFailedAttempts()).isEqualTo(2);
        assertThat(request.getDueAt()).isEqualTo(NOW.plus(Duration.ofMinutes(2)));
    }

    @Test
    void answerDueConsultations_givenLastAttemptFails_expectRequestDeletedAndNoticeStored() {
        ConsultationRequest request = request("kluiscode", 2);
        givenDueRequests(request);
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("API down"));

        consultationService.answerDueConsultations();

        verify(consultationRequestRepository).delete(request);
        ArgumentCaptor<ChatMessage> notice = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(notice.capture());
        assertThat(notice.getValue())
                .extracting(ChatMessage::getSender, ChatMessage::getCharacterId, ChatMessage::getContent)
                .containsExactly(
                        MessageSender.NOTICE,
                        "de-butler",
                        "De Kok heeft niet gereageerd op het bericht van De Butler.");
        verify(eventPublisher).publishEvent(TeamEvent.chat(TEAM_ID, CASE_ID, "de-butler"));
    }

    @Test
    void answerDueConsultations_givenConsultationRemovedFromCase_expectRequestDeletedWithoutModelCall() {
        ConsultationRequest request = request("verdwenen", 0);
        givenDueRequests(request);

        consultationService.answerDueConsultations();

        verify(consultationRequestRepository).delete(request);
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void answerDueConsultations_givenFirstRequestBreaks_expectSecondStillAnswered() {
        ConsultationRequest broken = request("kluiscode", 0);
        ConsultationRequest healthy = request("kluiscode", 0);
        givenDueRequests(broken, healthy);
        givenModelReplies("Het is 1234.");
        when(consultationRelay.relay(eq(broken), any(), any(), anyString()))
                .thenThrow(new IllegalStateException("Database down"));

        consultationService.answerDueConsultations();

        verify(consultationRelay).relay(eq(healthy), any(), any(), anyString());
    }

    private void givenDueRequests(ConsultationRequest... requests) {
        when(consultationRequestRepository.findByStatusAndDueAtLessThanEqualOrderByDueAtAsc(
                        ConsultationStatus.PENDING, NOW))
                .thenReturn(List.of(requests));
    }

    private void givenModelReplies(String text) {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(text)))));
    }

    private static ConsultationRequest request(String consultationId, int failedAttempts) {
        ConsultationRequest request = new ConsultationRequest(
                TEAM_ID, CASE_ID, "de-butler", consultationId, "Weet jij de code?", NOW.minusSeconds(300), NOW);
        for (int attempt = 0; attempt < failedAttempts; attempt++) {
            request.retryAt(NOW);
        }
        return request;
    }
}
