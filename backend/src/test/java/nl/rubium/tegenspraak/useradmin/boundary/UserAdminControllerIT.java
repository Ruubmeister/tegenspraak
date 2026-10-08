package nl.rubium.tegenspraak.useradmin.boundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import nl.rubium.tegenspraak.IntegrationTest;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import nl.rubium.tegenspraak.team.entity.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class UserAdminControllerIT extends IntegrationTest {

    private static final String USERS = "/api/admin/users";

    @Autowired
    private TeamRepository teamRepository;

    private PlayerPrincipal admin;

    @BeforeEach
    void setUp() {
        admin = admin();
    }

    @Test
    void listUsers_givenPlayer_expectForbidden() throws Exception {
        mockMvc.perform(get(USERS).with(user(createPlayer()))).andExpect(status().isForbidden());
    }

    @Test
    void listUsers_givenNoSession_expectUnauthorized() throws Exception {
        mockMvc.perform(get(USERS)).andExpect(status().isUnauthorized());
    }

    @Test
    void listUsers_givenAdmin_expectAdminAndPlayersListed() throws Exception {
        PlayerPrincipal player = createPlayer();

        mockMvc.perform(get(USERS).with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == '%s')].role", admin.getUsername())
                        .value("ADMIN"))
                .andExpect(jsonPath("$[?(@.username == '%s')].role", player.getUsername())
                        .value("PLAYER"));
    }

    @Test
    void createUser_givenNewUsername_expectCreatedPlayerWhoCanLogIn() throws Exception {
        String username = uniqueUsername();

        mockMvc.perform(post(USERS)
                        .with(user(admin))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, PLAYER_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("PLAYER"));

        mockMvc.perform(post("/api/auth/login")
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, PLAYER_PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void createUser_givenPlayer_expectForbidden() throws Exception {
        mockMvc.perform(post(USERS)
                        .with(user(createPlayer()))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(uniqueUsername(), PLAYER_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_givenTakenUsernameInOtherCase_expectConflict() throws Exception {
        String username = createPlayer().getUsername();

        mockMvc.perform(post(USERS)
                        .with(user(admin))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username.toUpperCase(), PLAYER_PASSWORD)))
                .andExpect(status().isConflict());
    }

    @Test
    void createUser_givenTooShortPassword_expectBadRequest() throws Exception {
        mockMvc.perform(post(USERS)
                        .with(user(admin))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(uniqueUsername(), "kort")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_givenLoggedInPlayer_expectSessionEndedAndNewPasswordWorks() throws Exception {
        PlayerPrincipal player = createPlayer();
        MockHttpSession playerSession = logIn(player.getUsername(), PLAYER_PASSWORD);

        mockMvc.perform(put(USERS + "/" + player.getId() + "/password")
                        .with(user(admin))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\": \"nieuw-wachtwoord\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").session(playerSession)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(player.getUsername(), "nieuw-wachtwoord")))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_givenAdminAccount_expectConflict() throws Exception {
        mockMvc.perform(put(USERS + "/" + admin.getId() + "/password")
                        .with(user(admin))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\": \"nieuw-wachtwoord\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteUser_givenLoggedInPlayer_expectAccountGoneAndSessionEnded() throws Exception {
        PlayerPrincipal player = createPlayer();
        MockHttpSession playerSession = logIn(player.getUsername(), PLAYER_PASSWORD);

        mockMvc.perform(delete(USERS + "/" + player.getId()).with(user(admin)).with(spaCsrf()))
                .andExpect(status().isNoContent());

        assertThat(playerAccountRepository.findById(player.getId())).isEmpty();
        mockMvc.perform(get("/api/auth/me").session(playerSession)).andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUser_givenAdminAccount_expectConflict() throws Exception {
        mockMvc.perform(delete(USERS + "/" + admin.getId()).with(user(admin)).with(spaCsrf()))
                .andExpect(status().isConflict());

        assertThat(playerAccountRepository.findById(admin.getId())).isPresent();
    }

    @Test
    void deleteUser_givenUnknownId_expectNotFound() throws Exception {
        mockMvc.perform(delete(USERS + "/" + UUID.randomUUID())
                        .with(user(admin))
                        .with(spaCsrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_givenOnlyMemberOfTeam_expectTeamDeletedAndSharedTeamKept() throws Exception {
        PlayerPrincipal player = createPlayer();
        PlayerPrincipal teammate = createPlayer();
        UUID soloTeamId = createTeam(player);
        UUID sharedTeamId = createTeam(player, teammate);

        mockMvc.perform(delete(USERS + "/" + player.getId()).with(user(admin)).with(spaCsrf()))
                .andExpect(status().isNoContent());

        assertThat(teamRepository.findById(soloTeamId)).isEmpty();
        mockMvc.perform(get("/api/teams/" + sharedTeamId).with(user(teammate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(1));
    }
}
