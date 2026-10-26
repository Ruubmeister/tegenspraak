package nl.rubium.tegenspraak.chat.entity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByTeamIdAndCaseIdAndCharacterIdOrderBySentAtAsc(
            UUID teamId, String caseId, String characterId);

    List<ChatMessage> findByTeamIdAndCaseIdAndCharacterIdOrderBySentAtDesc(
            UUID teamId, String caseId, String characterId, Limit limit);

    /** Every mail or file the team has ever shown this character, also messages beyond the prompt history. */
    @Query("select distinct attachment from ChatMessage message join message.attachments attachment"
            + " where message.teamId = :teamId and message.caseId = :caseId and message.characterId = :characterId")
    List<ChatAttachment> findAttachmentsInConversation(
            @Param("teamId") UUID teamId, @Param("caseId") String caseId, @Param("characterId") String characterId);
}
