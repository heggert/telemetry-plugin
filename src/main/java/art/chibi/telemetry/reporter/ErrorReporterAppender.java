package art.chibi.telemetry.reporter;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

public class ErrorReporterAppender extends AbstractAppender {

    private final ErrorReporter errorReporter;

    public ErrorReporterAppender(String name, ErrorReporter errorReporter) {
        super(name, null, null, true, Property.EMPTY_ARRAY);
        this.errorReporter = errorReporter;
    }

    @Override
    public void append(LogEvent event) {
        // If there's no exception, nothing to handle.
        Throwable thrown = event.getThrown();
        if (thrown == null) {
            return;
        }

        // Pass the exception details to ErrorReporter
        String message = (event.getMessage() != null) ? event.getMessage().getFormattedMessage() : null;
        errorReporter.reportException(thrown, message);
    }
}
