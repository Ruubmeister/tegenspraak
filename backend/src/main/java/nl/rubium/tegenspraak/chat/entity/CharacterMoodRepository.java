package nl.rubium.tegenspraak.chat.entity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterMoodRepository extends JpaRepository<CharacterMood, UUID> {

    /** Empty while the team has not talked to the character yet. */
    Optional<CharacterMood> findByTeamIdAndCaseIdAndCharacterId(UUID teamId, String caseId, String characterId);
}
