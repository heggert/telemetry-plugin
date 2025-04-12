package art.chibi.telemetry

import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property

class ErrorReporterAppender(
    name: String,
    private val errorReporter: ErrorReporter
) : AbstractAppender(name, null, null, true, Property.EMPTY_ARRAY) {

    override fun append(event: LogEvent) {
        // If there's no exception, nothing to handle.
        val thrown = event.thrown ?: return

        // Pass the exception details to ErrorReporter
        val message = event.message?.formattedMessage
        errorReporter.reportException(thrown, message)
    }
}