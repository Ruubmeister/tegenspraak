package nl.rubium.tegenspraak.chat.boundary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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

class ConversationReadIT extends IntegrationTest {

    private static final String CASE_PATH = "/cases/het-lek-bij-van-dijk";

    private PlayerPrincipal player;
    private PlayerPrincipal teammate;
    private UUID teamId;

    @BeforeEach
    void setUp() {
        player = createPlayer();
        teammate = createPlayer();
        teamId = createTeam(player, teammate);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Hoi!")))));
    }

    @Test
    void getUnreadConversations_givenNoMessages_expectEmpty() throws Exception {
        mockMvc.perform(get(unreadPath()).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getUnreadConversations_givenCharacterReplied_expectConversationUnreadForWholeTeam() throws Exception {
        sendHallo(teammate);

        mockMvc.perform(get(unreadPath()).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("sanne-bakker"));
    }

    @Test
    void markRead_givenLatestMessage_expectReadOnlyForThatPlayer() throws Exception {
        sendHallo(player);

        markRead(player, latestMessageId()).andExpect(status().isNoContent());

        mockMvc.perform(get(unreadPath()).with(user(player)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get(unreadPath()).with(user(teammate)))
                .andExpect(jsonPath("$[0]").value("sanne-bakker"));
    }

    @Test
    void markRead_givenNewerReplyAfterwards_expectUnreadAgain() throws Exception {
        sendHallo(player);
        markRead(player, latestMessageId());

        sendHallo(teammate);

        mockMvc.perform(get(unreadPath()).with(user(player)))
                .andExpect(jsonPath("$[0]").value("sanne-bakker"));
    }

    @Test
    void markRead_givenOlderMessageAfterNewer_expectReadPositionKept() throws Exception {
        sendHallo(player);
        String firstReply = latestMessageId();
        sendHallo(player);
        markRead(player, latestMessageId());

        markRead(player, firstReply).andExpect(status().isNoContent());

        mockMvc.perform(get(unreadPath()).with(user(player)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void markRead_givenMessageOfOtherTeam_expectNotFound() throws Exception {
        PlayerPrincipal outsider = createPlayer();
        UUID otherTeamId = createTeam(outsider);
        mockMvc.perform(post(messagesPath(otherTeamId))
                .with(user(outsider))
                .with(spaCsrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"Hallo\"}"));
        String otherTeamMessage = JsonPath.read(
                mockMvc.perform(get(messagesPath(otherTeamId)).with(user(outsider)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$[-1].id");

        markRead(player, otherTeamMessage).andExpect(status().isNotFound());
    }

    @Test
    void getUnreadConversations_givenNonMember_expectNotFound() throws Exception {
        mockMvc.perform(get(unreadPath()).with(user(createPlayer()))).andExpect(status().isNotFound());
    }

    private ResultActions markRead(PlayerPrincipal reader, String messageId) throws Exception {
        return mockMvc.perform(put(messagesPath(teamId) + "/read")
                .with(user(reader))
                .with(spaCsrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"messageId\": \"" + messageId + "\"}"));
    }

    private String latestMessageId() throws Exception {
        String conversation = mockMvc.perform(get(messagesPath(teamId)).with(user(player)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(conversation, "$[-1].id");
    }

    private void sendHallo(PlayerPrincipal sender) throws Exception {
        mockMvc.perform(post(messagesPath(teamId))
                        .with(user(sender))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Hallo\"}"))
                .andExpect(status().isOk());
    }

    private String unreadPath() {
        return "/api/teams/" + teamId + CASE_PATH + "/unread-conversations";
    }

    private static String messagesPath(UUID teamId) {
        return "/api/teams/" + teamId + CASE_PATH + "/characters/sanne-bakker/messages";
    }
}
