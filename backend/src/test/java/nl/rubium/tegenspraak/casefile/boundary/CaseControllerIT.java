package nl.rubium.tegenspraak.casefile.boundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import nl.rubium.tegenspraak.IntegrationTest;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaseControllerIT extends IntegrationTest {

    private static final String SAMPLE_CASE = "/api/cases/het-lek-bij-van-dijk";

    private PlayerPrincipal player;

    @BeforeEach
    void setUp() {
        player = registerPlayer();
    }

    @Test
    void listCases_givenNoSession_expectUnauthorized() throws Exception {
        mockMvc.perform(get("/api/cases")).andExpect(status().isUnauthorized());
    }

    @Test
    void listCases_givenLoggedInPlayer_expectSampleCase() throws Exception {
        mockMvc.perform(get("/api/cases").with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("het-lek-bij-van-dijk"))
                .andExpect(jsonPath("$[0].title").value("Het lek bij Van Dijk Vastgoed"));
    }

    @Test
    void getCase_givenSampleCase_expectCharactersAndDesktopsWithoutSecrets() throws Exception {
        String body = mockMvc.perform(get(SAMPLE_CASE).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characters[0].id").value("sanne-bakker"))
                .andExpect(jsonPath("$.desktops.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("setting").doesNotContain("secrets").doesNotContain("Dennis");
    }

    @Test
    void getCase_givenUnknownCase_expectNotFound() throws Exception {
        mockMvc.perform(get("/api/cases/bestaat-niet").with(user(player))).andExpect(status().isNotFound());
    }

    @Test
    void getDesktop_givenPersonDesktop_expectAppsWithTypeDiscriminator() throws Exception {
        mockMvc.perform(get(SAMPLE_CASE + "/desktops/werkplek-sanne-bakker").with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerType").value("person"))
                .andExpect(jsonPath("$.apps[0].type").value("mail"))
                .andExpect(jsonPath("$.apps[0].messages[2].sentAt").value("2026-03-12T21:47:00"))
                .andExpect(jsonPath("$.apps[1].type").value("files"));
    }
}
