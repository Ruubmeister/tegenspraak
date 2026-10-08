CREATE TABLE team_unlock
(
    id          UUID PRIMARY KEY,
    team_id     UUID         NOT NULL REFERENCES team (id) ON DELETE CASCADE,
    case_id     VARCHAR(100) NOT NULL,
    target      VARCHAR(400) NOT NULL,
    unlocked_at TIMESTAMPTZ  NOT NULL,
    UNIQUE (team_id, case_id, target)
);

CREATE TABLE wrong_code_attempt
(
    id           UUID PRIMARY KEY,
    team_id      UUID         NOT NULL REFERENCES team (id) ON DELETE CASCADE,
    case_id      VARCHAR(100) NOT NULL,
    target       VARCHAR(400) NOT NULL,
    attempted_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX wrong_code_attempt_target_idx ON wrong_code_attempt (team_id, case_id, target, attempted_at);
