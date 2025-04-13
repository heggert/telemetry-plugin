package art.chibi.telemetry.reporter;

import art.chibi.telemetry.Config;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.management.OperatingSystemMXBean;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.logging.Logger;

public class ServerStatsReporter {

    private final JavaPlugin plugin;
    private final HttpClient httpClient;
    private final OperatingSystemMXBean osBean;
    private final Config telemetryConfig;
    private final Logger logger;

    private long lastCpuTime = -1;
    private long lastSampleTime = -1;

    @Inject
    public ServerStatsReporter(
            JavaPlugin plugin,
            HttpClient httpClient,
            OperatingSystemMXBean osBean,
            Config telemetryConfig,
            Logger logger
    ) {
        this.plugin = plugin;
        this.httpClient = httpClient;
        this.osBean = osBean;
        this.telemetryConfig = telemetryConfig;
        this.logger = logger;
    }

    public void init() {
        // Initialize CPU usage markers
        lastCpuTime = osBean.getProcessCpuTime();
        lastSampleTime = System.nanoTime();
    }

    public void schedule(int telemetryInterval) {
        // Schedules the sendTelemetry() method to run periodically
        Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::sendTelemetry,
                0L,
                telemetryInterval
        );
    }

    private void sendTelemetry() {
        try {
            double[] tps = Bukkit.getServer().getTPS();
            double tps1 = tps[0];
            double tps5 = tps[1];
            double tps15 = tps[2];

            double cpuUsage = calculateCpuUsage();
            double memoryUsage = calculateMemoryUsage();
            int onlinePlayers = Bukkit.getOnlinePlayers().size();
            String timestamp = Instant.now().toString();

            String payload = buildPayload(timestamp, tps1, tps5, tps15, cpuUsage, memoryUsage, onlinePlayers);
            sendPayload(payload);

        } catch (Exception e) {
            logger.warning("Error sending telemetry data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private double calculateCpuUsage() {
        long currentCpuTime = osBean.getProcessCpuTime();
        long currentTime = System.nanoTime();
        double cpuUsage = 0.0;

        if (lastCpuTime > 0 && lastSampleTime > 0) {
            long cpuTimeDelta = currentCpuTime - lastCpuTime;
            long elapsedTime = currentTime - lastSampleTime;
            int processors = Runtime.getRuntime().availableProcessors();
            cpuUsage = (cpuTimeDelta / (elapsedTime * 1.0 * processors)) * 100.0;
        }

        lastCpuTime = currentCpuTime;
        lastSampleTime = currentTime;
        return cpuUsage;
    }

    private double calculateMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        // Convert to MB
        return usedMemory / (1024.0 * 1024.0);
    }

    private String buildPayload(
            String timestamp,
            double tps1, double tps5, double tps15,
            double cpu, double memory, int onlineUsers
    ) {
        // GraphQL mutation
        String query =
                "mutation logTelemetry($time: timestamptz!, $tps1: float8!, $tps5: float8!, $tps15: float8!, " +
                        "$cpu: float8!, $memory: float8!, $onlineUsers: Int!) { " +
                        "insert_" + telemetryConfig.metricsTable() + "(objects: { " +
                        "time: $time, " +
                        "tps_1min: $tps1, " +
                        "tps_5min: $tps5, " +
                        "tps_15min: $tps15, " +
                        "cpu_usage: $cpu, " +
                        "memory_usage: $memory, " +
                        "online_users: $onlineUsers " +
                        "}) { affected_rows } " +
                        "}";

        // JSON for variables
        String variables = String.format(
                "{\"time\": \"%s\", \"tps1\": %.2f, \"tps5\": %.2f, \"tps15\": %.2f, \"cpu\": %.2f, \"memory\": %.2f, \"onlineUsers\": %d}",
                timestamp, tps1, tps5, tps15, cpu, memory, onlineUsers
        );

        // Return full GraphQL request body
        return "{\"query\":\"" + query + "\",\"variables\":" + variables + "}";
    }

    private void sendPayload(String payload) {
        final int maxRetries = 3;
        final long retryDelaySeconds = 1L;

        // Attempts to send the payload asynchronously; on "GOAWAY", schedule a retry
        attemptSend(payload, 1, maxRetries, retryDelaySeconds);
    }

    private void attemptSend(String payload, int attempt, int maxRetries, long retryDelaySeconds) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(telemetryConfig.hasuraEndpoint()))
                .header("Content-Type", "application/json")
                .header("x-hasura-admin-secret", telemetryConfig.hasuraAdminSecret())
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        logger.warning("Error sending telemetry data. HTTP " + response.statusCode() + ": " + response.body());
                    }
                    // Always check for GraphQL errors in the response
                    logGraphQLErrors(response.body());
                })
                .exceptionally(ex -> {
                    String message = (ex.getMessage() != null) ? ex.getMessage() : "Unknown error";

                    // Simple check for the "GOAWAY" message
                    if (message.toUpperCase().contains("GOAWAY")) {
                        if (attempt < maxRetries) {
                            int nextAttempt = attempt + 1;
                            logger.warning("Received HTTP/2 GOAWAY on attempt " + attempt + ". Retrying in " +
                                    retryDelaySeconds + " seconds (attempt " + nextAttempt + "/" + maxRetries + ")...");

                            Bukkit.getScheduler().runTaskLater(
                                    plugin,
                                    () -> attemptSend(payload, nextAttempt, maxRetries, retryDelaySeconds),
                                    retryDelaySeconds * 20
                            );
                        } else {
                            logger.severe("Max retries (" + maxRetries + ") reached. Giving up on sending telemetry.");
                        }
                    } else {
                        logger.severe("HTTP error sending telemetry data: " + message);
                    }
                    return null;
                });
    }

    private void logGraphQLErrors(String responseBody) {
        JsonElement element = JsonParser.parseString(responseBody);
        if (element.isJsonObject()) {
            JsonObject jsonObj = element.getAsJsonObject();
            if (jsonObj.has("errors")) {
                for (JsonElement errorElem : jsonObj.getAsJsonArray("errors")) {
                    JsonObject error = errorElem.getAsJsonObject();
                    String message = error.has("message") ? error.get("message").getAsString() : "No message provided";
                    StringBuilder sb = new StringBuilder("GraphQL error: ").append(message);

                    if (error.has("extensions")) {
                        JsonObject extensions = error.getAsJsonObject("extensions");
                        if (extensions.has("code")) {
                            sb.append(" | code: ").append(extensions.get("code").getAsString());
                        }
                        sb.append(" | extensions: ").append(extensions);
                    }
                    logger.warning(sb.toString());
                }
            }
        }
    }
}
