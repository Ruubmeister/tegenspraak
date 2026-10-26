-- Teams type their answers and get a limited number of attempts per case. Earlier attempts keep their order.
ALTER TABLE verdict_attempt
    ADD COLUMN attempt_number INTEGER;

UPDATE verdict_attempt attempt
SET attempt_number = numbered.attempt_number
FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY team_id, case_id ORDER BY submitted_at) AS attempt_number
      FROM verdict_attempt) numbered
WHERE attempt.id = numbered.id;

ALTER TABLE verdict_attempt
    ALTER COLUMN attempt_number SET NOT NULL;

-- Two teammates submitting at the same time cannot both take the same attempt
ALTER TABLE verdict_attempt
    ADD CONSTRAINT verdict_attempt_team_case_number_key UNIQUE (team_id, case_id, attempt_number);

DROP INDEX verdict_attempt_team_case_idx;

CREATE TABLE verdict_attempt_answer
(
    attempt_id  UUID         NOT NULL REFERENCES verdict_attempt (id) ON DELETE CASCADE,
    question_id VARCHAR(100) NOT NULL,
    answer      TEXT         NOT NULL,
    correct     BOOLEAN      NOT NULL,
    PRIMARY KEY (attempt_id, question_id)
);
