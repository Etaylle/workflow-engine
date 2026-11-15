package engine.execution;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** What the builders share: the preamble, reading the files of a step, a failed outcome, the gate. */
final class BuilderSupport {

    /** Told to every builder before the step; the placeholder is the name of its guide file. */
    private static final String PREAMBLE = "You are one step of a task build run by an orchestrator,"
            + " working alone in the current folder (the workspace). Read %s and CONTRACT.md"
            + " first. Do only the step below, nothing from other steps. Earlier steps left their"
            + " documents in docs/. Do not run git commit or any other git command that changes the"
            + " repository; the orchestrator commits. Use ./check.sh as the only way to build and"
            + " test everything; it prints CHECK PASSED when green. Finish with one line saying"
            + " what you did.";

    private BuilderSupport() {
    }

    /** The preamble followed by the prompt of the step, for the builder whose guide file is named. */
    static String prompt(String guideFile, String stepPrompt) {
        return PREAMBLE.formatted(guideFile) + "\n\n" + stepPrompt;
    }

    static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    static String oneLine(String text) {
        String line = text.strip().replace('\n', ' ');
        return line.length() > 300 ? line.substring(0, 300) + "..." : line;
    }

    static StepOutcome failed(int seconds, String subtype, String summary) {
        return new StepOutcome(false, BigDecimal.ZERO, 0, seconds, subtype, summary);
    }

    /** Runs ./check.sh in the workspace: passed only with exit code 0 and the line CHECK PASSED. */
    static GateResult gate(ExecutionSettings settings, Path workspace, Map<String, String> appEnv,
            Path logDir) {
        Path out = logDir.resolve("check.log");
        ProcessRunner.Result result = ProcessRunner.run(List.of("./check.sh"), workspace, appEnv, out,
                logDir.resolve("check.err"), settings.checkTimeout());
        List<String> lines = new ArrayList<>(
                List.of((read(out) + read(logDir.resolve("check.err"))).split("\n")));
        boolean passed = result.exitCode() == 0 && lines.stream().anyMatch("CHECK PASSED"::equals);
        if (result.timedOut()) {
            lines.add("The check took longer than " + settings.checkTimeout().toMinutes()
                    + " minutes and was stopped");
        }
        return new GateResult(passed, lines.subList(Math.max(0, lines.size() - 12), lines.size()));
    }
}
