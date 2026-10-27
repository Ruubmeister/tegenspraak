package nl.rubium.tegenspraak;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import nl.rubium.tegenspraak.auth.control.PlayerAccountService;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import nl.rubium.tegenspraak.auth.entity.PlayerAccountRepository;
import nl.rubium.tegenspraak.auth.entity.PlayerRole;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.chat.control.MessageTone;
import nl.rubium.tegenspraak.chat.control.MessageToneClassifier;
import nl.rubium.tegenspraak.team.control.TeamService;
import nl.rubium.tegenspraak.team.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Full application against a Postgres Testcontainer. The LLM is mocked so tests never call a real API. */
@SpringBootTest(
        properties = {
            "spring.ai.anthropic.api-key=test-key",
            "tegenspraak.admin.initial-password=" + IntegrationTest.ADMIN_PASSWORD,
            // Tests answer due consultations themselves, so the scheduler must not get there first
            "tegenspraak.consultations.poll-interval=1h",
            // Test players have no registered session, so a heartbeat would close their event streams
            "tegenspraak.live-updates.heartbeat-interval=1h"
        })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    protected static final String PLAYER_PASSWORD = "geheim-wachtwoord";
    protected static final String ADMIN_PASSWORD = "admin-wachtwoord";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected PlayerAccountService playerAccountService;

    @Autowired
    protected PlayerAccountRepository playerAccountRepository;

    @Autowired
    protected TeamService teamService;

    @MockitoBean
    protected ChatModel chatModel;

    /** Mocked separately, so tests of the character's replies only see calls for those replies. */
    @MockitoBean
    protected MessageToneClassifier messageToneClassifier;

    /** The real model has configured options; the character tools are added to a copy of them. */
    @BeforeEach
    void stubChatModelOptions() {
        when(chatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        when(messageToneClassifier.classify(any(CharacterProfile.class), any(), any(String.class), anyInt()))
                .thenReturn(MessageTone.NEUTRAL);
    }

    /** Mirrors the SPA: the XSRF-TOKEN cookie value is echoed back in the X-XSRF-TOKEN header. */
    protected static RequestPostProcessor spaCsrf() {
        return request -> {
            String token = UUID.randomUUID().toString();
            request.setCookies(new Cookie("XSRF-TOKEN", token));
            request.addHeader("X-XSRF-TOKEN", token);
            return request;
        };
    }

    protected static String uniqueUsername() {
        return "speler-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected static String credentials(String username, String password) {
        return """
                {"username": "%s", "password": "%s"}
                """.formatted(username, password);
    }

    /** Creates a player with a unique name, so tests do not depend on each other's data. */
    protected PlayerPrincipal createPlayer() {
        return PlayerPrincipal.from(playerAccountService.createPlayer(uniqueUsername(), PLAYER_PASSWORD));
    }

    /** Creates a team with the given players as members and returns its id. */
    protected UUID createTeam(PlayerPrincipal creator, PlayerPrincipal... otherMembers) {
        Team team = teamService
                .createTeam(creator.getId(), "Team " + creator.getUsername())
                .team();
        for (PlayerPrincipal member : otherMembers) {
            teamService.joinTeam(member.getId(), team.getJoinCode());
        }
        return team.getId();
    }

    /** The admin account created at startup by AdminAccountInitializer. */
    protected PlayerPrincipal admin() {
        return playerAccountRepository.findAllByOrderByUsernameAsc().stream()
                .filter(account -> account.getRole() == PlayerRole.ADMIN)
                .findFirst()
                .map(PlayerPrincipal::from)
                .orElseThrow();
    }

    /** Logs in through the real endpoint, so the session is registered like in production. */
    protected MockHttpSession logIn(String username, String password) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, password)))
                .andReturn()
                .getRequest()
                .getSession();
    }
}
