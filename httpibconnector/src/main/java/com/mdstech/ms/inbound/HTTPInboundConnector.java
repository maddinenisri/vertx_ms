package com.mdstech.ms.inbound;

import com.mdstech.ms.inbound.domain.Person;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by Srini on 4/10/17.
 */
public class HTTPInboundConnector extends AbstractVerticle {

    private static final Logger LOGGER = Logger.getLogger(HTTPInboundConnector.class.getName());

    private Map<Integer, Person> products = new LinkedHashMap<>();
    @Override
    public void start(Future<Void> future) {
        createSampleData();

        Router router = Router.router(vertx);
        // Bind "/" to our hello message.
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader("content-type", "text/html")
                    .end("<h1>First Sample Vert.x Service</h1>");
        });

        router.route("/assets/*").handler(StaticHandler.create("assets"));

        router.get("/api/persons").handler(this::getAll);
        router.route("/api/persons*").handler(BodyHandler.create());
        router.post("/api/persons").handler(this::addOne);
        router.get("/api/persons/:id").handler(this::getOne);
        router.put("/api/persons/:id").handler(this::updateOne);
        router.delete("/api/persons/:id").handler(this::deleteOne);

        // Create the HTTP server and pass the "accept" method to the request handler.
        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        // Retrieve the port from the configuration,
                        // default to 8080.
                        config().getInteger("http.port", 8080),
                        result -> {
                            if (result.succeeded()) {
                                future.complete();
                            } else {
                                future.fail(result.cause());
                            }
                        }
                );
    }

    private void addOne(RoutingContext routingContext) {
        // Read the request's content and create an instance of Person.
        final Person person = Json.decodeValue(routingContext.getBodyAsString(),
                Person.class);
        // Add it to the backend map
        products.put(person.getId(), person);

        // Return the created person as JSON
        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(person));
    }

    private void getOne(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            final Integer idAsInteger = Integer.valueOf(id);
            Person person = products.get(idAsInteger);
            if (person == null) {
                routingContext.response().setStatusCode(404).end();
            } else {
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(person));
            }
        }
    }

    private void updateOne(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        JsonObject json = routingContext.getBodyAsJson();
        if (id == null || json == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            final Integer idAsInteger = Integer.valueOf(id);
            Person person = products.get(idAsInteger);
            if (person == null) {
                routingContext.response().setStatusCode(404).end();
            } else {
                person.setName(json.getString("name"));
                person.setRole(json.getString("role"));
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(person));
            }
        }
    }

    private void deleteOne(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            Integer idAsInteger = Integer.valueOf(id);
            products.remove(idAsInteger);
        }
        routingContext.response().setStatusCode(204).end();
    }

    private void getAll(RoutingContext routingContext) {
        // Write the HTTP response
        // The response is in JSON using the utf-8 encoding
        // We returns the list of bottles
        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(products.values()));
    }

    private void createSampleData() {
        Person sriniObj = new Person("Srini", "Architect");
        products.put(sriniObj.getId(), sriniObj);
        Person lakObj = new Person("Lak", "Developer");
        products.put(lakObj.getId(), lakObj);
    }
}
