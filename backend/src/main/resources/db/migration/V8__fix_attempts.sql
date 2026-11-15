-- US-06.05: every attempt of a step is an agent call; a call whose check failed keeps the check output.
ALTER TABLE execution.agent_calls ADD COLUMN check_output text;
