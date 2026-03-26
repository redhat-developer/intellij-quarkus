package org.acme.config;

import java.time.Duration;
import java.time.LocalTime;
import javax.ws.rs.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/greeting")
public class DefaultValueResource {

    @ConfigProperty(name = "greeting1", defaultValue = "foo")
    int greeting1;

    @ConfigProperty(name = "greeting2", defaultValue = "bar")
    Integer greeting2;

    @ConfigProperty(name = "greeting3", defaultValue = "1")
    boolean greeting3;

    @ConfigProperty(name = "greeting4", defaultValue = "128")
    byte greeting4;

    @ConfigProperty(name = "greeting5", defaultValue = "baz")
    String greeting5;

    @ConfigProperty(name = "greeting6", defaultValue = "1.0")
    float greeting6;

    @ConfigProperty(name = "greeting7", defaultValue = "java.lang.String")
    Class<?> greeting7;

    @ConfigProperty(name = "greeting8", defaultValue = "A")
    char greeting8;

    @ConfigProperty(name = "greeting9")
    String greeting9;

    @ConfigProperty(name = "greeting10", defaultValue = "AB")
    char greeting10;

    @ConfigProperty(name = "stocks.persister.path", defaultValue = "trades.fory")
    Path streamsDataPath;

    public enum ProcessingLevel {
        ALL,
        MESSAGES,
        MESSAGES_PERSIST,
    }

    @ConfigProperty(name = "stocks.handler.processingLevel", defaultValue = "ALL")
    ProcessingLevel processingLevel;

    @ConfigProperty(name = "stocks.handler.processingLevel.error", defaultValue = "FOO")
    ProcessingLevel processingLevelErr;

    @ConfigProperty(name = "startTime", defaultValue = "00:00")
    private LocalTime startTime;

    @ConfigProperty(name = "startTime.error", defaultValue = "00-00")
    private LocalTime startTimeErr;

    @ConfigProperty(name = "app.duration", defaultValue = "PT15M")
    Duration duration;

    @ConfigProperty(name = "app.duration.error", defaultValue = "PT15X")
    Duration durationErr;
}
