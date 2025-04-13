package art.chibi.telemetry;

import art.chibi.telemetry.reporter.ErrorReporter;
import art.chibi.telemetry.reporter.ServerStatsReporter;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import java.util.logging.Level;

public class TelemetryPlugin extends JavaPlugin {

    @Inject
    ServerStatsReporter serverStatsTelemetry;

    @Inject
    ErrorReporter errorReporter;

    @Override
    public void onEnable() {
        try {
            // 1) Load plugin configuration from config.yml
            Config telemetryConfig = loadConfiguration();

            // 2) Build the Dagger component
            TelemetryComponent component = DaggerTelemetryComponent.builder()
                    .pluginInstance(this)
                    .logger(getLogger())
                    .telemetryConfig(telemetryConfig)
                    .build();

            // 3) Inject this plugin, populating serverStatsTelemetry & errorReporter
            component.inject(this);

            // 4) Install the Log4j2 appender for capturing exceptions
            errorReporter.installAppender();

            // 5) Initialize & schedule periodic stats
            serverStatsTelemetry.init(); // e.g. sets CPU markers
            serverStatsTelemetry.schedule(telemetryConfig.telemetryInterval());

            getLogger().info("TelemetryPlugin activated. Telemetry interval: "
                    + telemetryConfig.telemetryInterval() + " Ticks.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Initialization error", e);
        }
    }

    private Config loadConfiguration() {
        saveDefaultConfig();

        String hasuraAdminSecret = getConfig().getString("hasura-admin-secret", "");
        String hasuraEndpoint = getConfig().getString("hasura-endpoint", "https://graphql.chibi.art");
        int telemetryInterval = getConfig().getInt("telemetry-interval", 200);
        String metricsTable = getConfig().getString("metrics-table", "minecraft_metrics");
        String exceptionLogTable = getConfig().getString("exception-log-table", "exception_logs");

        return new Config(
                hasuraAdminSecret,
                hasuraEndpoint,
                telemetryInterval,
                metricsTable,
                exceptionLogTable
        );
    }
}
