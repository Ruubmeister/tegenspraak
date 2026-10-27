package nl.rubium.tegenspraak.liveupdate.boundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
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
import org.springframework.test.web.servlet.MvcResult;

class TeamEventControllerIT extends IntegrationTest {

    private PlayerPrincipal player;
    private PlayerPrincipal teammate;
    private UUID teamId;

    @BeforeEach
    void setUp() {
        player = createPlayer();
        teammate = createPlayer();
        teamId = createTeam(player, teammate);
    }

    @Test
    void streamEvents_givenMember_expectOpenEventStreamWithoutProxyBuffering() throws Exception {
        mockMvc.perform(get(eventsPath(teamId)).with(user(player)).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andExpect(header().string("X-Accel-Buffering", "no"));
    }

    @Test
    void streamEvents_givenNonMember_expectNotFound() throws Exception {
        mockMvc.perform(get(eventsPath(teamId)).with(user(createPlayer())).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isNotFound());
    }

    @Test
    void streamEvents_givenTeammateGetsReply_expectChatEventPushed() throws Exception {
        MvcResult stream = openStream(player);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Hoi!")))));

        mockMvc.perform(post("/api/teams/" + teamId + "/cases/het-lek-bij-van-dijk/characters/sanne-bakker/messages")
                        .with(user(teammate))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Hallo\"}"))
                .andExpect(status().isOk());

        assertThat(stream.getResponse().getContentAsString())
                .contains("data:{\"type\":\"CHAT\",\"caseId\":\"het-lek-bij-van-dijk\","
                        + "\"characterId\":\"sanne-bakker\"}");
    }

    @Test
    void streamEvents_givenPlayerJoins_expectMembersEventPushedToTeamOnly() throws Exception {
        MvcResult stream = openStream(player);
        PlayerPrincipal outsider = createPlayer();
        UUID otherTeamId = createTeam(outsider);
        MvcResult otherStream = mockMvc.perform(
                        get(eventsPath(otherTeamId)).with(user(outsider)).accept(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        String joinCode = teamService.getTeam(teamId, player.getId()).team().getJoinCode();
        teamService.joinTeam(createPlayer().getId(), joinCode);

        assertThat(stream.getResponse().getContentAsString()).contains("\"type\":\"MEMBERS\"");
        assertThat(otherStream.getResponse().getContentAsString()).doesNotContain("MEMBERS");
    }

    private MvcResult openStream(PlayerPrincipal member) throws Exception {
        return mockMvc.perform(get(eventsPath(teamId)).with(user(member)).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();
    }

    private static String eventsPath(UUID teamId) {
        return "/api/teams/" + teamId + "/events";
    }
}
