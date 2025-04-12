package art.chibi.telemetry.di

import art.chibi.telemetry.TelemetryPlugin
import art.chibi.telemetry.config.TelemetryConfig
import dagger.BindsInstance
import dagger.Component
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger

@Component(modules = [TelemetryModule::class])
interface TelemetryComponent {

    fun inject(plugin: TelemetryPlugin)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun pluginInstance(plugin: JavaPlugin): Builder

        @BindsInstance
        fun logger(logger: Logger): Builder

        @BindsInstance
        fun telemetryConfig(config: TelemetryConfig): Builder

        fun build(): TelemetryComponent
    }
}