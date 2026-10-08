package nl.rubium.tegenspraak.team.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    Optional<Team> findByJoinCode(String joinCode);

    boolean existsByJoinCode(String joinCode);

    @Query("select team from Team team where team.id in"
            + " (select member.teamId from TeamMember member where member.playerId = :playerId)"
            + " order by lower(team.name)")
    List<Team> findTeamsOfPlayer(@Param("playerId") UUID playerId);

    @Modifying
    @Query("delete from Team team where not exists"
            + " (select member from TeamMember member where member.teamId = team.id)")
    int deleteTeamsWithoutMembers();
}
