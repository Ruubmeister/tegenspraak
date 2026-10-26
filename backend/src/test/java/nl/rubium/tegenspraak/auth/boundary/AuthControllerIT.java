package nl.rubium.tegenspraak.auth.boundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import nl.rubium.tegenspraak.IntegrationTest;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class AuthControllerIT extends IntegrationTest {

    @Test
    void register_givenAnonymousRequest_expectUnauthorizedBecauseSelfRegistrationIsDisabled() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(uniqueUsername(), PLAYER_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_givenValidCredentials_expectSessionWithNewIdAndRole() throws Exception {
        String username = createPlayer().getUsername();
        MockHttpSession anonymousSession = new MockHttpSession();
        String anonymousSessionId = anonymousSession.getId();

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .session(anonymousSession)
                        .with(sessionCookie(anonymousSessionId))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, PLAYER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("PLAYER"))
                .andReturn()
                .getRequest()
                .getSession();

        assertThat(session.getId()).isNotEqualTo(anonymousSessionId);
    }

    @Test
    void login_givenCsrfCookieFromEarlierRequest_expectLoginAcceptedAndTokenRotated() throws Exception {
        String username = createPlayer().getUsername();
        Cookie csrfCookie =
                mockMvc.perform(get("/api/auth/me")).andReturn().getResponse().getCookie("XSRF-TOKEN");

        Cookie[] cookies = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, PLAYER_PASSWORD)))
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
        String username = createPlayer().getUsername();

        mockMvc.perform(post("/api/auth/login")
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "fout-wachtwoord")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_givenNoCsrfToken_expectForbidden() throws Exception {
        String username = createPlayer().getUsername();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, PLAYER_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void currentPlayer_givenNoSession_expectUnauthorizedWithCsrfCookie() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void currentPlayer_givenAdminSession_expectAdminRole() throws Exception {
        MockHttpSession session = logIn(admin().getUsername(), ADMIN_PASSWORD);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void changePassword_givenCorrectCurrentPassword_expectNewPasswordWorks() throws Exception {
        PlayerPrincipal player = createPlayer();
        MockHttpSession session = logIn(player.getUsername(), PLAYER_PASSWORD);

        mockMvc.perform(put("/api/auth/password")
                        .session(session)
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordChange(PLAYER_PASSWORD, "nieuw-wachtwoord")))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(player.getUsername(), "nieuw-wachtwoord")))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_givenWrongCurrentPassword_expectUnprocessableContent() throws Exception {
        MockHttpSession session = logIn(createPlayer().getUsername(), PLAYER_PASSWORD);

        mockMvc.perform(put("/api/auth/password")
                        .session(session)
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordChange("fout-wachtwoord", "nieuw-wachtwoord")))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void changePassword_givenTooShortNewPassword_expectBadRequest() throws Exception {
        MockHttpSession session = logIn(createPlayer().getUsername(), PLAYER_PASSWORD);

        mockMvc.perform(put("/api/auth/password")
                        .session(session)
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordChange(PLAYER_PASSWORD, "kort")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_givenLoggedInPlayer_expectSessionEnded() throws Exception {
        MockHttpSession session = logIn(createPlayer().getUsername(), PLAYER_PASSWORD);

        mockMvc.perform(post("/api/auth/logout").session(session).with(spaCsrf()))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();
    }

    /**
     * Mirrors a browser sending its JSESSIONID cookie. Spring Security only changes the session id on login when the
     * requested session id is valid, and MockHttpServletRequest no longer treats a missing one as valid.
     */
    private static RequestPostProcessor sessionCookie(String sessionId) {
        return request -> {
            request.setRequestedSessionId(sessionId);
            request.setRequestedSessionIdFromCookie(true);
            return request;
        };
    }

    private static String passwordChange(String currentPassword, String newPassword) {
        return """
                {"currentPassword": "%s", "newPassword": "%s"}
                """.formatted(currentPassword, newPassword);
    }
}
