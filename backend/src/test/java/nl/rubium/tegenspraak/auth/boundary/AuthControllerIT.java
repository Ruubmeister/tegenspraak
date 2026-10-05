package nl.rubium.tegenspraak.auth.boundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import nl.rubium.tegenspraak.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class AuthControllerIT extends IntegrationTest {

    @Test
    void register_givenNewUsername_expectCreatedAndLoggedIn() throws Exception {
        String username = uniqueUsername();

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/register")
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "geheim-wachtwoord")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andReturn()
                .getRequest()
                .getSession();

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void register_givenTakenUsernameInOtherCase_expectConflict() throws Exception {
        String username = registerPlayer().getUsername();

        mockMvc.perform(post("/api/auth/register")
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username.toUpperCase(), "geheim-wachtwoord")))
                .andExpect(status().isConflict());
    }

    @Test
    void register_givenTooShortPassword_expectBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(uniqueUsername(), "kort")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_givenValidCredentials_expectSessionWithNewId() throws Exception {
        String username = registerPlayer().getUsername();
        MockHttpSession anonymousSession = new MockHttpSession();
        String anonymousSessionId = anonymousSession.getId();

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .session(anonymousSession)
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "geheim-wachtwoord")))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();

        assertThat(session.getId()).isNotEqualTo(anonymousSessionId);
    }

    @Test
    void login_givenCsrfCookieFromEarlierRequest_expectLoginAcceptedAndTokenRotated() throws Exception {
        String username = registerPlayer().getUsername();
        Cookie csrfCookie =
                mockMvc.perform(get("/api/auth/me")).andReturn().getResponse().getCookie("XSRF-TOKEN");

        Cookie[] cookies = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "geheim-wachtwoord")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();

        assertThat(cookies)
                .filteredOn(cookie -> cookie.getName().equals("XSRF-TOKEN"))
                .anySatisfy(cookie -> assertThat(cookie.getValue()).isNotBlank().isNotEqualTo(csrfCookie.getValue()));
    }

    @Test
    void login_givenWrongPassword_expectUnauthorized() throws Exception {
        String username = registerPlayer().getUsername();

        mockMvc.perform(post("/api/auth/login")
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "fout-wachtwoord")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_givenNoCsrfToken_expectForbidden() throws Exception {
        String username = registerPlayer().getUsername();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "geheim-wachtwoord")))
                .andExpect(status().isForbidden());
    }

    @Test
    void currentPlayer_givenNoSession_expectUnauthorizedWithCsrfCookie() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void logout_givenLoggedInPlayer_expectSessionEnded() throws Exception {
        String username = registerPlayer().getUsername();
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "geheim-wachtwoord")))
                .andReturn()
                .getRequest()
                .getSession();

        mockMvc.perform(post("/api/auth/logout").session(session).with(spaCsrf()))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();
    }

    private static String uniqueUsername() {
        return "speler-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String credentials(String username, String password) {
        return """
                {"username": "%s", "password": "%s"}
                """.formatted(username, password);
    }
}
