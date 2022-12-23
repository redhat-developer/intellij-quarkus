package org.acme;

import io.quarkus.scheduler.Scheduled;

public class ScheduledResource {

    @Scheduled(cron = "* * * * * *")
    public void validCronA() {}

    @Scheduled(cron = "*/1 */1 */1 */1 */1 */1")
    public void validCronB() {}

    @Scheduled(cron = "${cron.expression}")
    public void validCronC() {}

    @Scheduled(cron = "59 * * * * *")
    public void validCronD() {}

    @Scheduled(cron = "* 59 * * * *")
    public void validCronE() {}

    @Scheduled(cron = "* * 23 * * *")
    public void validCronF() {}

    @Scheduled(cron = "* * * 31 * *")
    public void validCronG() {}

    @Scheduled(cron = "* * * * 12 *")
    public void validCronH() {}

    @Scheduled(cron = "* * * * DEC *")
    public void validCronI() {}

    @Scheduled(cron = "* * * * * 1")
    public void validCronJ() {}

    @Scheduled(cron = "* * * * * MON")
    public void validCronK() {}

    @Scheduled(cron = "* * * * * * 2021")
    public void validCronL() {}

    @Scheduled(cron = "")
    public void invalidCronA() {}

    @Scheduled(cron = "* * * * * * * *")
    public void invalidCronB() {}

    @Scheduled(cron = "60 * * * * *")
    public void invalidCronC() {}

    @Scheduled(cron = "* 60 * * * *")
    public void invalidCronD() {}

    @Scheduled(cron = "* * 24 * * *")
    public void invalidCronE() {}

    @Scheduled(cron = "* * * 32 * *")
    public void invalidCronF() {}

    @Scheduled(cron = "* * * * 13 *")
    public void invalidCronG() {}

    @Scheduled(cron = "* * * * january *")
    public void invalidCronH() {}

    @Scheduled(cron = "* * * * * 8")
    public void invalidCronI() {}

    @Scheduled(cron = "* * * * * monday")
    public void invalidCronJ() {}

    @Scheduled(cron = "* * * * * * 2100")
    public void invalidCronK() {}

    @Scheduled(cron = "${cron.expression}}")
    public void invalidCronL() {}

    @Scheduled(every = "1m")
    public void validEveryA() {}

    @Scheduled(every = "PT15M")
    public void validEveryB() {}

    @Scheduled(every = "${every.time}")
    public void validEveryC() {}

    @Scheduled(every = "1a")
    public void invalidEveryA() {}

    @Scheduled(every = "PT15MS")
    public void invalidEveryB() {}

    @Scheduled(every = "${every.time}}")
    public void invalidEveryC() {}

    @Scheduled(delayed = "1m")
    public void validDelayedA() {}

    @Scheduled(delayed = "PT15M")
    public void validDelayedB() {}

    @Scheduled(delayed = "${delayed.time}")
    public void validDelayedC() {}

    @Scheduled(delayed = "1a")
    public void invalidDelayedA() {}

    @Scheduled(delayed = "PT15MS")
    public void invalidDelayedB() {}

    @Scheduled(delayed = "${delayed.time}}")
    public void invalidDelayedC() {}
}