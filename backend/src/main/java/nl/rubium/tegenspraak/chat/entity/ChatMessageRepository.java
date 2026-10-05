package nl.rubium.tegenspraak.chat.entity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByPlayerIdAndCaseIdAndCharacterIdOrderBySentAtAsc(
            UUID playerId, String caseId, String characterId);

    List<ChatMessage> findByPlayerIdAndCaseIdAndCharacterIdOrderBySentAtDesc(
            UUID playerId, String caseId, String characterId, Limit limit);
}
