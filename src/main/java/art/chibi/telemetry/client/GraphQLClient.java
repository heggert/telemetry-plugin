package art.chibi.telemetry.client;

import art.chibi.telemetry.Config;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;

@Singleton
public class GraphQLClient {
    private final HttpClient httpClient;
    private final Config config;
    private final Gson gson;
    private final Logger logger;

    @Inject
    public GraphQLClient(
            HttpClient httpClient,
            Config config,
            Gson gson,
            Logger logger,
            JavaPlugin plugin
    ) {
        this.httpClient = httpClient;
        this.config = config;
        this.gson = gson;
        this.logger = logger;
    }

    /**
     * Send any arbitrary GraphQL operation.  The caller is responsible
     * for constructing a valid `query` string and a matching variables map.
     */
    public void sendMutation(String query, Map<String, Object> variables) {
        JsonObject body = new JsonObject();
        body.addProperty("query", query);
        body.add("variables", gson.toJsonTree(variables));

        String payload = gson.toJson(body);
        attemptSend(payload);
    }

    private void attemptSend(String payload) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(config.hasuraEndpoint()))
                .header("Content-Type", "application/json")
                .header("x-hasura-admin-secret", config.hasuraAdminSecret())
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        httpClient
                .sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> {
                    if (resp.statusCode() != 200) {
                        logger.warning("GraphQL HTTP " + resp.statusCode() + ": " + resp.body());
                    }
                    logGraphQLErrors(resp.body());
                })
                .exceptionally(ex -> {
                    String msg = ex.getMessage() == null ? "Unknown error" : ex.getMessage();
                    if (msg.toUpperCase().contains("GOAWAY")) {
                        // we assume the server may already have processed it
                        logger.info("Received HTTP/2 GOAWAY; not retrying.");
                    } else {
                        logger.severe("GraphQL send error: " + msg);
                    }
                    return null;
                });
    }

    private void logGraphQLErrors(String body) {
        JsonElement e = JsonParser.parseString(body);
        if (!e.isJsonObject()) return;
        JsonObject o = e.getAsJsonObject();
        if (!o.has("errors")) return;

        for (JsonElement err : o.getAsJsonArray("errors")) {
            JsonObject eo = err.getAsJsonObject();
            String m = eo.has("message") ? eo.get("message").getAsString() : "no message";
            StringBuilder sb = new StringBuilder("GraphQL error: ").append(m);
            if (eo.has("extensions")) {
                JsonObject ext = eo.getAsJsonObject("extensions");
                if (ext.has("code")) {
                    sb.append(" | code=").append(ext.get("code").getAsString());
                }
                sb.append(" | ext=").append(ext);
            }
            logger.warning(sb.toString());
        }
    }
}
