package nl.rubium.tegenspraak.chat.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import nl.rubium.tegenspraak.casefile.control.CaseContentNotFoundException;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CaseFixtures;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.casefile.entity.ContactIntroduction;
import nl.rubium.tegenspraak.chat.entity.ChatAttachment;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;
import nl.rubium.tegenspraak.chat.entity.ChatMessageRepository;
import nl.rubium.tegenspraak.chat.entity.MessageSender;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.FileEntry;
import nl.rubium.tegenspraak.desktop.entity.FilesApp;
import nl.rubium.tegenspraak.desktop.entity.Lock;
import nl.rubium.tegenspraak.desktop.entity.MailApp;
import nl.rubium.tegenspraak.desktop.entity.MailMessage;
import nl.rubium.tegenspraak.desktop.entity.OwnerType;
import nl.rubium.tegenspraak.investigation.control.EvidenceItem;
import nl.rubium.tegenspraak.investigation.control.EvidenceRef;
import nl.rubium.tegenspraak.investigation.control.InvestigationService;
import nl.rubium.tegenspraak.investigation.control.TeamProgress;
import nl.rubium.tegenspraak.team.control.TeamNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.data.domain.Limit;

class ChatServiceTest {

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID PLAYER_ID = UUID.randomUUID();
    private static final String CASE_ID = "demo";
    private static final String CHARACTER_ID = "de-butler";
    private static final Conversation CONVERSATION = new Conversation(TEAM_ID, CASE_ID, CHARACTER_ID);

