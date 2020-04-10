package com.pkb;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.vertx.core.logging.LoggerFactory.getLogger;
import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;

public class TicketStatusHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = getLogger(lookup().lookupClass());
    private static final String TICKETS = "/api/v2/tickets/";

    private enum Status {
        open(2),
        pending(3),
        resolved(4),
        closed(5),
        waiting_on_customer(6),
        waiting_on_3rd_party(7),
        waiting_on_developer(9),
        waiting_on_senior_stuff_input(11),
        waiting_on_jira(12),
        cie_deletion(16);

        private final int code;

        Status(int code) {
            this.code = code;
        }

        static Status fromCode(int code) {
            return Stream.of(Status.values())
                    .filter(status -> code == status.code)
                    .findFirst().orElseThrow(() -> new IllegalStateException(format("%s is not recognized status code", code)));
        }
    }

    private final WebClient httpClient;
    private final Supplier<String> authProvider;
    private final String baseUrl;

    public TicketStatusHandler(WebClient httpClient, Supplier<String> authProvider, String baseUrl) {
        this.httpClient = httpClient;
        this.authProvider = authProvider;
        this.baseUrl = baseUrl;
    }

    @Override
    public void handle(RoutingContext context) {
        String ticketId = context.request().getParam("ticketId");
        Status status = Status.valueOf(context.request().getParam("status"));

        httpClient.putAbs(format("%s%s%s", baseUrl, TICKETS, ticketId))
                .putHeader("Authorization", authProvider.get())
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(fdRequest(status), fdResponse -> {
                    if (fdResponse.succeeded()) {
                        LOGGER.info(fdResponse.result().bodyAsString());
                        int updatedStatusCode = fdResponse.result().bodyAsJsonObject().getInteger("status");

                        context.response()
                                .end(new JsonObject()
                                        .put("status", "ok")
                                        .put("current_fd_ticket_status", Status.fromCode(updatedStatusCode))
                                        .toString());
                    } else {
                        context.response().setStatusCode(500)
                                .end(new JsonObject()
                                        .put("status", "failed")
                                        .toString());
                        LOGGER.error(fdResponse.cause());
                    }
                });
    }

    private JsonObject fdRequest(Status status) {
        return new JsonObject().put("status", status.code);
    }
}

