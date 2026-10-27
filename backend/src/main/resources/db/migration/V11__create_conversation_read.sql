-- Up to which message a player has read a team's conversation with a character. Newer character messages and notices
-- mark the conversation as unread for that player.
CREATE TABLE conversation_read
(
    id           UUID PRIMARY KEY,
    player_id    UUID         NOT NULL REFERENCES player_account (id) ON DELETE CASCADE,
    team_id      UUID         NOT NULL REFERENCES team (id) ON DELETE CASCADE,
    case_id      VARCHAR(100) NOT NULL,
    character_id VARCHAR(100) NOT NULL,
    read_until   TIMESTAMPTZ  NOT NULL,
    UNIQUE (player_id, team_id, case_id, character_id)
);
