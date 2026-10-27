package nl.rubium.tegenspraak.chat.boundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nl.rubium.tegenspraak.IntegrationTest;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import nl.rubium.tegenspraak.casefile.entity.Mood;
import nl.rubium.tegenspraak.chat.control.ConsultationService;
import nl.rubium.tegenspraak.chat.entity.CharacterMood;
import nl.rubium.tegenspraak.chat.entity.CharacterMoodRepository;
import nl.rubium.tegenspraak.chat.entity.ConsultationRequest;
import nl.rubium.tegenspraak.chat.entity.ConsultationRequestRepository;
import nl.rubium.tegenspraak.chat.entity.ConsultationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/** Yara Haddou asks Hendrik Pauw for the code of his atelier pc, see kunstbrand-in-haarlem. */
class ConsultationIT extends IntegrationTest {

    private static final String CASE_ID = "kunstbrand-in-haarlem";
    private static final String ASKER_ID = "yara-haddou";

    @Autowired
    private CharacterMoodRepository characterMoodRepository;

    @Autowired
    private ConsultationRequestRepository consultationRequestRepository;

    @Autowired
    private ConsultationService consultationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PlayerPrincipal player;
    private UUID teamId;

    @BeforeEach
    void setUp() {
        player = createPlayer();
        teamId = createTeam(player);
        characterMoodRepository.save(CharacterMood.start(teamId, CASE_ID, ASKER_ID, Mood.TRUSTING));
    }

    @Test
    void sendMessage_givenYaraAsksHendrik_expectNoticeAndPendingRequestDueWithinDelayRange() throws Exception {
        givenYaraAsksHendrik();
        Instant before = Instant.now();

        sendMessage("Kun je Hendrik vragen hoe we op zijn pc komen?");

        mockMvc.perform(get(messagesPath()).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[2].sender").value("NOTICE"))
                .andExpect(jsonPath("$[2].content").value("Yara Haddou heeft Hendrik Pauw een bericht gestuurd."));
        ConsultationRequest request = singleRequest();
        assertThat(request.getConsultationId()).isEqualTo("code-atelier-pauw");
        assertThat(request.getQuestion()).isEqualTo("Hoe komen de rechercheurs op je pc?");
        assertThat(request.getStatus()).isEqualTo(ConsultationStatus.PENDING);
        assertThat(request.getDueAt())
                .isBetween(before.plus(Duration.ofMinutes(2)), Instant.now().plus(Duration.ofMinutes(5)));
    }

    @Test
    void answerDueConsultations_givenRequestDue_expectYaraPassesAnswerOnAndRequestAnswered() throws Exception {
        givenYaraAsksHendrik();
        sendMessage("Kun je Hendrik vragen hoe we op zijn pc komen?");
        makeRequestsDue();
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(reply("Beste Yara, de code is spieraam."))
                .thenReturn(reply("Hendrik heeft gereageerd: de code is spieraam."));

        consultationService.answerDueConsultations();

        mockMvc.perform(get(messagesPath()).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[3].sender").value("CHARACTER"))
                .andExpect(jsonPath("$[3].content").value("Hendrik heeft gereageerd: de code is spieraam."));
        assertThat(singleRequest().getStatus()).isEqualTo(ConsultationStatus.ANSWERED);
    }

    @Test
    void sendMessage_givenConsultationAnswered_expectAskToolNoLongerOffered() throws Exception {
        givenYaraAsksHendrik();
        sendMessage("Kun je Hendrik vragen hoe we op zijn pc komen?");
        makeRequestsDue();
        when(chatModel.call(any(Prompt.class))).thenReturn(reply("Het is spieraam."));
        consultationService.answerDueConsultations();
        when(chatModel.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            boolean askToolOffered = prompt.getOptions() instanceof ToolCallingChatOptions options
                    && options.getToolCallbacks().stream()
                            .anyMatch(tool -> tool.getToolDefinition().name().equals("ask_contact"));
            return reply(askToolOffered ? "aangeboden" : "niet aangeboden");
        });

        sendMessage("Nog een vraag");

        mockMvc.perform(get(messagesPath()).with(user(player)))
                .andExpect(jsonPath("$[5].content").value("niet aangeboden"));
    }

    /** The mocked model runs the ask_contact tool itself, like the real tool loop would, then replies. */
    private void givenYaraAsksHendrik() {
        when(chatModel.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            ((ToolCallingChatOptions) prompt.getOptions())
                    .getToolCallbacks().stream()
                            .filter(tool -> tool.getToolDefinition().name().equals("ask_contact"))
                            .findFirst()
                            .orElseThrow()
                            .call("{\"consultationId\": \"code-atelier-pauw\","
                                    + " \"question\": \"Hoe komen de rechercheurs op je pc?\"}");
            return reply("Ik stuur Hendrik een berichtje.");
        });
    }

    private void makeRequestsDue() {
        jdbcTemplate.update(
                "update consultation_request set due_at = now() - interval '1 minute' where team_id = ?", teamId);
    }

    private ConsultationRequest singleRequest() {
        List<ConsultationRequest> requests =
                consultationRequestRepository.findByTeamIdAndCaseIdAndAskerId(teamId, CASE_ID, ASKER_ID);
        assertThat(requests).hasSize(1);
        return requests.getFirst();
    }

    private void sendMessage(String content) throws Exception {
        mockMvc.perform(post(messagesPath())
                        .with(user(player))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + content + "\"}"))
                .andExpect(status().isOk());
    }

    private static ChatResponse reply(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private String messagesPath() {
        return "/api/teams/" + teamId + "/cases/" + CASE_ID + "/characters/" + ASKER_ID + "/messages";
    }
}
