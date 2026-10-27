-- A character asked another character something on behalf of a team. The answer is posted once due_at has passed.
-- A request that keeps failing is deleted, so the character can ask again.
CREATE TABLE consultation_request
(
    id              UUID PRIMARY KEY,
    team_id         UUID         NOT NULL REFERENCES team (id) ON DELETE CASCADE,
    case_id         VARCHAR(100) NOT NULL,
    asker_id        VARCHAR(100) NOT NULL,
    consultation_id VARCHAR(100) NOT NULL,
    question        TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    failed_attempts INTEGER      NOT NULL,
    requested_at    TIMESTAMPTZ  NOT NULL,
    due_at          TIMESTAMPTZ  NOT NULL,
    UNIQUE (team_id, case_id, asker_id, consultation_id)
);

CREATE INDEX consultation_request_due_idx ON consultation_request (status, due_at);
