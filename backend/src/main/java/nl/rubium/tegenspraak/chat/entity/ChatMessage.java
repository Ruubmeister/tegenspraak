package nl.rubium.tegenspraak.chat.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

/**
 * One message in the conversation between a team and a character within a case. Player messages keep the author's
 * name, so the conversation stays readable after a player leaves the team. Player messages may carry attachments.
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

    /** Loaded eagerly in batches: a conversation is always shown with its attachments. */
    @ElementCollection(fetch = FetchType.EAGER)
    @BatchSize(size = 50)
    @CollectionTable(name = "chat_message_attachment", joinColumns = @JoinColumn(name = "message_id"))
    @OrderColumn(name = "position")
    private List<ChatAttachment> attachments = new ArrayList<>();

    protected ChatMessage() {}

    private ChatMessage(
            UUID teamId,
            String caseId,
            String characterId,
            MessageSender sender,
            String authorName,
            String content,
            List<ChatAttachment> attachments) {
        this.id = UUID.randomUUID();
        this.teamId = teamId;
        this.caseId = caseId;
        this.characterId = characterId;
        this.sender = sender;
        this.authorName = authorName;
        this.content = content;
        this.sentAt = Instant.now();
        this.attachments = new ArrayList<>(attachments);
    }

    public static ChatMessage fromPlayer(
            UUID teamId,
            String caseId,
            String characterId,
            String authorName,
            String content,
            List<ChatAttachment> attachments) {
        return new ChatMessage(teamId, caseId, characterId, MessageSender.PLAYER, authorName, content, attachments);
    }

    public static ChatMessage fromCharacter(UUID teamId, String caseId, String characterId, String content) {
        return new ChatMessage(teamId, caseId, characterId, MessageSender.CHARACTER, null, content, List.of());
    }

    public static ChatMessage notice(UUID teamId, String caseId, String characterId, String content) {
        return new ChatMessage(teamId, caseId, characterId, MessageSender.NOTICE, null, content, List.of());
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

    /** Null for character messages and notices. */
    public String getAuthorName() {
        return authorName;
    }

    public String getContent() {
        return content;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    /** Empty for character messages and notices. */
    public List<ChatAttachment> getAttachments() {
        return List.copyOf(attachments);
    }
}
