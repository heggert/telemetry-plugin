package art.chibi.telemetry;

import com.sun.management.OperatingSystemMXBean;
import dagger.Provides;

import java.lang.management.ManagementFactory;
import java.net.http.HttpClient;

@dagger.Module
public class TelemetryModule {

    @Provides
    public HttpClient provideHttpClient() {
        return HttpClient.newHttpClient();
    }

    @Provides
    public OperatingSystemMXBean provideOperatingSystemMXBean() {
        return (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }
}
