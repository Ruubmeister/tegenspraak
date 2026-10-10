package nl.rubium.tegenspraak.investigation.boundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import nl.rubium.tegenspraak.IntegrationTest;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class InvestigationControllerIT extends IntegrationTest {

    private static final String DIARY = "$.apps[1].files[?(@.id == 'dagboek')]";

    private PlayerPrincipal player;
    private UUID teamId;

    @BeforeEach
    void setUp() {
        player = createPlayer();
        teamId = createTeam(player);
    }

    @Test
    void getCase_givenNewTeam_expectVisibleCharactersOnlyAndNoSecrets() throws Exception {
        String body = mockMvc.perform(get(casePath()).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characters[*].id").value(contains("sanne-bakker")))
                .andExpect(jsonPath("$.desktops.length()").value(2))
                .andExpect(jsonPath("$.desktops[0].lock").value(nullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("setting").doesNotContain("secrets").doesNotContain("Dennis");
    }

    @Test
    void getCase_givenNonMember_expectNotFound() throws Exception {
        mockMvc.perform(get(casePath()).with(user(createPlayer()))).andExpect(status().isNotFound());
    }

    @Test
    void getCase_givenUnknownCase_expectNotFound() throws Exception {
        mockMvc.perform(get("/api/teams/" + teamId + "/cases/bestaat-niet").with(user(player)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEvidence_givenNewTeam_expectOpenMailsAndFilesWithoutLockedDiary() throws Exception {
        String body = mockMvc.perform(get(casePath() + "/evidence").with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.itemId == 'deadline-herinnering')].kind")
                        .value(contains("mail")))
                .andExpect(jsonPath("$[?(@.itemId == 'deadline-herinnering')].title")
                        .value(contains("Bod Oude Suikerfabriek")))
                .andExpect(jsonPath("$[?(@.itemId == 'notities')].kind").value(contains("file")))
                .andExpect(jsonPath("$[?(@.itemId == 'dagboek')]").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("4.250.000");
    }

    @Test
    void getEvidence_givenDiaryUnlocked_expectDiaryListed() throws Exception {
        unlockDiary(player, "1403").andExpect(status().isOk());

        mockMvc.perform(get(casePath() + "/evidence").with(user(player)))
                .andExpect(jsonPath("$[?(@.itemId == 'dagboek')].title").value(contains("dagboek.txt")));
    }

    @Test
    void getEvidence_givenNonMember_expectNotFound() throws Exception {
        mockMvc.perform(get(casePath() + "/evidence").with(user(createPlayer())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDesktop_givenPersonDesktop_expectAppsWithTypeDiscriminator() throws Exception {
        mockMvc.perform(get(sanneDesktopPath()).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerType").value("person"))
                .andExpect(jsonPath("$.apps[0].type").value("mail"))
                .andExpect(jsonPath("$.apps[0].messages[2].sentAt").value("2026-03-12T21:47:00"))
                .andExpect(jsonPath("$.apps[1].type").value("files"));
    }

    @Test
    void getDesktop_givenLockedDiary_expectNameAndHintWithoutContentOrCode() throws Exception {
        String body = mockMvc.perform(get(sanneDesktopPath()).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(DIARY + ".name").value(contains("dagboek.txt")))
                .andExpect(jsonPath(DIARY + ".content").value(contains(nullValue())))
                .andExpect(jsonPath(DIARY + ".lock.hint")
                        .value(contains("Wachtwoordhint: etentje bij de ouders van D. (ddmm)")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("1403").doesNotContain("Bod is binnen");
    }

    @Test
    void unlockItem_givenWrongCode_expectUnprocessableContent() throws Exception {
        unlockDiary(player, "1203").andExpect(status().isUnprocessableContent());
    }

    @Test
    void unlockItem_givenCorrectCode_expectContentVisibleForTeammate() throws Exception {
        PlayerPrincipal teammate = createPlayer();
        teamId = createTeam(player, teammate);

        unlockDiary(player, " 1403 ")
                .andExpect(status().isOk())
                .andExpect(jsonPath(DIARY + ".lock").value(contains(nullValue())));

        mockMvc.perform(get(sanneDesktopPath()).with(user(teammate)))
                .andExpect(jsonPath(DIARY + ".content").value(contains(containsString("Bod is binnen"))));
    }

    @Test
    void unlockItem_givenFiveWrongCodes_expectTooManyRequestsEvenForCorrectCode() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            unlockDiary(player, "000" + attempt).andExpect(status().isUnprocessableContent());
        }

        unlockDiary(player, "1403")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber());
    }

    @Test
    void unlockItem_givenWrongCodesInOtherTeam_expectNoCooldownForThisTeam() throws Exception {
        PlayerPrincipal otherPlayer = createPlayer();
        UUID otherTeamId = createTeam(otherPlayer);
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post(diaryUnlockPath(otherTeamId))
                    .with(user(otherPlayer))
                    .with(spaCsrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\": \"0000\"}"));
        }

        unlockDiary(player, "1403").andExpect(status().isOk());
    }

    @Test
    void unlockItem_givenUnknownItem_expectNotFound() throws Exception {
        mockMvc.perform(post(sanneDesktopPath() + "/apps/bestanden/items/bestaat-niet/unlock")
                        .with(user(player))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"1403\"}"))
                .andExpect(status().isNotFound());
    }

    private ResultActions unlockDiary(PlayerPrincipal unlocker, String code) throws Exception {
        return mockMvc.perform(post(diaryUnlockPath(teamId))
                .with(user(unlocker))
                .with(spaCsrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\": \"" + code + "\"}"));
    }

    private String casePath() {
        return "/api/teams/" + teamId + "/cases/het-lek-bij-van-dijk";
    }

    private String sanneDesktopPath() {
        return casePath() + "/desktops/werkplek-sanne-bakker";
    }

    private static String diaryUnlockPath(UUID teamId) {
        return "/api/teams/" + teamId
                + "/cases/het-lek-bij-van-dijk/desktops/werkplek-sanne-bakker/apps/bestanden/items/dagboek/unlock";
    }
}
