-- The steps no longer assume a backend, a database and a frontend (owner decision of 2026-11-15:
-- the technology follows the wish, and the result is verified by using it). The repeatable seed
-- holds these texts for a fresh database; this brings the seeded short profile of an existing one
-- up to date. Profiles the owner copied and edited are theirs and stay as they are.
update settings.profile_steps
   set prompt = $p$Step PLAN. Read CONTRACT.md, the guide file and the files already in the workspace. Write docs/PLAN.md, under 70 lines: (1) what will be built for "{task}", in the words of the wish; (2) the technology: the one the workspace gives, or, when the guide leaves the choice to you, the simplest one that fits the wish, with one sentence of reason (check what is installed on this machine first); (3) the acceptance criteria, numbered, each verifiable, covering everything a user expects from start to end, the steps done by a person included; (4) the parts to write, and only where the wish needs them the data to store and the interfaces; (5) the tests that prove the criteria, and how the result is tried by hand with ./start.sh. Do not write production code or tests in this step.$p$
 where id = md5('step:short:plan')::uuid;

update settings.profile_steps
   set prompt = $p$Step BUILD WITH TESTS. Implement docs/PLAN.md completely for "{task}": the working software, the tests that prove every criterion, and ./check.sh, ./start.sh and README.md as the guide file describes them. Then use the software yourself the way a client would: start it with ./start.sh and try every criterion (a web page: open it and use its controls; a console program: run it and read what it prints); fix what does not work when used, even if the tests pass. Run ./check.sh and fix until it prints CHECK PASSED.$p$
 where id = md5('step:short:build')::uuid;

update settings.profile_steps
   set prompt = prompt || $p$ Use the software as a client would: start it with ./start.sh and try every acceptance criterion yourself; a criterion that a test proves but that does not work when the software is used is a fail and must be fixed.$p$
 where id in (md5('step:short:check')::uuid, md5('step:full:16-audit')::uuid)
   and prompt not like '%as a client would%';
