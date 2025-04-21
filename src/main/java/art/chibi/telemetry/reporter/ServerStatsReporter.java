package art.chibi.telemetry.reporter;

import art.chibi.telemetry.Config;
import art.chibi.telemetry.client.GraphQLClient;
import com.sun.management.OperatingSystemMXBean;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ServerStatsReporter {

    private final GraphQLClient graphQL;
    private final OperatingSystemMXBean osBean;
    private final Config cfg;
    private final JavaPlugin plugin;

    // For CPU usage delta calculations
    private long lastCpuTime = -1;
    private long lastSampleTime = -1;

    @Inject
    public ServerStatsReporter(
            GraphQLClient graphQL,
            OperatingSystemMXBean osBean,
            Config telemetryConfig,
            JavaPlugin plugin
    ) {
        this.graphQL = graphQL;
        this.osBean = osBean;
        this.cfg = telemetryConfig;
        this.plugin = plugin;
    }

    /**
     * Call once at startup to initialize CPU‐usage sampling.
     */
    public void init() {
        lastCpuTime = osBean.getProcessCpuTime();
        lastSampleTime = System.nanoTime();
    }

    /**
     * Schedule sendTelemetry() to run every `interval` ticks.
     */
    public void schedule(int interval) {
        Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::sendTelemetry,
                0L,
                interval
        );
    }

    /**
     * Gather server stats, build GraphQL mutation + variables, and dispatch.
     */
    private void sendTelemetry() {
        try {
            double[] tps = Bukkit.getServer().getTPS();
            double cpu = calculateCpuUsage();
            double memory = calculateMemoryUsage();
            int users = Bukkit.getOnlinePlayers().size();
            String time = Instant.now().toString();

            String mutation =
                    "mutation($time: timestamptz!, $tps1: float8!, $tps5: float8!, $tps15: float8!, " +
                            "$cpu: float8!, $memory: float8!, $onlineUsers: Int!) { " +
                            "insert_" + cfg.metricsTable() + "(objects: { " +
                            "time: $time, tps_1min: $tps1, tps_5min: $tps5, tps_15min: $tps15, " +
                            "cpu_usage: $cpu, memory_usage: $memory, online_users: $onlineUsers }) " +
                            "{ affected_rows } }";

            Map<String, Object> vars = new HashMap<>();
            vars.put("time", time);
            vars.put("tps1", tps[0]);
            vars.put("tps5", tps[1]);
            vars.put("tps15", tps[2]);
            vars.put("cpu", cpu);
            vars.put("memory", memory);
            vars.put("onlineUsers", users);

            graphQL.sendMutation(mutation, vars);

        } catch (Exception e) {
            // Use the plugin's logger to record the exception context
            plugin.getLogger().log(Level.WARNING, "Error sending telemetry data", e);
        }
    }

    /**
     * Compute CPU usage since the last sample as a percentage.
     */
    private double calculateCpuUsage() {
        long currentCpuTime = osBean.getProcessCpuTime();
        long currentTime = System.nanoTime();
        double usage = 0.0;

        if (lastCpuTime > 0 && lastSampleTime > 0) {
            long cpuDelta = currentCpuTime - lastCpuTime;
            long elapsed = currentTime - lastSampleTime;
            int cores = Runtime.getRuntime().availableProcessors();
            usage = (cpuDelta / (elapsed * 1.0 * cores)) * 100.0;
        }

        lastCpuTime = currentCpuTime;
        lastSampleTime = currentTime;
        return usage;
    }

    /**
     * Compute current heap usage in megabytes.
     */
    private double calculateMemoryUsage() {
        Runtime rt = Runtime.getRuntime();
        long usedBytes = rt.totalMemory() - rt.freeMemory();
        return usedBytes / (1024.0 * 1024.0);
    }
}
