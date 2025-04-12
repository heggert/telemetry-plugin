package art.chibi.telemetry

import art.chibi.telemetry.config.TelemetryConfig
import art.chibi.telemetry.di.DaggerTelemetryComponent
import art.chibi.telemetry.di.TelemetryComponent
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level
import javax.inject.Inject

class TelemetryPlugin : JavaPlugin() {

    // Injected services
    @Inject
    lateinit var serverStatsTelemetry: ServerStatsTelemetry

    @Inject
    lateinit var errorReporter: ErrorReporter

    override fun onEnable() {
        try {
            // 1) Load plugin configuration from config.yml
            val telemetryConfig = loadConfiguration()

            // 2) Build the Dagger component
            val component: TelemetryComponent = DaggerTelemetryComponent.builder()
                .pluginInstance(this)
                .logger(logger)
                .telemetryConfig(telemetryConfig)
                .build()

            // 3) Inject this plugin, populating serverStatsTelemetry & errorReporter
            component.inject(this)

            // 4) Install the Log4j2 appender for capturing exceptions
            errorReporter.installAppender()

            // 5) Initialize & schedule periodic stats
            serverStatsTelemetry.init()   // e.g. sets CPU markers
            serverStatsTelemetry.schedule(telemetryConfig.telemetryInterval)

            logger.info("TelemetryPlugin activated. Telemetry interval: ${telemetryConfig.telemetryInterval} Ticks.")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Initialization error", e)
        }
    }

    private fun loadConfiguration(): TelemetryConfig {
        saveDefaultConfig()

        val hasuraAdminSecret = config.getString("hasura-admin-secret", "") ?: ""
        val hasuraEndpoint = config.getString("hasura-endpoint", "https://graphql.chibi.art")
            ?: "https://graphql.chibi.art"
        val telemetryInterval = config.getInt("telemetry-interval", 200)
        val metricsTable = config.getString("metrics-table", "minecraft_metrics") ?: "minecraft_metrics"
        val exceptionLogTable = config.getString("exception-log-table", "exception_logs") ?: "exception_logs"

        return TelemetryConfig(
            hasuraAdminSecret = hasuraAdminSecret,
            hasuraEndpoint = hasuraEndpoint,
            telemetryInterval = telemetryInterval,
            metricsTable = metricsTable,
            exceptionLogTable = exceptionLogTable
        )
    }
}
