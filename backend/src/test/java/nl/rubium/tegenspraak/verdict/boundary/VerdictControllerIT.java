package nl.rubium.tegenspraak.verdict.boundary;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import nl.rubium.tegenspraak.IntegrationTest;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class VerdictControllerIT extends IntegrationTest {

    private static final String CORRECT_ANSWERS =
            "{\"answers\": {\"wie\": \"dennis-de-vries\", \"hoe\": \"prive-mail\", \"opzet\": \"onvoorzichtig\"}}";
    private static final String ONE_WRONG_ANSWER =
            "{\"answers\": {\"wie\": \"sanne-bakker\", \"hoe\": \"prive-mail\", \"opzet\": \"onvoorzichtig\"}}";

    private PlayerPrincipal player;
    private UUID teamId;

    @BeforeEach
    void setUp() {
        player = createPlayer();
        teamId = createTeam(player);
    }

    @Test
    void getVerdict_givenNoAttempts_expectQuestionsWithoutAnswersOrEpilogue() throws Exception {
        mockMvc.perform(get(verdictPath(teamId)).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(3))
                .andExpect(jsonPath("$.questions[0].options[0].label").value("Pieter van Dijk"))
                .andExpect(jsonPath("$.questions[0].answer").doesNotExist())
                .andExpect(jsonPath("$.attempts").value(0))
                .andExpect(jsonPath("$.solved").value(false))
                .andExpect(jsonPath("$.lastScore").value(nullValue()))
                .andExpect(jsonPath("$.epilogue").value(nullValue()));
    }

    @Test
    void submitVerdict_givenOneWrongAnswer_expectScoreWithoutEpilogue() throws Exception {
        submit(ONE_WRONG_ANSWER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(false))
                .andExpect(jsonPath("$.attempts").value(1))
                .andExpect(jsonPath("$.lastScore.correctAnswers").value(2))
                .andExpect(jsonPath("$.lastScore.totalQuestions").value(3))
                .andExpect(jsonPath("$.lastScore.submittedBy").value(player.getUsername()))
                .andExpect(jsonPath("$.epilogue").value(nullValue()));
    }

    @Test
    void submitVerdict_givenCorrectAnswersAfterWrongAttempt_expectSolvedWithEpilogue() throws Exception {
        submit(ONE_WRONG_ANSWER);

        submit(CORRECT_ANSWERS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(true))
                .andExpect(jsonPath("$.attempts").value(2))
                .andExpect(jsonPath("$.epilogue").isNotEmpty());

        mockMvc.perform(get(solvedCasesPath(teamId)).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("het-lek-bij-van-dijk"));
    }

    @Test
    void submitVerdict_givenAlreadySolved_expectConflict() throws Exception {
        submit(CORRECT_ANSWERS);

        submit(CORRECT_ANSWERS).andExpect(status().isConflict());
    }

    @Test
    void submitVerdict_givenMissingAnswer_expectBadRequest() throws Exception {
        submit("{\"answers\": {\"wie\": \"dennis-de-vries\"}}").andExpect(status().isBadRequest());
    }

    @Test
    void submitVerdict_givenUnknownCase_expectNotFound() throws Exception {
        mockMvc.perform(post("/api/teams/" + teamId + "/cases/bestaat-niet/verdict")
                        .with(user(player))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORRECT_ANSWERS))
                .andExpect(status().isNotFound());
    }

    @Test
    void getVerdict_givenTeammateSolvedCase_expectSolvedForWholeTeam() throws Exception {
        PlayerPrincipal teammate = createPlayer();
        teamId = createTeam(player, teammate);

        submit(CORRECT_ANSWERS).andExpect(status().isOk());

        mockMvc.perform(get(verdictPath(teamId)).with(user(teammate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(true))
                .andExpect(jsonPath("$.lastScore.submittedBy").value(player.getUsername()));
        mockMvc.perform(get(solvedCasesPath(teamId)).with(user(teammate)))
                .andExpect(jsonPath("$[0]").value("het-lek-bij-van-dijk"));
    }

    @Test
    void getSolvedCaseIds_givenOtherTeamOfSamePlayerSolvedCase_expectEmpty() throws Exception {
        submit(CORRECT_ANSWERS);

        mockMvc.perform(get(solvedCasesPath(createTeam(player))).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getVerdict_givenNonMember_expectNotFound() throws Exception {
        mockMvc.perform(get(verdictPath(teamId)).with(user(createPlayer()))).andExpect(status().isNotFound());
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
