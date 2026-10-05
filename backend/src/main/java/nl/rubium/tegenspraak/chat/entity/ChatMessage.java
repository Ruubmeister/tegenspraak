package nl.rubium.tegenspraak.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One message in the conversation between a player and a character within a case. */
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Column(name = "character_id", nullable = false)
    private String characterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageSender sender;

    @Column(nullable = false)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    protected ChatMessage() {}

    public ChatMessage(UUID playerId, String caseId, String characterId, MessageSender sender, String content) {
        this.id = UUID.randomUUID();
        this.playerId = playerId;
        this.caseId = caseId;
        this.characterId = characterId;
        this.sender = sender;
        this.content = content;
        this.sentAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlayerId() {
        return playerId;
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

    public String getContent() {
        return content;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
