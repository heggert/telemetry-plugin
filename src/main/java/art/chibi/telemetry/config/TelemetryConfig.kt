package art.chibi.telemetry.config

import javax.inject.Inject

class TelemetryConfig @Inject constructor(
        val hasuraAdminSecret: String,
        val hasuraEndpoint: String,
        val telemetryInterval: Int,
        val metricsTable: String,
        val exceptionLogTable: String
)
