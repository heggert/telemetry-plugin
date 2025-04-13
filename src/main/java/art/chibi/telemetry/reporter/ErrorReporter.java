package art.chibi.telemetry.reporter;

import art.chibi.telemetry.Config;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;

import javax.inject.Inject;
import java.net.http.HttpClient;
import java.util.logging.Logger;

public class ErrorReporter {

    private final Logger logger;

    @Inject
    public ErrorReporter(HttpClient httpClient, Config telemetryConfig, Logger logger) {
        this.logger = logger;
    }

    /**
     * Called by the ErrorReporterAppender whenever a thrown exception is found in the logs.
     *
     * @param cause      The thrown exception to be reported
     * @param logMessage The log message associated with the exception
     */
    public void reportException(Throwable cause, String logMessage) {
        // 1) Print to console (or Paper server logger)
        logger.warning("[ErrorReporter] Caught exception: " + cause.getMessage());
        if (logMessage != null && !logMessage.isEmpty()) {
            logger.warning("[ErrorReporter] Original log message: " + logMessage);
        }
        cause.printStackTrace();

        // 2) Optionally, upload to GraphQL or Sentry, e.g. using httpClient
        // ...
    }

    /**
     * Installs the Log4j2 appender at runtime.
     */
    public void installAppender() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();

        // Create and start our custom appender that references *this* ErrorReporter
        ErrorReporterAppender appender = new ErrorReporterAppender("ExceptionCatcher", this);
        appender.start();

        // Add to config
        config.addAppender(appender);

        // Attach to the root logger
        org.apache.logging.log4j.core.config.LoggerConfig rootLoggerConfig =
                config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        rootLoggerConfig.addAppender(appender, Level.ALL, null);

        // Apply changes
        ctx.updateLoggers();
        logger.info("[ErrorReporter] Log4j2 appender installed.");
    }
}
