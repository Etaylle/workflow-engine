package engine.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

class StatusControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:15:30Z");

    @Test
    void reportsNameBuildIdUtcTimeAndDemoMode() {
        Properties properties = new Properties();
        properties.setProperty("version", "abc1234");
        StatusController controller =
                new StatusController(
                        "workflow-engine",
                        new BuildProperties(properties),
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        true);

        EngineStatus status = controller.status();

        assertThat(status).isEqualTo(new EngineStatus("workflow-engine", "abc1234", NOW, true));
    }
}
