package com.mdstech.ms;

import com.mdstech.ms.domain.Person;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Srini on 4/10/17.
 */
public class HttpInboundListener extends AbstractVerticle {
    private static final Logger LOGGER = Logger.getLogger(HttpInboundListener.class.getName());

    private static final String CREATE_PERSON_TOPIC = "personCreateTopic";
    private static final String UPDATE_PERSON_TOPIC = "personUpdateTopic";


    private KafkaProducer<String, String> producer = null;

    @Override
    public void start(Future<Void> future) {
        initializeKafkaProducer();

        Router router = Router.router(vertx);
        // Bind "/" to our hello message.
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader("content-type", "text/html")
                    .end("<h1>First Sample Vert.x Service</h1>");
        });

        router.route("/assets/*").handler(StaticHandler.create("assets"));

        router.route("/api/persons*").handler(BodyHandler.create());
        router.post("/api/persons").handler(this::addOne);
        router.put("/api/persons/:id").handler(this::updateOne);

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

    private void initializeKafkaProducer() {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", config().getString("kafka.url", "kafka:9092"));
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("acks", "1");
        producer = KafkaProducer.create(vertx, config);
    }

    private void addOne(RoutingContext routingContext) {
        // Read the request's content and create an instance of Person.
        final Person person = Json.decodeValue(routingContext.getBodyAsString(),
                Person.class);
        LOGGER.log(Level.INFO, String.format("Including new record for %s and %s", person.getName(), person.getRole()));
        // Add it to the backend map
        KafkaProducerRecord<String, String> record =
                KafkaProducerRecord.create(CREATE_PERSON_TOPIC, routingContext.getBodyAsString());
        LOGGER.log(Level.INFO, String.format("Created topic %s and %s", person.getName(), person.getRole()));

        producer.write(record, recordMetadataAsyncResult -> {
            if(recordMetadataAsyncResult.succeeded()) {
                LOGGER.log(Level.INFO, String.format("Created topic %s and %s", person.getName(), person.getRole()));
            }
            else {
                LOGGER.log(Level.SEVERE, String.format("Created topic %s and %s and %s", person.getName(), person.getRole(), recordMetadataAsyncResult.cause().getMessage()));
            }
        });

        LOGGER.log(Level.INFO, String.format("Successfully published %s and %s", person.getName(), person.getRole()));

        // Return the created person as JSON
        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(person));
    }

    private void updateOne(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        JsonObject json = routingContext.getBodyAsJson();
        if (id == null || json == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            final Integer idAsInteger = Integer.valueOf(id);
            json.put("id", idAsInteger);
            Person person = json.mapTo(Person.class);

            KafkaProducerRecord<String, String> record =
                    KafkaProducerRecord.create(UPDATE_PERSON_TOPIC, json.toString());

            producer.write(record);

            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(person));
        }
    }
}