    private static final CharacterProfile GARDENER = new CharacterProfile(
            "de-tuinman", "De Tuinman", "Tuinman", "In de kas.", null, null, null, null, true, null);
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
            List.of(new ContactIntroduction("de-tuinman", "De speler vraagt naar de tuin.")));
    private static final Desktop BUTLER_LAPTOP = new Desktop(
            "laptop-butler",
            "De Butler",
            OwnerType.PERSON,
            "Laptop",
            null,
            List.of(
                    new MailApp(
                            "mail",
                            "Mail",
                            List.of(new MailMessage(
                                    "afspraak",
                                    "Inbox",
                                    "kok@landhuis.nl",
                                    "butler@landhuis.nl",
                                    "Diner",
                                    LocalDateTime.of(2026, 3, 11, 16, 2),
                                    "Het diner is om acht uur.",
                                    null))),
                    new FilesApp(
                            "bestanden",
                            "Bestanden",
                            List.of(
                                    new FileEntry("menu", "Documenten", "menu.txt", "Soep en wild.", null),
                                    new FileEntry(
                                            "dagboek",
                                            "Persoonlijk",
                                            "dagboek.txt",
                                            "Ik zag de tuinman.",
                                            new Lock("1234", "Verjaardag"))))));
    private static final EvidenceRef DINNER_MAIL = new EvidenceRef("laptop-butler", "mail", "afspraak");
    private static final EvidenceRef MENU_FILE = new EvidenceRef("laptop-butler", "bestanden", "menu");
    private static final EvidenceRef LOCKED_DIARY = new EvidenceRef("laptop-butler", "bestanden", "dagboek");
    private static final CaseDefinition CASE_DEFINITION = new CaseDefinition(
            CASE_ID, CaseFixtures.manifestWithSetting("Geheim"), List.of(BUTLER, GARDENER), List.of(BUTLER_LAPTOP));

    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ChatModel chatModel = mock(ChatModel.class);
    private final InvestigationService investigationService = mock(InvestigationService.class);
    private final ChatService chatService = new ChatService(
            new CharacterPromptBuilder(),
            chatMessageRepository,
            chatModel,
            ToolCallingManager.builder().build(),
            investigationService,
            new ConversationLocks(),
            40);
    private final Set<String> unlockedTargets = new HashSet<>();

    @BeforeEach
    void setUp() {
        when(investigationService.requireAvailableCharacter(TEAM_ID, PLAYER_ID, CASE_ID, CHARACTER_ID))
                .thenReturn(BUTLER);
        when(investigationService.getProgress(TEAM_ID, PLAYER_ID, CASE_ID))
                .thenAnswer(invocation -> new TeamProgress(CASE_DEFINITION, unlockedTargets));
        when(investigationService.introduceCharacters(eq(TEAM_ID), eq(CASE_ID), anyList()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(chatModel.getOptions())
                .thenReturn(ToolCallingChatOptions.builder()
                        .model("test-model")
                        .maxTokens(100)
                        .build());
    }

    @Test
    void sendMessage_givenPreviousConversation_expectSystemPromptHistoryAndNewMessageSentToModel() {
        givenStoredHistoryNewestFirst(characterMessage("Goedenavond."), playerMessage("bram", "Hallo"));
        givenModelReplies("Ik was in de keuken.");

        chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Waar was u?", List.of());

        List<Message> sent = capturePromptMessages();
        assertThat(sent)
                .extracting(Message::getMessageType)
                .containsExactly(MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT, MessageType.USER);
        assertThat(sent).extracting(Message::getText).endsWith("[bram]: Hallo", "Goedenavond.", "[anouk]: Waar was u?");
    }

    @Test
    void sendMessage_givenModelReply_expectPlayerMessageWithAuthorAndReplyStored() {
        givenStoredHistoryNewestFirst();
        givenModelReplies("  Ik was in de keuken.  ");

        ChatMessage reply = chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Waar was u?", List.of());

        assertThat(reply.getSender()).isEqualTo(MessageSender.CHARACTER);
        assertThat(reply.getContent()).isEqualTo("Ik was in de keuken.");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> saved = ArgumentCaptor.forClass(List.class);
        verify(chatMessageRepository).saveAll(saved.capture());
        assertThat(saved.getValue())
                .extracting(ChatMessage::getSender, ChatMessage::getAuthorName, ChatMessage::getTeamId)
                .containsExactly(
                        tuple(MessageSender.PLAYER, "anouk", TEAM_ID), tuple(MessageSender.CHARACTER, null, TEAM_ID));
    }

    @Test
    void sendMessage_givenThinkingGenerationBeforeText_expectTextUsedAsReply() {
        givenStoredHistoryNewestFirst();
        Generation omittedThinking = new Generation(new AssistantMessage(""));
        Generation text = new Generation(new AssistantMessage("Ik was in de keuken."));
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(omittedThinking, text)));

        ChatMessage reply = chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Waar was u?", List.of());

        assertThat(reply.getContent()).isEqualTo("Ik was in de keuken.");
    }

    @Test
    void sendMessage_givenHistoryCutOffAtCharacterMessage_expectLeadingCharacterMessageDropped() {
        givenStoredHistoryNewestFirst(
                characterMessage("Tweede antwoord"),
                playerMessage("anouk", "Tweede vraag"),
                characterMessage("Eerste antwoord"));
        givenModelReplies("Ja.");

        chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Derde vraag", List.of());

        assertThat(capturePromptMessages())
                .extracting(Message::getText)
                .doesNotContain("Eerste antwoord")
                .endsWith("[anouk]: Tweede vraag", "Tweede antwoord", "[anouk]: Derde vraag");
    }

    @Test
    void sendMessage_givenModelFails_expectCharacterUnavailableAndNothingStored() {
        givenStoredHistoryNewestFirst();
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("API down"));

        assertThatThrownBy(() -> chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Hallo", List.of()))
                .isInstanceOf(CharacterUnavailableException.class);
        verify(chatMessageRepository, never()).saveAll(anyList());
    }

    @Test
    void sendMessage_givenBlankModelReply_expectCharacterUnavailableAndNothingStored() {
        givenStoredHistoryNewestFirst();
        givenModelReplies(" ");

        assertThatThrownBy(() -> chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Hallo", List.of()))
                .isInstanceOf(CharacterUnavailableException.class);
        verify(chatMessageRepository, never()).saveAll(anyList());
    }

    @Test
    void sendMessage_givenUnknownCharacter_expectNotFoundAndModelNotCalled() {
        when(investigationService.requireAvailableCharacter(TEAM_ID, PLAYER_ID, CASE_ID, "onbekend"))
                .thenThrow(new CaseContentNotFoundException("Unknown"));

        assertThatThrownBy(() -> chatService.sendMessage(
                        PLAYER_ID, "anouk", new Conversation(TEAM_ID, CASE_ID, "onbekend"), "Hallo", List.of()))
                .isInstanceOf(CaseContentNotFoundException.class);
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void sendMessage_givenPlayerNotInTeam_expectTeamNotFoundAndModelNotCalled() {
        when(investigationService.requireAvailableCharacter(TEAM_ID, PLAYER_ID, CASE_ID, CHARACTER_ID))
                .thenThrow(new TeamNotFoundException("Not a member"));

        assertThatThrownBy(() -> chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Hallo", List.of()))
                .isInstanceOf(TeamNotFoundException.class);
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void getConversation_givenPlayerNotInTeam_expectTeamNotFound() {
        when(investigationService.requireAvailableCharacter(TEAM_ID, PLAYER_ID, CASE_ID, CHARACTER_ID))
                .thenThrow(new TeamNotFoundException("Not a member"));

        assertThatThrownBy(() -> chatService.getConversation(PLAYER_ID, CONVERSATION))
                .isInstanceOf(TeamNotFoundException.class);
    }

    @Test
    void sendMessage_givenIntroducibleContact_expectToolOfferedOnConfiguredOptions() {
        givenStoredHistoryNewestFirst();
        givenModelReplies("Goedenavond.");

        chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Hallo", List.of());

        ChatOptions options = capturePrompt().getOptions();
        assertThat(options).isInstanceOfSatisfying(ToolCallingChatOptions.class, toolOptions -> {
            assertThat(toolOptions.getModel()).isEqualTo("test-model");
            assertThat(toolOptions.getMaxTokens()).isEqualTo(100);
            assertThat(toolOptions.getToolCallbacks())
                    .extracting(callback -> callback.getToolDefinition().name())
                    .containsExactly("introduce_contact");
        });
    }

    @Test
    void sendMessage_givenContactAlreadyIntroduced_expectNoToolOffered() {
        unlockedTargets.add("character:de-tuinman");
        givenStoredHistoryNewestFirst();
        givenModelReplies("Goedenavond.");

        chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Hallo", List.of());

        ChatOptions options = capturePrompt().getOptions();
        boolean toolOffered = options instanceof ToolCallingChatOptions toolOptions
                && !toolOptions.getToolCallbacks().isEmpty();
        assertThat(toolOffered).isFalse();
    }

    @Test
    void sendMessage_givenModelIntroducesContact_expectContactUnlockedAndNoticeStored() {
        givenStoredHistoryNewestFirst();
        givenModelCallsToolThenReplies("{\"characterId\": \"de-tuinman\"}", "Vraag het De Tuinman maar.");

        chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Wie was er in de tuin?", List.of());

        verify(investigationService).introduceCharacters(TEAM_ID, CASE_ID, List.of(GARDENER));
        assertThat(savedMessages())
                .extracting(ChatMessage::getSender, ChatMessage::getContent)
                .containsExactly(
                        tuple(MessageSender.PLAYER, "Wie was er in de tuin?"),
                        tuple(MessageSender.CHARACTER, "Vraag het De Tuinman maar."),
                        tuple(MessageSender.NOTICE, "De Butler heeft je in contact gebracht met De Tuinman."));
    }

    @Test
    void sendMessage_givenToolCalledWithUnknownId_expectNothingIntroduced() {
        givenStoredHistoryNewestFirst();
        givenModelCallsToolThenReplies("{\"characterId\": \"de-kok\"}", "Ik ken geen kok.");

        chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Ken je de kok?", List.of());

        verify(investigationService).introduceCharacters(TEAM_ID, CASE_ID, List.of());
        assertThat(savedMessages()).extracting(ChatMessage::getSender).doesNotContain(MessageSender.NOTICE);
    }

    @Test
    void sendMessage_givenModelCallsTool_expectToolResultSentBackToModel() {
        givenStoredHistoryNewestFirst();
        givenModelCallsToolThenReplies("{\"characterId\": \"de-tuinman\"}", "Vraag het De Tuinman maar.");

        chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Wie was er in de tuin?", List.of());

        ArgumentCaptor<Prompt> prompts = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(2)).call(prompts.capture());
        assertThat(prompts.getAllValues().getLast().getInstructions())
                .extracting(Message::getMessageType)
                .endsWith(MessageType.ASSISTANT, MessageType.TOOL);
    }

    @Test
    void sendMessage_givenModelKeepsCallingTool_expectCharacterUnavailableAndNothingStored() {
        givenStoredHistoryNewestFirst();
        when(chatModel.call(any(Prompt.class))).thenReturn(toolCallResponse("{\"characterId\": \"de-tuinman\"}"));

        assertThatThrownBy(() ->
                        chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Wie was er in de tuin?", List.of()))
                .isInstanceOf(CharacterUnavailableException.class);
        verify(chatMessageRepository, never()).saveAll(anyList());
    }

    @Test
    void sendMessage_givenModelFailsAfterToolCall_expectNothingIntroducedOrStored() {
        givenStoredHistoryNewestFirst();
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(toolCallResponse("{\"characterId\": \"de-tuinman\"}"))
                .thenThrow(new IllegalStateException("API down"));

        assertThatThrownBy(() ->
                        chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Wie was er in de tuin?", List.of()))
                .isInstanceOf(CharacterUnavailableException.class);
        verify(investigationService, never()).introduceCharacters(any(), any(), anyList());
        verify(chatMessageRepository, never()).saveAll(anyList());
    }

    @Test
    void sendMessage_givenNoticeInHistory_expectNoticeNotSentToModel() {
        givenStoredHistoryNewestFirst(
                ChatMessage.notice(TEAM_ID, CASE_ID, CHARACTER_ID, "De Butler heeft je in contact gebracht."),
                characterMessage("Goedenavond."),
                playerMessage("anouk", "Hallo"));
        givenModelReplies("Ja?");

        chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Nog iets", List.of());

        assertThat(capturePromptMessages())
                .extracting(Message::getText)
                .noneMatch(text -> text.contains("in contact gebracht"));
    }

    private void givenModelCallsToolThenReplies(String toolInput, String reply) {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(toolCallResponse(toolInput))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(reply)))));
    }

    /** The chat model only reports the tool call, ChatService has to execute it. */
    private static ChatResponse toolCallResponse(String toolInput) {
        AssistantMessage toolCall = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "introduce_contact", toolInput)))
                .build();
        return new ChatResponse(List.of(new Generation(toolCall)));
    }

    private List<ChatMessage> savedMessages() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> saved = ArgumentCaptor.forClass(List.class);
        verify(chatMessageRepository).saveAll(saved.capture());
        return saved.getValue();
    }

    private Prompt capturePrompt() {
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        return prompt.getValue();
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

    private List<Message> capturePromptMessages() {
        return capturePrompt().getInstructions();
    }

    private static ChatMessage playerMessage(String authorName, String content) {
        return ChatMessage.fromPlayer(TEAM_ID, CASE_ID, CHARACTER_ID, authorName, content, List.of());
    }

    private static ChatMessage characterMessage(String content) {
        return ChatMessage.fromCharacter(TEAM_ID, CASE_ID, CHARACTER_ID, content);
    }

    @Test
    void sendMessage_givenOpenAttachments_expectContentInPromptAfterMessage() {
        givenStoredHistoryNewestFirst();
        givenModelReplies("Dat klopt.");

        chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Klopt dit?", List.of(DINNER_MAIL, MENU_FILE));

        assertThat(capturePromptMessages().getLast().getText())
                .startsWith("[anouk]: Klopt dit?")
                .contains("--- Bijlage 1: mail van de computer van De Butler ---")
                .contains("Van: kok@landhuis.nl", "Onderwerp: Diner", "Het diner is om acht uur.")
                .contains("--- Bijlage 2: bestand van de computer van De Butler ---")
                .contains("Naam: Documenten/menu.txt", "Soep en wild.");
    }

    @Test
    void sendMessage_givenDuplicateAttachments_expectReferencesStoredOnceWithPlayerMessage() {
        givenStoredHistoryNewestFirst();
        givenModelReplies("Dat klopt.");

        chatService.sendMessage(
                PLAYER_ID, "anouk", CONVERSATION, "Klopt dit?", List.of(DINNER_MAIL, MENU_FILE, DINNER_MAIL));

        assertThat(savedMessages().getFirst().getAttachments())
                .containsExactly(
                        new ChatAttachment("laptop-butler", "mail", "afspraak"),
                        new ChatAttachment("laptop-butler", "bestanden", "menu"));
    }

    @Test
    void sendMessage_givenLockedAttachment_expectNotFoundAndModelNotCalled() {
        givenStoredHistoryNewestFirst();

        assertThatThrownBy(() ->
                        chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Lees dit", List.of(LOCKED_DIARY)))
                .isInstanceOf(CaseContentNotFoundException.class);
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void sendMessage_givenUnlockedAttachment_expectContentInPrompt() {
        unlockedTargets.add("item:laptop-butler/bestanden/dagboek");
        givenStoredHistoryNewestFirst();
        givenModelReplies("Dat is privé.");

        chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Lees dit", List.of(LOCKED_DIARY));

        assertThat(capturePromptMessages().getLast().getText()).contains("Ik zag de tuinman.");
    }

    @Test
    void sendMessage_givenUnknownAttachment_expectNotFoundAndModelNotCalled() {
        givenStoredHistoryNewestFirst();
        EvidenceRef unknown = new EvidenceRef("laptop-butler", "mail", "bestaat-niet");

        assertThatThrownBy(() -> chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "Hallo", List.of(unknown)))
                .isInstanceOf(CaseContentNotFoundException.class);
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void sendMessage_givenFourAttachments_expectTooManyAttachmentsAndModelNotCalled() {
        EvidenceRef fourth = new EvidenceRef("laptop-butler", "mail", "nog-een");

        assertThatThrownBy(() -> chatService.sendMessage(
                        PLAYER_ID,
                        "anouk",
                        CONVERSATION,
                        "Hallo",
                        List.of(DINNER_MAIL, MENU_FILE, LOCKED_DIARY, fourth)))
                .isInstanceOf(TooManyAttachmentsException.class);
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void sendMessage_givenAttachmentInHistory_expectContentRepeatedInPrompt() {
        givenStoredHistoryNewestFirst(characterMessage("Ja."), playerMessageWithDinnerMail("bram", "Ken je deze?"));
        givenModelReplies("Zoals ik zei.");

        chatService.sendMessage(PLAYER_ID, "anouk", CONVERSATION, "En verder?", List.of());

        assertThat(capturePromptMessages().get(1).getText())
                .startsWith("[bram]: Ken je deze?")
                .contains("Het diner is om acht uur.");
    }

    @Test
    void getConversation_givenMessageWithAttachment_expectAttachmentResolved() {
        when(chatMessageRepository.findByTeamIdAndCaseIdAndCharacterIdOrderBySentAtAsc(TEAM_ID, CASE_ID, CHARACTER_ID))
                .thenReturn(List.of(playerMessageWithDinnerMail("bram", "Ken je deze?")));

        List<ConversationMessage> conversation = chatService.getConversation(PLAYER_ID, CONVERSATION);

        assertThat(conversation.getFirst().attachments())
                .extracting(EvidenceItem::title)
                .containsExactly("Diner");
    }

    private static ChatMessage playerMessageWithDinnerMail(String authorName, String content) {
        return ChatMessage.fromPlayer(
                TEAM_ID,
                CASE_ID,
                CHARACTER_ID,
                authorName,
                content,
                List.of(new ChatAttachment("laptop-butler", "mail", "afspraak")));
    }
}
