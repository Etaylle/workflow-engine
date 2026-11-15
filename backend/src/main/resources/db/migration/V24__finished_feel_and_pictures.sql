-- The steps ask for the complete known design, a finished feel, and a look at the pictures the
-- engine takes while it uses the software (owner decision of 2026-11-15). Idempotent.
update settings.profile_steps set prompt = prompt || $p$ When the wish names software with a well-known design (a classic game, a calculator, a to-do list), list every part of that design a user expects and plan all of them; for a classic game that is every rule, how the opponents behave, obstacles, bonus items, scoring, lives, levels, the start, pause and end screens, and sound. Describe every character and visual element the wish asks for: its shape, colours, and how it moves and is animated.$p$
 where id = md5('step:short:plan')::uuid and prompt not like '%every part of that design%';
update settings.profile_steps set prompt = prompt || $p$ Keep the rules of the software in code that has no drawing in it, and test those rules. Make the result feel finished, not like a draft: characters and pictures drawn with care and animated, a visible or audible answer to every action of the user, and start, pause and end screens where the kind of software has them.$p$
 where id = md5('step:short:build')::uuid and prompt not like '%feel finished%';
update settings.profile_steps set prompt = prompt || $p$ Look at the pictures in screens/ (the engine took them while it used the software in a browser; open each one) and judge them like a critical client against the wish: do the characters and the look match what the wish describes, is anything missing, ugly, overlapping, cut off or unreadable? Improve what falls short of the wish, then run ./check.sh again.$p$
 where id in (md5('step:short:check')::uuid, md5('step:full:16-audit')::uuid)
   and prompt not like '%pictures in screens/%';
