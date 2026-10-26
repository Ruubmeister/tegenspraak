package nl.rubium.tegenspraak.verdict.boundary;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import nl.rubium.tegenspraak.IntegrationTest;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class VerdictControllerIT extends IntegrationTest {

    private static final String ANSWERS = """
            {"answers": {
                "wie": "Dennis de Vries",
                "hoe": "Sanne stuurde het bod naar haar privé-mail en Dennis zag het op de laptop",
                "opzet": "Nee, ze was onvoorzichtig"
            }}
            """;
    private static final String ALL_CORRECT = grading(true, true, true);
    private static final String ONE_WRONG = grading(false, true, true);

    private PlayerPrincipal player;
    private UUID teamId;

    @BeforeEach
    void setUp() {
        player = createPlayer();
        teamId = createTeam(player);
    }

    @Test
    void getVerdict_givenNoAttempts_expectOpenQuestionsWithoutAnswersOrEpilogue() throws Exception {
        mockMvc.perform(get(verdictPath(teamId)).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(3))
                .andExpect(jsonPath("$.questions[0].question")
                        .value("Wie heeft het bedrag van het bod aan Kramer Bouw doorgegeven?"))
                .andExpect(jsonPath("$.questions[0].answer").doesNotExist())
                .andExpect(jsonPath("$.questions[0].options").doesNotExist())
                .andExpect(jsonPath("$.maxAttempts").value(5))
                .andExpect(jsonPath("$.attemptsLeft").value(5))
                .andExpect(jsonPath("$.solved").value(false))
                .andExpect(jsonPath("$.failed").value(false))
                .andExpect(jsonPath("$.attempts.length()").value(0))
                .andExpect(jsonPath("$.epilogue").value(nullValue()));
    }

    @Test
    void submitVerdict_givenOneAnswerGradedWrong_expectCountAndAnswersWithoutWhichIsWrong() throws Exception {
        givenGrading(ONE_WRONG);

        submit(ANSWERS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(false))
                .andExpect(jsonPath("$.attemptsLeft").value(4))
                .andExpect(jsonPath("$.attempts[0].number").value(1))
                .andExpect(jsonPath("$.attempts[0].correctAnswers").value(2))
                .andExpect(jsonPath("$.attempts[0].totalQuestions").value(3))
                .andExpect(jsonPath("$.attempts[0].submittedBy").value(player.getUsername()))
                .andExpect(jsonPath("$.attempts[0].answers.wie").value("Dennis de Vries"))
                .andExpect(jsonPath("$.attempts[0].correct").doesNotExist())
                .andExpect(jsonPath("$.epilogue").value(nullValue()));
    }

    @Test
    void submitVerdict_givenAllCorrectAfterWrongAttempt_expectSolvedWithEpilogue() throws Exception {
        givenGrading(ONE_WRONG);
        submit(ANSWERS);
        givenGrading(ALL_CORRECT);

        submit(ANSWERS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(true))
                .andExpect(jsonPath("$.attempts.length()").value(2))
                .andExpect(jsonPath("$.epilogue").isNotEmpty());

        mockMvc.perform(get(solvedCasesPath(teamId)).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("het-lek-bij-van-dijk"));
    }

    @Test
    void submitVerdict_givenFiveWrongAttempts_expectFailedWithEpilogueAndNoMoreAttempts() throws Exception {
        givenGrading(ONE_WRONG);
        for (int attempt = 0; attempt < 4; attempt++) {
            submit(ANSWERS).andExpect(status().isOk());
        }

        submit(ANSWERS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failed").value(true))
                .andExpect(jsonPath("$.solved").value(false))
                .andExpect(jsonPath("$.attemptsLeft").value(0))
                .andExpect(jsonPath("$.epilogue").isNotEmpty());
        submit(ANSWERS).andExpect(status().isConflict());
        mockMvc.perform(get(solvedCasesPath(teamId)).with(user(player)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void submitVerdict_givenGradingFails_expectBadGatewayAndNoAttemptUsed() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API down"));

        submit(ANSWERS).andExpect(status().isBadGateway());

        mockMvc.perform(get(verdictPath(teamId)).with(user(player)))
                .andExpect(jsonPath("$.attemptsLeft").value(5));
    }

    @Test
    void submitVerdict_givenAlreadySolved_expectConflict() throws Exception {
        givenGrading(ALL_CORRECT);
        submit(ANSWERS);

        submit(ANSWERS).andExpect(status().isConflict());
    }

    @Test
    void submitVerdict_givenMissingAnswer_expectBadRequest() throws Exception {
        submit("{\"answers\": {\"wie\": \"Dennis\"}}").andExpect(status().isBadRequest());
    }

    @Test
    void submitVerdict_givenBlankAnswer_expectBadRequest() throws Exception {
        submit("{\"answers\": {\"wie\": \"Dennis\", \"hoe\": \" \", \"opzet\": \"Nee\"}}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitVerdict_givenAnswerOverMaximumLength_expectBadRequest() throws Exception {
        String longAnswer = "a".repeat(SubmitVerdictRequest.MAX_ANSWER_LENGTH + 1);

        submit("{\"answers\": {\"wie\": \"" + longAnswer + "\", \"hoe\": \"x\", \"opzet\": \"Nee\"}}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitVerdict_givenUnknownCase_expectNotFound() throws Exception {
        mockMvc.perform(post("/api/teams/" + teamId + "/cases/bestaat-niet/verdict")
                        .with(user(player))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ANSWERS))
                .andExpect(status().isNotFound());
    }

    @Test
    void getVerdict_givenTeammateSolvedCase_expectSolvedForWholeTeam() throws Exception {
        PlayerPrincipal teammate = createPlayer();
        teamId = createTeam(player, teammate);
        givenGrading(ALL_CORRECT);

        submit(ANSWERS).andExpect(status().isOk());

        mockMvc.perform(get(verdictPath(teamId)).with(user(teammate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(true))
                .andExpect(jsonPath("$.attempts[0].submittedBy").value(player.getUsername()));
        mockMvc.perform(get(solvedCasesPath(teamId)).with(user(teammate)))
                .andExpect(jsonPath("$[0]").value("het-lek-bij-van-dijk"));
    }

    @Test
    void getSolvedCaseIds_givenOtherTeamOfSamePlayerSolvedCase_expectEmpty() throws Exception {
        givenGrading(ALL_CORRECT);
        submit(ANSWERS);

        mockMvc.perform(get(solvedCasesPath(createTeam(player))).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getVerdict_givenNonMember_expectNotFound() throws Exception {
        mockMvc.perform(get(verdictPath(teamId)).with(user(createPlayer()))).andExpect(status().isNotFound());
    }

    private void givenGrading(String json) {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));
    }

    private static String grading(boolean who, boolean how, boolean intent) {
        return """
                {"grades": [
                    {"questionId": "wie", "correct": %s},
                    {"questionId": "hoe", "correct": %s},
                    {"questionId": "opzet", "correct": %s}
                ]}
                """.formatted(who, how, intent);
    }

    private ResultActions submit(String body) throws Exception {
        return mockMvc.perform(post(verdictPath(teamId))
                .with(user(player))
                .with(spaCsrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String verdictPath(UUID teamId) {
        return "/api/teams/" + teamId + "/cases/het-lek-bij-van-dijk/verdict";
    }

    private static String solvedCasesPath(UUID teamId) {
        return "/api/teams/" + teamId + "/solved-cases";
    }
}
