package nl.rubium.tegenspraak.chat.entity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByTeamIdAndCaseIdAndCharacterIdOrderBySentAtAsc(
            UUID teamId, String caseId, String characterId);

    List<ChatMessage> findByTeamIdAndCaseIdAndCharacterIdOrderBySentAtDesc(
            UUID teamId, String caseId, String characterId, Limit limit);
}
