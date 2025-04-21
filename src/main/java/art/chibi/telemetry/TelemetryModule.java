package art.chibi.telemetry;

import art.chibi.telemetry.client.GraphQLClient;
import com.google.gson.Gson;
import com.sun.management.OperatingSystemMXBean;
import dagger.Module;
import dagger.Provides;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Singleton;
import java.lang.management.ManagementFactory;
import java.net.http.HttpClient;
import java.util.logging.Logger;

@Module
public class TelemetryModule {

    @Provides
    @Singleton
    public HttpClient provideHttpClient() {
        return HttpClient.newHttpClient();
    }

    @Provides
    @Singleton
    public OperatingSystemMXBean provideOperatingSystemMXBean() {
        return (OperatingSystemMXBean)
                ManagementFactory.getOperatingSystemMXBean();
    }

    @Provides
    @Singleton
    public Gson provideGson() {
        return new Gson();
    }

    @Provides
    @Singleton
    public GraphQLClient provideGraphQLClient(
            HttpClient httpClient,
            Config config,
            Gson gson,
            Logger logger,
            JavaPlugin plugin
    ) {
        return new GraphQLClient(httpClient, config, gson, logger, plugin);
    }
}
