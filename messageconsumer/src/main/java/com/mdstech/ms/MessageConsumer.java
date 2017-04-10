package com.mdstech.ms;

import com.mdstech.ms.domain.Person;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.kafka.client.consumer.KafkaConsumer;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Srini on 4/10/17.
 */
public class MessageConsumer extends AbstractVerticle {
    private static final Logger LOGGER = Logger.getLogger(MessageConsumer.class.getName());
    private Map<Integer, Person> products = new LinkedHashMap<>();
    private static final String CREATE_PERSON_TOPIC = "personCreateTopic";
    private static final String UPDATE_PERSON_TOPIC = "personUpdateTopic";

    private KafkaConsumer<String, String> consumer = null;

    @Override
    public void start(Future<Void> future) {
        initializeKafkaConsumer();

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
        router.get("/api/persons/:id").handler(this::getOne);

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

    private void initializeKafkaConsumer() {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", config().getString("kafka.url", "kafka:9092"));
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("group.id", "my_group");
        config.put("auto.offset.reset", "earliest");
        config.put("enable.auto.commit", "false");
        consumer = KafkaConsumer.create(vertx, config);
        consumer.handler(record -> {
            System.out.println("Processing key=" + record.key() + ",value=" + record.value() +
                    ",partition=" + record.partition() + ",offset=" + record.offset());
            if(CREATE_PERSON_TOPIC.equals(record.topic())) {
                addOne(record.value());
            }
            else if(UPDATE_PERSON_TOPIC.equals(record.topic())) {
                updateOne(record.value());
            }
        });

        // subscribe to several topics
        Set<String> topics = new HashSet<>();
        topics.add(CREATE_PERSON_TOPIC);
        topics.add(UPDATE_PERSON_TOPIC);
        consumer.subscribe(topics, ar -> {
            if (ar.succeeded()) {
                System.out.println("subscribed");
            } else {
                System.out.println("Could not subscribe " + ar.cause().getMessage());
            }
        });
    }

    private void addOne(String data) {
        // Read the request's content and create an instance of Person.
        final Person person = Json.decodeValue(data,
                Person.class);
        products.put(person.getId(), person);
    }

    private void updateOne(String data) {
        final Person person = Json.decodeValue(data,
                Person.class);
        if(products.containsKey(person.getId())) {
            LOGGER.log(Level.SEVERE, String.format("No person object found with id %d", person.getId()));
        }
        products.put(person.getId(), person);
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

    private void getAll(RoutingContext routingContext) {
        // Write the HTTP response
        // The response is in JSON using the utf-8 encoding
        // We returns the list of bottles
        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(products.values()));
    }

}
