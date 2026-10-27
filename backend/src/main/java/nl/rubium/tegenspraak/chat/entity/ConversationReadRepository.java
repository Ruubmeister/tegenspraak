package nl.rubium.tegenspraak.chat.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationReadRepository extends JpaRepository<ConversationRead, UUID> {

    /**
     * Never moves the read position back, so a slow request for an older message cannot mark newer ones as unread
     * again.
     */
    @Modifying
    @Query(
            value = "insert into conversation_read (id, player_id, team_id, case_id, character_id, read_until)"
                    + " values (:id, :playerId, :teamId, :caseId, :characterId, :readUntil)"
                    + " on conflict (player_id, team_id, case_id, character_id)"
                    + " do update set read_until = greatest(conversation_read.read_until, excluded.read_until)",
            nativeQuery = true)
    void markReadUntil(
            @Param("id") UUID id,
            @Param("playerId") UUID playerId,
            @Param("teamId") UUID teamId,
            @Param("caseId") String caseId,
            @Param("characterId") String characterId,
            @Param("readUntil") Instant readUntil);

    /**
     * Characters with a reply or notice the player has not read yet. Player messages are left out: every one of them
     * comes with a character reply, which is what makes the conversation unread.
     */
    @Query(
            value = "select distinct message.character_id from chat_message message"
                    + " left join conversation_read seen on seen.player_id = :playerId"
                    + " and seen.team_id = message.team_id and seen.case_id = message.case_id"
                    + " and seen.character_id = message.character_id"
                    + " where message.team_id = :teamId and message.case_id = :caseId"
                    + " and message.sender <> 'PLAYER'"
                    + " and (seen.read_until is null or message.sent_at > seen.read_until)",
            nativeQuery = true)
    List<String> findUnreadCharacterIds(
            @Param("playerId") UUID playerId, @Param("teamId") UUID teamId, @Param("caseId") String caseId);
}
