package engine.execution;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** The steps of a task as data: read from a profile file (the short profile is the default). */
final class StepProfile {

    private StepProfile() {
    }

    static List<StepDef> shortProfile() {
        try (InputStream in = StepProfile.class.getResourceAsStream("/execution/profile-short.txt")) {
            if (in == null) {
                throw new IllegalStateException("profile-short.txt is missing from the jar");
            }
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("The short step profile could not be read", e);
        }
    }

    static List<StepDef> parse(String text) {
        List<StepDef> steps = new ArrayList<>();
        String[] header = null;
        StringBuilder prompt = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            if (line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("=== ")) {
                add(steps, header, prompt);
                header = line.substring(4).split("\\|");
                prompt = new StringBuilder();
            } else {
                prompt.append(line).append('\n');
            }
        }
        add(steps, header, prompt);
        return List.copyOf(steps);
    }

    private static void add(List<StepDef> steps, String[] header, StringBuilder prompt) {
        if (header == null) {
            return;
        }
        if (header.length != 4) {
            throw new IllegalStateException("A step header needs key | title | gate | cap");
        }
        steps.add(new StepDef(header[0].trim(), header[1].trim(), "check".equals(header[2].trim()),
                new BigDecimal(header[3].trim()), prompt.toString().trim()));
    }
}
