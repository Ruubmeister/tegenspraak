CREATE TABLE team
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    join_code  VARCHAR(8)  NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE team_member
(
    id        UUID PRIMARY KEY,
    team_id   UUID        NOT NULL REFERENCES team (id) ON DELETE CASCADE,
    player_id UUID        NOT NULL REFERENCES player_account (id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL,
    UNIQUE (team_id, player_id)
);

CREATE INDEX team_member_player_idx ON team_member (player_id);

-- Progress belongs to teams instead of players; earlier per player progress is dropped
DROP TABLE chat_message;
DROP TABLE verdict_attempt;

CREATE TABLE chat_message
(
    id           UUID PRIMARY KEY,
    team_id      UUID         NOT NULL REFERENCES team (id) ON DELETE CASCADE,
    case_id      VARCHAR(100) NOT NULL,
    character_id VARCHAR(100) NOT NULL,
    sender       VARCHAR(20)  NOT NULL,
    author_name  VARCHAR(50),
    content      TEXT         NOT NULL,
    sent_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX chat_message_conversation_idx ON chat_message (team_id, case_id, character_id, sent_at);

CREATE TABLE verdict_attempt
(
    id              UUID PRIMARY KEY,
    team_id         UUID         NOT NULL REFERENCES team (id) ON DELETE CASCADE,
    case_id         VARCHAR(100) NOT NULL,
    submitted_by    VARCHAR(50)  NOT NULL,
    correct_answers INTEGER      NOT NULL,
    total_questions INTEGER      NOT NULL,
    solved          BOOLEAN      NOT NULL,
    submitted_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX verdict_attempt_team_case_idx ON verdict_attempt (team_id, case_id, submitted_at);
