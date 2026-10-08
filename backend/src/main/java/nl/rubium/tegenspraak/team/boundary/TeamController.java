package nl.rubium.tegenspraak.team.boundary;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import nl.rubium.tegenspraak.team.control.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public List<TeamResponse> getTeams(@AuthenticationPrincipal PlayerPrincipal player) {
        return teamService.getTeams(player.getId()).stream()
                .map(TeamResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse createTeam(
            @Valid @RequestBody CreateTeamRequest request, @AuthenticationPrincipal PlayerPrincipal player) {
        return TeamResponse.from(teamService.createTeam(player.getId(), request.name()));
    }

    @PostMapping("/join")
    public TeamResponse joinTeam(
            @Valid @RequestBody JoinTeamRequest request, @AuthenticationPrincipal PlayerPrincipal player) {
        return TeamResponse.from(teamService.joinTeam(player.getId(), request.joinCode()));
    }

    @GetMapping("/{teamId}")
    public TeamResponse getTeam(@PathVariable UUID teamId, @AuthenticationPrincipal PlayerPrincipal player) {
        return TeamResponse.from(teamService.getTeam(teamId, player.getId()));
    }

    @DeleteMapping("/{teamId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveTeam(@PathVariable UUID teamId, @AuthenticationPrincipal PlayerPrincipal player) {
        teamService.leaveTeam(teamId, player.getId());
    }
}
