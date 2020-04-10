package com.pkb;


import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.Base64;
import java.util.function.Supplier;

import static io.vertx.core.logging.LoggerFactory.getLogger;
import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;

public class FdVerticle extends AbstractVerticle {
    private static final Logger LOGGER = getLogger(lookup().lookupClass());

    private class AuthProvider implements Supplier<String> {
        private final String key;

        private AuthProvider(String key) {
            this.key = key;
        }

        @Override
        public String get() {
            return Base64.getEncoder().encodeToString(format("%s:X", key).getBytes());
        }
    }

    @Override
    public void start(Future<Void> startedResult) throws Exception {
        super.start(startedResult);

        ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setType("file")
                .setOptional(false)
                .setConfig(new JsonObject().put("path", "/home/utamas/projects/pkb/sandbox/fd-integration/fd.json"));

        ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(fileStore))
                .getConfig(configLoading -> {
                    if (configLoading.succeeded()) {
                        JsonObject config = configLoading.result();

                        Router router = Router.router(vertx);
                        router.route("/api/*").subRouter(api(config));
                        router.get("/healthz").handler(request -> request.response().end("ok"));

                        HttpServer server = vertx.createHttpServer();
                        server.requestHandler(router)
                                .listen(8080, startup -> {
                                    if (startup.succeeded()) {
                                        startedResult.succeeded();
                                    } else {
                                        startup.failed();
                                    }
                                });

                    } else {
                        startedResult.failed();
                    }
                });
    }

    private Router api(JsonObject config) {
        WebClient httpClient = WebClient.create(vertx);
        Supplier<String> authProvider = new AuthProvider(config.getString("token"));
        String baseUrl = config.getString("baseUrl");

        Router api = Router.router(vertx);
        api.route()
                .produces("application/json")
                .handler(BodyHandler.create());

        api.put("/message/:ticketId").handler(new SendMessageHandler(httpClient, authProvider, baseUrl));
        api.post("/status/:ticketId/:status").handler(new TicketStatusHandler(httpClient, authProvider, baseUrl));

        return api;
    }
}
