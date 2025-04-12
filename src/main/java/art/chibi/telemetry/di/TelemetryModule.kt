package art.chibi.telemetry.di

import com.sun.management.OperatingSystemMXBean
import dagger.Module
import dagger.Provides
import java.lang.management.ManagementFactory
import java.net.http.HttpClient

@Module
class TelemetryModule {

    @Provides
    fun provideHttpClient(): HttpClient = HttpClient.newHttpClient()

    @Provides
    fun provideOperatingSystemMXBean(): OperatingSystemMXBean =
        ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
}
