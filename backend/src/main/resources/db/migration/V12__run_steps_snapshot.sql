-- A run keeps the steps it started with (the profile may be edited later). Runs of earlier builds
-- have none and cannot be continued.
ALTER TABLE execution.runs ADD COLUMN steps_json text NOT NULL DEFAULT '[]';
