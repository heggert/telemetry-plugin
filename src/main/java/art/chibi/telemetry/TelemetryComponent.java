package art.chibi.telemetry;

import dagger.BindsInstance;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

@dagger.Component(modules = TelemetryModule.class)
public interface TelemetryComponent {

    void inject(TelemetryPlugin plugin);

    @dagger.Component.Builder
    interface Builder {

        @BindsInstance
        Builder pluginInstance(JavaPlugin plugin);

        @BindsInstance
        Builder logger(Logger logger);

        @BindsInstance
        Builder telemetryConfig(Config config);

        TelemetryComponent build();
    }
}
