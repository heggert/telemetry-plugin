package art.chibi.telemetry

import art.chibi.telemetry.config.TelemetryConfig
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sun.management.OperatingSystemMXBean
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.logging.Logger
import javax.inject.Inject

class ServerStatsTelemetry @Inject constructor(
    private val plugin: JavaPlugin,
    private val httpClient: HttpClient,
    private val osBean: OperatingSystemMXBean,
    private val telemetryConfig: TelemetryConfig,
    private val logger: Logger // We'll pass the plugin's logger or a separate DI'd logger
) {
    private var lastCpuTime: Long = -1
    private var lastSampleTime: Long = -1

    fun init() {
        // Initialize CPU usage markers
        lastCpuTime = osBean.processCpuTime
        lastSampleTime = System.nanoTime()
    }

    fun schedule(telemetryInterval: Int) {
        // Bukkit scheduler to run tasks
        Bukkit.getScheduler().runTaskTimer(
            plugin,
            Runnable { sendTelemetry() },
            0L,
            telemetryInterval.toLong()
        )
    }

    private fun sendTelemetry() {
        try {
            val tps = Bukkit.getServer().tps
            val tps1 = tps[0]
            val tps5 = tps[1]
            val tps15 = tps[2]
            val cpuUsage = calculateCpuUsage()
            val memoryUsage = calculateMemoryUsage()
            val onlinePlayers = Bukkit.getOnlinePlayers().size
            val timestamp = Instant.now().toString()

            val payload = buildPayload(timestamp, tps1, tps5, tps15, cpuUsage, memoryUsage, onlinePlayers)
            sendPayload(payload)
        } catch (e: Exception) {
            logger.warning("Error sending telemetry data: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun calculateCpuUsage(): Double {
        val currentCpuTime = osBean.processCpuTime
        val currentTime = System.nanoTime()
        var cpuUsage = 0.0
        if (lastCpuTime > 0 && lastSampleTime > 0) {
            val cpuTimeDelta = currentCpuTime - lastCpuTime
            val elapsedTime = currentTime - lastSampleTime
            val processors = Runtime.getRuntime().availableProcessors()
            cpuUsage = (cpuTimeDelta / (elapsedTime.toDouble() * processors)) * 100.0
        }
        lastCpuTime = currentCpuTime
        lastSampleTime = currentTime
        return cpuUsage
    }

    private fun calculateMemoryUsage(): Double {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024.0 * 1024.0)
    }

    private fun buildPayload(
        timestamp: String,
        tps1: Double, tps5: Double, tps15: Double,
        cpu: Double, memory: Double, onlineUsers: Int
    ): String {
        val query = "mutation logTelemetry(\$time: timestamptz!, \$tps1: float8!, \$tps5: float8!, \$tps15: float8!, " +
                "\$cpu: float8!, \$memory: float8!, \$onlineUsers: Int!) { " +
                "insert_${telemetryConfig.metricsTable}(objects: { " +
                "time: \$time, " +
                "tps_1min: \$tps1, " +
                "tps_5min: \$tps5, " +
                "tps_15min: \$tps15, " +
                "cpu_usage: \$cpu, " +
                "memory_usage: \$memory, " +
                "online_users: \$onlineUsers " +
                "}) { affected_rows } }"
        val variables = String.format(
            "{\"time\": \"%s\", \"tps1\": %.2f, \"tps5\": %.2f, \"tps15\": %.2f, \"cpu\": %.2f, \"memory\": %.2f, \"onlineUsers\": %d}",
            timestamp, tps1, tps5, tps15, cpu, memory, onlineUsers
        )
        return "{\"query\":\"$query\",\"variables\":$variables}"
    }

    private fun sendPayload(payload: String) {
        val request = HttpRequest.newBuilder()
            .uri(java.net.URI.create(telemetryConfig.hasuraEndpoint))
            .header("Content-Type", "application/json")
            .header("x-hasura-admin-secret", telemetryConfig.hasuraAdminSecret)
            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
            .build()

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept { response ->
                if (response.statusCode() != 200) {
                    logger.warning("Error sending telemetry data. HTTP ${response.statusCode()}: ${response.body()}")
                }
                logGraphQLErrors(response.body())
            }
            .exceptionally { ex ->
                logger.severe("HTTP error sending telemetry data: ${ex.message}")
                null
            }
    }

    private fun logGraphQLErrors(responseBody: String) {
        val element: JsonElement = JsonParser.parseString(responseBody)
        if (element.isJsonObject) {
            val jsonObj: JsonObject = element.asJsonObject
            if (jsonObj.has("errors")) {
                val errors = jsonObj.getAsJsonArray("errors")
                for (errorElem in errors) {
                    val error = errorElem.asJsonObject
                    val message = error.takeIf { it.has("message") }?.get("message")?.asString ?: "No message provided"
                    val sb = StringBuilder("GraphQL error: $message")
                    if (error.has("extensions")) {
                        val extensions = error.getAsJsonObject("extensions")
                        if (extensions.has("code")) {
                            sb.append(" | code: ").append(extensions.get("code").asString)
                        }
                        sb.append(" | extensions: ").append(extensions)
                    }
                    logger.warning(sb.toString())
                }
            }
        }
    }
}
