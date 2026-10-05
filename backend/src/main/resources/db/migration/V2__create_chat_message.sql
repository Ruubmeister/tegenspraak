CREATE TABLE chat_message
(
    id           UUID PRIMARY KEY,
    player_id    UUID         NOT NULL REFERENCES player_account (id) ON DELETE CASCADE,
    case_id      VARCHAR(100) NOT NULL,
    character_id VARCHAR(100) NOT NULL,
    sender       VARCHAR(20)  NOT NULL,
    content      TEXT         NOT NULL,
    sent_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX chat_message_conversation_idx ON chat_message (player_id, case_id, character_id, sent_at);
