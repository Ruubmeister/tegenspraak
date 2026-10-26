-- How a character feels about a team, from -100 (hostile) to 100 (trusting). Shifts with the tone of the team's messages.
CREATE TABLE character_mood
(
    id           UUID PRIMARY KEY,
    team_id      UUID         NOT NULL REFERENCES team (id) ON DELETE CASCADE,
    case_id      VARCHAR(100) NOT NULL,
    character_id VARCHAR(100) NOT NULL,
    score        INTEGER      NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    UNIQUE (team_id, case_id, character_id)
);
