package art.chibi.telemetry;

import javax.inject.Inject;

public record Config(
        String hasuraAdminSecret,
        String hasuraEndpoint,
        int telemetryInterval,
        String metricsTable,
        String exceptionLogTable
) {

    @Inject
    public Config {
    }
}
