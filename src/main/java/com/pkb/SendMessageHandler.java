package com.pkb;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;

import java.util.function.Supplier;

import static io.vertx.core.logging.LoggerFactory.getLogger;
import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;

public class SendMessageHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = getLogger(lookup().lookupClass());

    public static class Payload {
        private String message;

        public Payload() {
        }

        public Payload(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    private final WebClient httpClient;
    private final Supplier<String> authProvider;
    private final String baseUrl;

    public SendMessageHandler(WebClient httpClient, Supplier<String> authProvider, String baseUrl) {
        this.httpClient = httpClient;
        this.authProvider = authProvider;
        this.baseUrl = baseUrl;
    }

    @Override
    public void handle(RoutingContext context) {
        String ticketId = context.request().getParam("ticketId");

        Payload payload = Json.decodeValue(context.getBodyAsString(), Payload.class);

        httpClient.postAbs(format("%s/api/v2/tickets/%s/reply", baseUrl, ticketId))
                .putHeader("Authorization", authProvider.get())
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(fdRequest(payload), fdResponse -> {
                    if (fdResponse.succeeded()) {
                        context.response().end(new JsonObject()
                                .put("status", "ok")
                                .toString());
                    } else {
                        LOGGER.error(fdResponse.cause());
                        context.response().setStatusCode(500)
                                .end(new JsonObject()
                                        .put("status", "failed")
                                        .toString());
                    }
                });
    }

    private JsonObject fdRequest(Payload payload) {
        return new JsonObject().put("body", payload.message);
    }
}
