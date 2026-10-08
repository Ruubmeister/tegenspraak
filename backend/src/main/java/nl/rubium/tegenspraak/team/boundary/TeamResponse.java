package nl.rubium.tegenspraak.team.boundary;

import java.util.List;
import java.util.UUID;
import nl.rubium.tegenspraak.team.control.JoinCodeGenerator;
import nl.rubium.tegenspraak.team.control.TeamDetails;

/** Only sent to members, who may share the join code with others. */
public record TeamResponse(UUID id, String name, String joinCode, List<String> members) {

    static TeamResponse from(TeamDetails details) {
        return new TeamResponse(
                details.team().getId(),
                details.team().getName(),
                JoinCodeGenerator.format(details.team().getJoinCode()),
                details.memberUsernames());
    }
}
