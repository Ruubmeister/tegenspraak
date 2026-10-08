CREATE TABLE verdict_attempt
(
    id              UUID PRIMARY KEY,
    player_id       UUID         NOT NULL REFERENCES player_account (id) ON DELETE CASCADE,
    case_id         VARCHAR(100) NOT NULL,
    correct_answers INTEGER      NOT NULL,
    total_questions INTEGER      NOT NULL,
    solved          BOOLEAN      NOT NULL,
    submitted_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX verdict_attempt_player_case_idx ON verdict_attempt (player_id, case_id, submitted_at);
