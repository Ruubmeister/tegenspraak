package nl.rubium.tegenspraak.chat.boundary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import nl.rubium.tegenspraak.IntegrationTest;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;

class ChatControllerIT extends IntegrationTest {

    private static final String MESSAGES = "/api/cases/het-lek-bij-van-dijk/characters/sanne-bakker/messages";

    private PlayerPrincipal player;

    @BeforeEach
    void setUp() {
        player = registerPlayer();
    }

    @Test
    void sendMessage_givenCharacterReplies_expectReplyReturnedAndConversationStored() throws Exception {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(
                        new ChatResponse(List.of(new Generation(new AssistantMessage("Hoi, waarmee kan ik helpen?")))));

        mockMvc.perform(post(MESSAGES)
                        .with(user(player))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Hallo Sanne\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sender").value("CHARACTER"))
                .andExpect(jsonPath("$.content").value("Hoi, waarmee kan ik helpen?"));

        mockMvc.perform(get(MESSAGES).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sender").value("PLAYER"))
                .andExpect(jsonPath("$[0].content").value("Hallo Sanne"))
                .andExpect(jsonPath("$[1].sender").value("CHARACTER"));
    }

    @Test
    void getConversation_givenOtherPlayersConversation_expectEmpty() throws Exception {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Hoi")))));
        mockMvc.perform(post(MESSAGES)
                .with(user(player))
                .with(spaCsrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"Hallo\"}"));

        mockMvc.perform(get(MESSAGES).with(user(registerPlayer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void sendMessage_givenBlankContent_expectBadRequest() throws Exception {
        mockMvc.perform(post(MESSAGES)
                        .with(user(player))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendMessage_givenModelFailure_expectBadGateway() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("API down"));

        mockMvc.perform(post(MESSAGES)
                        .with(user(player))
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Hallo\"}"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void getConversation_givenUnknownCharacter_expectNotFound() throws Exception {
        mockMvc.perform(get("/api/cases/het-lek-bij-van-dijk/characters/onbekend/messages")
                        .with(user(player)))
                .andExpect(status().isNotFound());
    }
}
