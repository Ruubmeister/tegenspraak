package nl.rubium.tegenspraak.team.control;

import java.util.List;
import java.util.UUID;
import nl.rubium.tegenspraak.liveupdate.entity.TeamEvent;
import nl.rubium.tegenspraak.team.entity.Team;
import nl.rubium.tegenspraak.team.entity.TeamMember;
import nl.rubium.tegenspraak.team.entity.TeamMemberRepository;
import nl.rubium.tegenspraak.team.entity.TeamRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Players investigate in teams. Playing alone is a team with one member. */
@Service
public class TeamService {

    private static final int MAX_CODE_ATTEMPTS = 10;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final JoinCodeGenerator joinCodeGenerator;
    private final ApplicationEventPublisher eventPublisher;

    public TeamService(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            JoinCodeGenerator joinCodeGenerator,
            ApplicationEventPublisher eventPublisher) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.joinCodeGenerator = joinCodeGenerator;
        this.eventPublisher = eventPublisher;
    }

    /** The creator becomes the first member. */
    @Transactional
    public TeamDetails createTeam(UUID playerId, String name) {
        Team team = teamRepository.save(new Team(name.strip(), uniqueJoinCode()));
        teamMemberRepository.save(new TeamMember(team.getId(), playerId));
        return details(team);
    }

    /** Joining a team the player is already in just returns it. */
    @Transactional
    public TeamDetails joinTeam(UUID playerId, String joinCode) {
        Team team = teamRepository
                .findByJoinCode(JoinCodeGenerator.normalize(joinCode))
                .orElseThrow(() -> new TeamNotFoundException("No team with this join code"));
        if (!teamMemberRepository.existsByTeamIdAndPlayerId(team.getId(), playerId)) {
            teamMemberRepository.save(new TeamMember(team.getId(), playerId));
            eventPublisher.publishEvent(TeamEvent.members(team.getId()));
        }
        return details(team);
    }

    public List<TeamDetails> getTeams(UUID playerId) {
        return teamRepository.findTeamsOfPlayer(playerId).stream()
                .map(this::details)
                .toList();
    }

    public TeamDetails getTeam(UUID teamId, UUID playerId) {
        requireMember(teamId, playerId);
        Team team = teamRepository
                .findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException("Team not found: " + teamId));
        return details(team);
    }

    /** A team without members is deleted, together with its chats and verdict attempts. */
    @Transactional
    public void leaveTeam(UUID teamId, UUID playerId) {
        requireMember(teamId, playerId);
        teamMemberRepository.deleteByTeamIdAndPlayerId(teamId, playerId);
        if (teamMemberRepository.countByTeamId(teamId) == 0) {
            teamRepository.deleteById(teamId);
        } else {
            eventPublisher.publishEvent(TeamEvent.members(teamId));
        }
    }

    /** Guards every team scoped endpoint. Non-members get the same answer as for a missing team. */
    public void requireMember(UUID teamId, UUID playerId) {
        if (!isMember(teamId, playerId)) {
            throw new TeamNotFoundException("Player " + playerId + " is not a member of team " + teamId);
        }
    }

    public boolean isMember(UUID teamId, UUID playerId) {
        return teamMemberRepository.existsByTeamIdAndPlayerId(teamId, playerId);
    }

    /** Called after deleting a player, whose memberships the database removed. */
    @Transactional
    public void deleteTeamsWithoutMembers() {
        teamRepository.deleteTeamsWithoutMembers();
    }

    private TeamDetails details(Team team) {
        return new TeamDetails(team, teamMemberRepository.findMemberUsernames(team.getId()));
    }

    private String uniqueJoinCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = joinCodeGenerator.generate();
            if (!teamRepository.existsByJoinCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique join code");
    }
}
