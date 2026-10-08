package nl.rubium.tegenspraak.team.control;

import java.util.List;
import nl.rubium.tegenspraak.team.entity.Team;

/** A team with the usernames of its members, in the order they joined. */
public record TeamDetails(Team team, List<String> memberUsernames) {

    public TeamDetails {
        memberUsernames = List.copyOf(memberUsernames);
    }
}
