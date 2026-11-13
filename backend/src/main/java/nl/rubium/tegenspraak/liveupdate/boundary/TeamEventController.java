package nl.rubium.tegenspraak.liveupdate.boundary;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import nl.rubium.tegenspraak.liveupdate.control.TeamEventStreams;
import nl.rubium.tegenspraak.team.control.TeamService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Server-sent events telling the team's open clients what to load again, see TeamEvent. */
@RestController
public class TeamEventController {

    private final TeamService teamService;
    private final TeamEventStreams teamEventStreams;

    public TeamEventController(TeamService teamService, TeamEventStreams teamEventStreams) {
        this.teamService = teamService;
        this.teamEventStreams = teamEventStreams;
    }

    /**
     * nginx buffers proxied responses by default, which would hold the events back; the header turns that off. Left out
     * of the OpenAPI spec: the browser reads it with EventSource, and the frontend types the events itself.
     */
    @Hidden
    @GetMapping(path = "/api/teams/{teamId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(
            @PathVariable UUID teamId,
            @AuthenticationPrincipal PlayerPrincipal player,
            HttpSession session,
            HttpServletResponse response) {
        teamService.requireMember(teamId, player.getId());
        response.setHeader("X-Accel-Buffering", "no");
        return teamEventStreams.open(teamId, player.getId(), session.getId());
    }
}
