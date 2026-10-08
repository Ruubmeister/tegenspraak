package nl.rubium.tegenspraak.casefile.boundary;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import nl.rubium.tegenspraak.IntegrationTest;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaseControllerIT extends IntegrationTest {

    private PlayerPrincipal player;

    @BeforeEach
    void setUp() {
        player = createPlayer();
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
                .andExpect(jsonPath("$[0].title").value("Het lek bij Van Dijk Vastgoed (oefenzaak)"));
    }
}
