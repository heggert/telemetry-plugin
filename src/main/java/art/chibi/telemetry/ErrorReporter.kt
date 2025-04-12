package art.chibi.telemetry

import art.chibi.telemetry.config.TelemetryConfig
import java.net.http.HttpClient
import java.util.logging.Logger
import javax.inject.Inject

class ErrorReporter @Inject constructor(
    private val httpClient: HttpClient,
    private val telemetryConfig: TelemetryConfig,
    private val logger: Logger
) {

    /**
     * Called by the ErrorReporterAppender whenever a thrown exception is found in the logs.
     */
    fun reportException(cause: Throwable, logMessage: String?) {
        // 1) Print to console (or Paper server logger)
        logger.warning("[ErrorReporter] Caught exception: ${cause.message}")
        if (!logMessage.isNullOrEmpty()) {
            logger.warning("[ErrorReporter] Original log message: $logMessage")
        }
        cause.printStackTrace()

        // 2) Optionally, upload to GraphQL or Sentry, e.g. using httpClient
        // ...
    }

    /**
     * Installs the Log4j2 appender at runtime.
     */
    fun installAppender() {
        val ctx = org.apache.logging.log4j.LogManager.getContext(false) as org.apache.logging.log4j.core.LoggerContext
        val config = ctx.configuration

        // Create and start our custom appender that references *this* ErrorReporter
        val appender = ErrorReporterAppender("ExceptionCatcher", this)
        appender.start()

        // Add to config
        config.addAppender(appender)

        // Attach to the root logger
        val rootLoggerConfig = config.getLoggerConfig(org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME)
        rootLoggerConfig.addAppender(appender, org.apache.logging.log4j.Level.ALL, null)

        // Apply changes
        ctx.updateLoggers()
        logger.info("[ErrorReporter] Log4j2 appender installed.")
    }
}
