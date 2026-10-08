package nl.rubium.tegenspraak.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One message in the conversation between a team and a character within a case. Player messages keep the author's
 * name, so the conversation stays readable after a player leaves the team.
 */
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Column(name = "character_id", nullable = false)
    private String characterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageSender sender;

    @Column(name = "author_name")
    private String authorName;

    @Column(nullable = false)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    protected ChatMessage() {}

    private ChatMessage(
            UUID teamId, String caseId, String characterId, MessageSender sender, String authorName, String content) {
        this.id = UUID.randomUUID();
        this.teamId = teamId;
        this.caseId = caseId;
        this.characterId = characterId;
        this.sender = sender;
        this.authorName = authorName;
        this.content = content;
        this.sentAt = Instant.now();
    }

    public static ChatMessage fromPlayer(
            UUID teamId, String caseId, String characterId, String authorName, String content) {
        return new ChatMessage(teamId, caseId, characterId, MessageSender.PLAYER, authorName, content);
    }

    public static ChatMessage fromCharacter(UUID teamId, String caseId, String characterId, String content) {
        return new ChatMessage(teamId, caseId, characterId, MessageSender.CHARACTER, null, content);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getCharacterId() {
        return characterId;
    }

    public MessageSender getSender() {
        return sender;
    }

    /** Null for character messages. */
    public String getAuthorName() {
        return authorName;
    }

    public String getContent() {
        return content;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
