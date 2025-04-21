package art.chibi.telemetry.reporter;

import art.chibi.telemetry.Config;
import art.chibi.telemetry.client.GraphQLClient;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import javax.inject.Inject;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ErrorReporter {

    private final GraphQLClient graphQL;
    private final Config cfg;
    private final Logger logger;

    @Inject
    public ErrorReporter(
            GraphQLClient graphQL,
            Config telemetryConfig,
            Logger logger
    ) {
        this.graphQL = graphQL;
        this.cfg = telemetryConfig;
        this.logger = logger;
    }

    /**
     * Installs the Log4j2 appender at runtime to catch exceptions.
     */
    public void installAppender() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();

        ErrorReporterAppender appender = new ErrorReporterAppender("ExceptionCatcher", this);
        appender.start();

        config.addAppender(appender);
        LoggerConfig rootLoggerConfig =
                config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        rootLoggerConfig.addAppender(appender, Level.ALL, null);

        ctx.updateLoggers();
        logger.info("[ErrorReporter] Log4j2 appender installed.");
    }

    /**
     * Called by the appender whenever an exception is detected in the logs.
     */
    public void reportException(Throwable cause, String logMessage) {
        // Timestamp
        String timestamp = Instant.now().toString();
        // Pick either the provided message or the exception's own message
        String message = (logMessage != null && !logMessage.isEmpty())
                ? logMessage
                : cause.getMessage();

        // Capture full stacktrace
        StringWriter sw = new StringWriter();
        cause.printStackTrace(new PrintWriter(sw));
        String stacktrace = sw.toString();

        // Build the GraphQL mutation
        String mutation =
                "mutation($time: timestamptz!, $level: String!, $logger: String, " +
                        "$thread: String, $message: String, $stacktrace: String) { " +
                        "insert_" + cfg.exceptionLogTable() + "(objects: { " +
                        "time: $time, " +
                        "level: $level, " +
                        "logger: $logger, " +
                        "thread: $thread, " +
                        "message: $message, " +
                        "stacktrace: $stacktrace " +
                        "}) { affected_rows } " +
                        "}";

        // Prepare variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("time", timestamp);
        variables.put("level", "ERROR");
        variables.put("logger", logger.getName());
        variables.put("thread", Thread.currentThread().getName());
        variables.put("message", message);
        variables.put("stacktrace", stacktrace);

        // Dispatch via the shared GraphQLClient (with retries, GOAWAY handling, etc.)
        graphQL.sendMutation(mutation, variables);
    }
}
