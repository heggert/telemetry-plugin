package art.chibi.telemetry;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Level;
import java.time.Instant;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TelemetryPlugin extends JavaPlugin {

    private static final String CONFIG_HASURA_ADMIN_SECRET = "hasura-admin-secret";
    private static final String CONFIG_HASURA_ENDPOINT = "hasura-endpoint";
    private static final String CONFIG_TELEMETRY_INTERVAL = "telemetry-interval";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private OperatingSystemMXBean osBean;
    private String hasuraAdminSecret;
    private String hasuraEndpoint;
    private int telemetryInterval; // in ticks

    // Fields for CPU usage calculation using process CPU time.
    private long lastCpuTime = -1;
    private long lastSampleTime = -1;

    @Override
    public void onEnable() {
        try {
            loadConfiguration();
            initCpuUsageMarkers();
            scheduleTelemetryTask();
            getLogger().info("TelemetryPlugin has been enabled. Telemetry interval: " + telemetryInterval + " ticks.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Initialization error", e);
        }
    }

    private void loadConfiguration() {
        saveDefaultConfig();
        hasuraAdminSecret = getConfig().getString(CONFIG_HASURA_ADMIN_SECRET, "");
        hasuraEndpoint = getConfig().getString(CONFIG_HASURA_ENDPOINT, "");
        telemetryInterval = getConfig().getInt(CONFIG_TELEMETRY_INTERVAL, 200);
    }

    private void initCpuUsageMarkers() {
        osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        lastCpuTime = osBean.getProcessCpuTime();
        lastSampleTime = System.nanoTime();
    }

    private void scheduleTelemetryTask() {
        Bukkit.getScheduler().runTaskTimer(this, this::sendTelemetry, 0L, telemetryInterval);
    }

    private void sendTelemetry() {
        try {
            // Gather metrics
            double[] tps = Bukkit.getServer().getTPS();
            double tps1 = tps[0], tps5 = tps[1], tps15 = tps[2];
            double cpuUsage = calculateCpuUsage();
            double memoryUsage = calculateMemoryUsage();
            int onlinePlayers = Bukkit.getOnlinePlayers().size();
            String timestamp = Instant.now().toString();

            // Build and send payload
            String payload = buildPayload(timestamp, tps1, tps5, tps15, cpuUsage, memoryUsage, onlinePlayers);
            sendPayload(payload);
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Exception occurred while sending telemetry data", e);
        }
    }

    /**
     * Calculates the server process's CPU usage percentage based on the change in
     * CPU time.
     */
    private double calculateCpuUsage() {
        long currentCpuTime = osBean.getProcessCpuTime();
        long currentTime = System.nanoTime();
        double cpuUsage = 0;
        if (lastCpuTime > 0 && lastSampleTime > 0) {
            long cpuTimeDelta = currentCpuTime - lastCpuTime;
            long elapsedTime = currentTime - lastSampleTime;
            int processors = Runtime.getRuntime().availableProcessors();
            cpuUsage = (cpuTimeDelta / (double) (elapsedTime * processors)) * 100.0;
        }
        lastCpuTime = currentCpuTime;
        lastSampleTime = currentTime;
        return cpuUsage;
    }

    /**
     * Calculates the used memory (in MB) of the server process.
     */
    private double calculateMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return usedMemory / (1024.0 * 1024.0);
    }

    /**
     * Builds the GraphQL payload string with the given metrics.
     */
    private String buildPayload(String timestamp, double tps1, double tps5, double tps15, double cpu,
            double memory, int onlineUsers) {
        String query = "mutation logTelemetry($time: timestamptz!, $tps1: float8!, $tps5: float8!, $tps15: float8!, $cpu: float8!, $memory: float8!, $onlineUsers: Int!) { "
                + "insert_minecraft_metrics(objects: { "
                + "time: $time, "
                + "tps_1min: $tps1, "
                + "tps_5min: $tps5, "
                + "tps_15min: $tps15, "
                + "cpu_usage: $cpu, "
                + "memory_usage: $memory, "
                + "online_users: $onlineUsers "
                + "}) { affected_rows } }";
        String variables = String.format(
                "{\"time\": \"%s\", \"tps1\": %.2f, \"tps5\": %.2f, \"tps15\": %.2f, \"cpu\": %.2f, \"memory\": %.2f, \"onlineUsers\": %d}",
                timestamp, tps1, tps5, tps15, cpu, memory, onlineUsers);
        return "{\"query\":\"" + query + "\",\"variables\":" + variables + "}";
    }

    /**
     * Sends the telemetry payload to the Hasura endpoint.
     */
    private void sendPayload(String payload) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(hasuraEndpoint))
                .header("Content-Type", "application/json")
                .header("x-hasura-admin-secret", hasuraAdminSecret)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    try {
                        if (response.statusCode() != 200) {
                            getLogger().warning("Non-200 HTTP response code: " + response.statusCode());
                        }
                        logGraphQLErrors(response.body());
                    } catch (Exception e) {
                        getLogger().log(Level.WARNING, "Failed to process GraphQL response", e);
                    }
                })
                .exceptionally(ex -> {
                    getLogger().log(Level.SEVERE, "Failed to send telemetry: " + ex.getMessage(), ex);
                    return null;
                });
    }

    /**
     * Parses the GraphQL response body and logs any errors found.
     */
    private void logGraphQLErrors(String responseBody) {
        JsonElement element = JsonParser.parseString(responseBody);
        if (element.isJsonObject()) {
            JsonObject jsonObj = element.getAsJsonObject();
            if (jsonObj.has("errors")) {
                JsonArray errors = jsonObj.getAsJsonArray("errors");
                for (int i = 0; i < errors.size(); i++) {
                    JsonObject error = errors.get(i).getAsJsonObject();
                    String message = error.has("message") ? error.get("message").getAsString() : "No message provided";
                    StringBuilder logMessage = new StringBuilder("GraphQL error: ").append(message);
                    if (error.has("extensions")) {
                        JsonObject extensions = error.getAsJsonObject("extensions");
                        if (extensions.has("code")) {
                            logMessage.append(" | code: ").append(extensions.get("code").getAsString());
                        }
                        logMessage.append(" | extensions: ").append(extensions.toString());
                    }
                    getLogger().warning(logMessage.toString());
                }
            }
        }
    }
}
