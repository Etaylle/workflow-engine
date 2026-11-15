-- The independent check compares the result with the original wish and the kind of software (owner
-- decision of 2026-11-15). The repeatable seed holds the text for a fresh database; this brings an
-- existing one up to date. Idempotent: a prompt that already says it stays.
update settings.profile_steps
   set prompt = prompt || $p$ Compare the result with the ORIGINAL WISH (section Wish of CONTRACT.md) and the kind of software (section Software template of CONTRACT.md), not only with the task text. The verdict is NOT DONE when the program is not the kind of software the wish asks for (for example a web service, web page or database for a console wish, or a console program for a web wish), when ./start.sh does not start it, or when README.md does not match.$p$
 where id in (md5('step:short:check')::uuid, md5('step:full:16-audit')::uuid)
   and prompt not like '%ORIGINAL WISH%';
