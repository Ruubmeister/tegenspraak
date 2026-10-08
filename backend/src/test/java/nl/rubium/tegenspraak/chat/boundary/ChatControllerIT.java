package nl.rubium.tegenspraak.chat.boundary;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.http.MediaType;

class ChatControllerIT extends IntegrationTest {

    private PlayerPrincipal player;
    private UUID teamId;
    private String messages;

    @BeforeEach
    void setUp() {
        player = createPlayer();
        teamId = createTeam(player);
        messages = messagesPath(teamId, "sanne-bakker");
    }

    @Test
    void sendMessage_givenCharacterReplies_expectReplyReturnedAndConversationStored() throws Exception {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(
                        new ChatResponse(List.of(new Generation(new AssistantMessage("Hoi, waarmee kan ik helpen?")))));

        mockMvc.perform(post(messages)
                        .with(user(player))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Hallo Sanne\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sender").value("CHARACTER"))
                .andExpect(jsonPath("$.content").value("Hoi, waarmee kan ik helpen?"));

        mockMvc.perform(get(messages).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sender").value("PLAYER"))
                .andExpect(jsonPath("$[0].content").value("Hallo Sanne"))
                .andExpect(jsonPath("$[0].authorName").value(player.getUsername()))
                .andExpect(jsonPath("$[1].sender").value("CHARACTER"));
    }

    @Test
    void getConversation_givenTeammateSentMessage_expectSharedConversationWithAuthor() throws Exception {
        PlayerPrincipal teammate = createPlayer();
        UUID sharedTeamId = createTeam(player, teammate);
        givenCharacterReplies("Hoi");

        sendHallo(player, messagesPath(sharedTeamId, "sanne-bakker"));

        mockMvc.perform(get(messagesPath(sharedTeamId, "sanne-bakker")).with(user(teammate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].authorName").value(player.getUsername()))
                .andExpect(jsonPath("$[1].authorName").value(nullValue()));
    }

    @Test
    void getConversation_givenOtherTeamsConversation_expectEmpty() throws Exception {
        givenCharacterReplies("Hoi");
        sendHallo(player, messages);

        mockMvc.perform(get(messagesPath(createTeam(player), "sanne-bakker")).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getConversation_givenNonMember_expectNotFound() throws Exception {
        mockMvc.perform(get(messages).with(user(createPlayer()))).andExpect(status().isNotFound());
    }

    @Test
    void sendMessage_givenNonMember_expectNotFoundAndModelNotCalled() throws Exception {
        mockMvc.perform(post(messages)
                        .with(user(createPlayer()))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Hallo\"}"))
                .andExpect(status().isNotFound());

        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void sendMessage_givenBlankContent_expectBadRequest() throws Exception {
        mockMvc.perform(post(messages)
                        .with(user(player))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendMessage_givenModelFailure_expectBadGateway() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("API down"));

        mockMvc.perform(post(messages)
                        .with(user(player))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Hallo\"}"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void getConversation_givenUnknownCharacter_expectNotFound() throws Exception {
        mockMvc.perform(get(messagesPath(teamId, "onbekend")).with(user(player)))
                .andExpect(status().isNotFound());
    }

    @Test
    void sendMessage_givenSanneIntroducesDennis_expectDennisAvailableForTeamWithNotice() throws Exception {
        mockMvc.perform(get(messagesPath(teamId, "dennis-de-vries")).with(user(player)))
                .andExpect(status().isNotFound());
        when(chatModel.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            ((ToolCallingChatOptions) prompt.getOptions())
                    .getToolCallbacks()
                    .getFirst()
                    .call("{\"characterId\": \"dennis-de-vries\"}");
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Hier is zijn nummer."))));
        });

        sendHallo(player, messages);

        mockMvc.perform(get("/api/teams/" + teamId + "/cases/het-lek-bij-van-dijk")
                        .with(user(player)))
                .andExpect(jsonPath("$.characters[*].id").value(containsInAnyOrder("sanne-bakker", "dennis-de-vries")));
        mockMvc.perform(get(messages).with(user(player)))
                .andExpect(jsonPath("$[2].sender").value("NOTICE"))
                .andExpect(jsonPath("$[2].content")
                        .value("Sanne Bakker heeft je in contact gebracht met Dennis de Vries."));
        mockMvc.perform(get(messagesPath(teamId, "dennis-de-vries")).with(user(player)))
                .andExpect(status().isOk());
    }

    private void givenCharacterReplies(String reply) {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(reply)))));
    }

    private void sendHallo(PlayerPrincipal sender, String path) throws Exception {
        mockMvc.perform(post(path)
                        .with(user(sender))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Hallo\"}"))
                .andExpect(status().isOk());
    }

    private static String messagesPath(UUID teamId, String characterId) {
        return "/api/teams/" + teamId + "/cases/het-lek-bij-van-dijk/characters/" + characterId + "/messages";
    }
}
