package nl.rubium.tegenspraak.team.entity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    boolean existsByTeamIdAndPlayerId(UUID teamId, UUID playerId);

    long countByTeamId(UUID teamId);

    void deleteByTeamIdAndPlayerId(UUID teamId, UUID playerId);

    @Query("select account.username from TeamMember member"
            + " join PlayerAccount account on account.id = member.playerId"
            + " where member.teamId = :teamId order by member.joinedAt")
    List<String> findMemberUsernames(@Param("teamId") UUID teamId);
}
