package nl.rubium.tegenspraak.liveupdate.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import nl.rubium.tegenspraak.liveupdate.entity.TeamEvent;
import nl.rubium.tegenspraak.team.control.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class TeamEventStreamsTest {

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID OTHER_TEAM_ID = UUID.randomUUID();
    private static final UUID PLAYER_ID = UUID.randomUUID();
    private static final String SESSION_ID = "sessie-1";

    private final TeamService teamService = mock(TeamService.class);
    private final SessionRegistry sessionRegistry = mock(SessionRegistry.class);
    private final TeamEventStreams streams = new TeamEventStreams(
            teamService, sessionRegistry, new LiveUpdateProperties(Duration.ofSeconds(25), Duration.ofMinutes(30)));

    @BeforeEach
    void setUp() {
        when(teamService.isMember(TEAM_ID, PLAYER_ID)).thenReturn(true);
        when(sessionRegistry.getSessionInformation(SESSION_ID))
                .thenReturn(new SessionInformation("speler", SESSION_ID, new Date()));
    }

    @Test
    void publish_givenStreamOfTeam_expectEventSentAsJson() {
        RecordingEmitter emitter = register(TEAM_ID);

        streams.publish(TeamEvent.chat(TEAM_ID, "zaak", "de-butler"));

        assertThat(emitter.sentText())
                .contains("data:{\"type\":\"CHAT\",\"caseId\":\"zaak\",\"characterId\":\"de-butler\"}");
    }

    @Test
    void publish_givenStreamOfOtherTeam_expectNothingSent() {
        RecordingEmitter emitter = register(OTHER_TEAM_ID);

        streams.publish(TeamEvent.members(TEAM_ID));

        assertThat(emitter.sentText()).isEmpty();
    }

    @Test
    void publish_givenClosedStream_expectStreamDropped() {
        RecordingEmitter emitter = register(TEAM_ID);
        emitter.failOnSend = true;

        streams.publish(TeamEvent.members(TEAM_ID));

        assertThat(streams.streamsOf(TEAM_ID)).isEmpty();
    }

    @Test
    void checkStreams_givenMemberWithLiveSession_expectHeartbeatSent() {
        RecordingEmitter emitter = register(TEAM_ID);

        streams.checkStreams();

        assertThat(emitter.sentText()).contains(":ping");
        assertThat(streams.streamsOf(TEAM_ID)).hasSize(1);
    }

    @Test
    void checkStreams_givenPlayerLeftTeam_expectStreamClosed() {
        RecordingEmitter emitter = register(TEAM_ID);
        when(teamService.isMember(TEAM_ID, PLAYER_ID)).thenReturn(false);

        streams.checkStreams();

        assertThat(streams.streamsOf(TEAM_ID)).isEmpty();
        assertThat(emitter.sentText()).doesNotContain("ping");
    }

    @Test
    void checkStreams_givenSessionEnded_expectStreamClosed() {
        register(TEAM_ID);
        when(sessionRegistry.getSessionInformation(SESSION_ID)).thenReturn(null);

        streams.checkStreams();

        assertThat(streams.streamsOf(TEAM_ID)).isEmpty();
    }

    @Test
    void checkStreams_givenSessionExpired_expectStreamClosed() {
        register(TEAM_ID);
        SessionInformation session = new SessionInformation("speler", SESSION_ID, new Date());
        session.expireNow();
        when(sessionRegistry.getSessionInformation(SESSION_ID)).thenReturn(session);

        streams.checkStreams();

        assertThat(streams.streamsOf(TEAM_ID)).isEmpty();
    }

    private RecordingEmitter register(UUID teamId) {
        RecordingEmitter emitter = new RecordingEmitter();
        streams.register(new TeamEventStreams.TeamStream(teamId, PLAYER_ID, SESSION_ID, emitter));
        return emitter;
    }

    /** Collects what would be written to the response; JSON data is written the way Spring MVC would. */
    private static final class RecordingEmitter extends SseEmitter {

        private final List<String> sent = new ArrayList<>();
        private final JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter();
        private boolean failOnSend;

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (failOnSend) {
                throw new IOException("Broken pipe");
            }
            Set<ResponseBodyEmitter.DataWithMediaType> parts = builder.build();
            sent.add(parts.stream().map(this::asText).collect(Collectors.joining()));
        }

        private String asText(ResponseBodyEmitter.DataWithMediaType part) {
            if (part.getData() instanceof String text) {
                return text;
            }
            try {
                MockHttpOutputMessage output = new MockHttpOutputMessage();
                converter.write(part.getData(), part.getMediaType(), output);
                return output.getBodyAsString();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        String sentText() {
            return String.join("", sent);
        }
    }
}
