-- The last step of each seeded profile also proves that ./start.sh starts the app and that README.md
-- matches (owner decision of 2026-11-15). The repeatable seed holds the new text for a fresh database;
-- this migration brings an existing one up to date. Idempotent: a prompt that already says it stays.
update settings.profile_steps
   set prompt = prompt || $p$ Also prove that the application starts the way a client starts it: run ./start.sh in the background with APP_PORT set to a free port, wait (with curl, at most 3 minutes) until the application answers, then stop it. Check that README.md describes the application, its prerequisites, ./start.sh and ./check.sh as they are now; fix README.md or start.sh when they do not match.$p$
 where id in (md5('step:short:check')::uuid, md5('step:full:16-audit')::uuid)
   and prompt not like '%./start.sh%';
