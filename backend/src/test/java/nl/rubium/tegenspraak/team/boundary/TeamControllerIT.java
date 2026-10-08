package nl.rubium.tegenspraak.team.boundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import nl.rubium.tegenspraak.IntegrationTest;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import nl.rubium.tegenspraak.team.entity.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class TeamControllerIT extends IntegrationTest {

    private static final String TEAMS = "/api/teams";

    @Autowired
    private TeamRepository teamRepository;

    private PlayerPrincipal player;

    @BeforeEach
    void setUp() {
        player = createPlayer();
    }

    @Test
    void createTeam_givenName_expectTeamWithJoinCodeAndCreatorAsMember() throws Exception {
        mockMvc.perform(post(TEAMS)
                        .with(user(player))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"  De Speurneuzen  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("De Speurneuzen"))
                .andExpect(jsonPath("$.joinCode").value(matchesPattern("[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}")))
                .andExpect(jsonPath("$.members").value(contains(player.getUsername())));
    }

    @Test
    void createTeam_givenBlankName_expectBadRequest() throws Exception {
        mockMvc.perform(post(TEAMS)
                        .with(user(player))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void joinTeam_givenCodeInLowercaseWithDash_expectMemberAddedAndTeamListed() throws Exception {
        String joinCode = createTeamThroughApi(player).toLowerCase();
        PlayerPrincipal teammate = createPlayer();

        join(teammate, joinCode)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members").value(contains(player.getUsername(), teammate.getUsername())));

        mockMvc.perform(get(TEAMS).with(user(teammate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("De Speurneuzen"));
    }

    @Test
    void joinTeam_givenAlreadyMember_expectNoDuplicateMember() throws Exception {
        String joinCode = createTeamThroughApi(player);

        join(player, joinCode)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(1));
    }

    @Test
    void joinTeam_givenUnknownCode_expectNotFound() throws Exception {
        join(player, "AAAA-AAAA").andExpect(status().isNotFound());
    }

    @Test
    void getTeams_givenOtherPlayersTeam_expectNotListed() throws Exception {
        createTeam(createPlayer());

        mockMvc.perform(get(TEAMS).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getTeam_givenNonMember_expectNotFound() throws Exception {
        UUID teamId = createTeam(createPlayer());

        mockMvc.perform(get(TEAMS + "/" + teamId).with(user(player))).andExpect(status().isNotFound());
    }

    @Test
    void leaveTeam_givenOtherMembersRemain_expectTeamKeptWithoutPlayer() throws Exception {
        PlayerPrincipal teammate = createPlayer();
        UUID teamId = createTeam(player, teammate);

        mockMvc.perform(delete(TEAMS + "/" + teamId + "/members/me")
                        .with(user(player))
                        .with(spaCsrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(TEAMS + "/" + teamId).with(user(teammate)))
                .andExpect(jsonPath("$.members").value(contains(teammate.getUsername())));
        mockMvc.perform(get(TEAMS + "/" + teamId).with(user(player))).andExpect(status().isNotFound());
    }

    @Test
    void leaveTeam_givenLastMember_expectTeamDeleted() throws Exception {
        UUID teamId = createTeam(player);

        mockMvc.perform(delete(TEAMS + "/" + teamId + "/members/me")
                        .with(user(player))
                        .with(spaCsrf()))
                .andExpect(status().isNoContent());

        assertThat(teamRepository.findById(teamId)).isEmpty();
    }

    private String createTeamThroughApi(PlayerPrincipal creator) throws Exception {
        String body = mockMvc.perform(post(TEAMS)
                        .with(user(creator))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"De Speurneuzen\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.joinCode");
    }

    private ResultActions join(PlayerPrincipal joiner, String joinCode) throws Exception {
        return mockMvc.perform(post(TEAMS + "/join")
                .with(user(joiner))
                .with(spaCsrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"joinCode\": \"" + joinCode + "\"}"));
    }
}
