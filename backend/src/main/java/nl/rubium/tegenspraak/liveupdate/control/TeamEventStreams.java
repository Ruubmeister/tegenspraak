package nl.rubium.tegenspraak.liveupdate.control;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import nl.rubium.tegenspraak.liveupdate.entity.TeamEvent;
import nl.rubium.tegenspraak.liveupdate.entity.TeamEventType;
import nl.rubium.tegenspraak.team.control.TeamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The open server-sent event streams per team. Team events are pushed once the change that caused them is committed,
 * so a client never loads data that is not stored yet. A regular heartbeat keeps proxies from closing idle streams and
 * closes streams of players who left the team or whose session ended. Streams also end after a fixed time; the browser
 * then reconnects, which checks the session again. The application runs as a single instance, so keeping the streams
 * in memory is enough.
 */
@Component
public class TeamEventStreams {

    private static final Logger LOG = LoggerFactory.getLogger(TeamEventStreams.class);

    /** How long the browser waits before reconnecting after the stream dropped. */
    private static final Duration RECONNECT_DELAY = Duration.ofSeconds(3);

    record TeamStream(UUID teamId, UUID playerId, String sessionId, SseEmitter emitter) {}

    record EventMessage(TeamEventType type, String caseId, String characterId) {}

    private final Map<UUID, List<TeamStream>> streamsByTeam = new ConcurrentHashMap<>();
    private final TeamService teamService;
    private final SessionRegistry sessionRegistry;
    private final Duration streamTimeout;

    public TeamEventStreams(
            TeamService teamService,
            SessionRegistry sessionRegistry,
            @Value("${tegenspraak.live-updates.stream-timeout}") Duration streamTimeout) {
        this.teamService = teamService;
        this.sessionRegistry = sessionRegistry;
        this.streamTimeout = streamTimeout;
    }

    /** The caller has checked that the player is a member of the team. */
    public SseEmitter open(UUID teamId, UUID playerId, String sessionId) {
        TeamStream stream = new TeamStream(teamId, playerId, sessionId, new SseEmitter(streamTimeout.toMillis()));
        register(stream);
        // Sent right away, so the browser sees the stream open before the first event
        send(
                stream,
                SseEmitter.event().reconnectTime(RECONNECT_DELAY.toMillis()).comment("verbonden"));
        return stream.emitter();
    }

    void register(TeamStream stream) {
        streamsByTeam
                .computeIfAbsent(stream.teamId(), teamId -> new CopyOnWriteArrayList<>())
                .add(stream);
        stream.emitter().onCompletion(() -> remove(stream));
        stream.emitter().onTimeout(() -> remove(stream));
        stream.emitter().onError(error -> remove(stream));
    }

    /** An event published outside a transaction, such as after storing a verdict attempt, is pushed right away. */
    @TransactionalEventListener(fallbackExecution = true)
    public void publish(TeamEvent event) {
        EventMessage message = new EventMessage(event.type(), event.caseId(), event.characterId());
        for (TeamStream stream : streamsOf(event.teamId())) {
            send(stream, SseEmitter.event().data(message, MediaType.APPLICATION_JSON));
        }
    }

    @Scheduled(
            initialDelayString = "${tegenspraak.live-updates.heartbeat-interval}",
            fixedDelayString = "${tegenspraak.live-updates.heartbeat-interval}")
    public void checkStreams() {
        for (List<TeamStream> streams : streamsByTeam.values()) {
            for (TeamStream stream : streams) {
                if (isStillAllowed(stream)) {
                    send(stream, SseEmitter.event().comment("ping"));
                } else {
                    remove(stream);
                    stream.emitter().complete();
                }
            }
        }
    }

    List<TeamStream> streamsOf(UUID teamId) {
        return List.copyOf(streamsByTeam.getOrDefault(teamId, List.of()));
    }

    private boolean isStillAllowed(TeamStream stream) {
        SessionInformation session = sessionRegistry.getSessionInformation(stream.sessionId());
        return session != null && !session.isExpired() && teamService.isMember(stream.teamId(), stream.playerId());
    }

    /** A stream the browser closed fails on the next send; it is dropped then. */
    private void send(TeamStream stream, SseEmitter.SseEventBuilder event) {
        try {
            stream.emitter().send(event);
        } catch (IOException | IllegalStateException e) {
            LOG.debug("Dropping closed event stream of player {} in team {}", stream.playerId(), stream.teamId());
            remove(stream);
        }
    }

    private void remove(TeamStream stream) {
        streamsByTeam.computeIfPresent(stream.teamId(), (teamId, streams) -> {
            streams.remove(stream);
            return streams.isEmpty() ? null : streams;
        });
    }
}
