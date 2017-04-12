package com.mdstech.ms;

import com.mdstech.ms.domain.Person;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Created by Srini on 4/11/17.
 */
public class BulkMessageHandler extends AbstractVerticle {

    private static final Logger LOGGER = Logger.getLogger(BulkMessageHandler.class.getName());

    private EventBus eventBus;
    private static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final int BAD_REQUEST_ERROR_CODE = 400;

    @Override
    public void start(Future<Void> future) {

//        System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());

        eventBus = vertx.eventBus();
        Router router = Router.router(vertx);
        // Bind "/" to our hello message.
        router.route("/").handler(routingContext -> {
            routingContext.request().response().sendFile("index.html");
        });

        router.route("/api/message*").handler(BodyHandler.create().setMergeFormAttributes(true));

        router.post("/api/message").handler(this::readUploadedFile);

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

    public void readUploadedFile(RoutingContext routingContext) {
        LOGGER.log(Level.INFO,"Start readUploadedFile");
        Set<FileUpload> fileUploadSet = routingContext.fileUploads();
        Iterator<FileUpload> fileUploadIterator = fileUploadSet.iterator();
        LOGGER.log(Level.INFO,"Uploaded file size: " + fileUploadSet.size());
        while (fileUploadIterator.hasNext()) {
            FileUpload fileUpload = fileUploadIterator.next();
            LOGGER.log(Level.INFO,"Uploaded file name: " + fileUpload.uploadedFileName());
            // To get the uploaded file do
            Buffer uploadedFile = vertx.fileSystem().readFileBlocking(fileUpload.uploadedFileName());

            // Uploaded File Name
            try {
                String fileName = URLDecoder.decode(fileUpload.fileName(), "UTF-8");
                LOGGER.log(Level.INFO,"Uploaded file name: " + fileName);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            List<Person> persons = processEvent(uploadedFile);

            persons.stream().forEach(this::publishToMessageSender);

            LOGGER.log(Level.INFO, String.format("Uploaded persons size %d", persons.size()));
            // Use the Event Bus to dispatch the file now
            // Since Event Bus does not support POJOs by default so we need to create a MessageCodec implementation
            // and provide methods for encode and decode the bytes
        }
        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end("Uploaded Successfully");

    }

    private List<Person> processEvent(Buffer buffer) {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        List<Person> persons = new ArrayList<>();
        try {
            XMLEventReader xmlEventReader =
                    xmlInputFactory.createXMLEventReader(new ByteArrayInputStream(buffer.getBytes()));
            Person person = null;
            while(xmlEventReader.hasNext()) {
                XMLEvent xmlEvent = xmlEventReader.nextEvent();
                if (xmlEvent.isStartElement()){
                    StartElement startElement = xmlEvent.asStartElement();
                    if(startElement.getName().getLocalPart().equals("person")) {
                        person = new Person();
                    }
                    else if(startElement.getName().getLocalPart().equals("name")){
                        xmlEvent = xmlEventReader.nextEvent();
                        person.setName(xmlEvent.asCharacters().getData());
                    }
                    else if(startElement.getName().getLocalPart().equals("role")){
                        xmlEvent = xmlEventReader.nextEvent();
                        person.setRole(xmlEvent.asCharacters().getData());
                    }
                }
                else if(xmlEvent.isEndElement()) {
                    EndElement endElement = xmlEvent.asEndElement();
                    if(endElement.getName().getLocalPart().equals("person")){
                        persons.add(person);
                    }
                }
            }
        }
        catch (XMLStreamException ex) {
            ex.printStackTrace();
        }
        return persons;
    }

    private void publishToMessageSender(Person person) {
        String data = Json.encodePrettily(person);
        LOGGER.log(Level.INFO, ""+config().getInteger("http.producer.port"));
        LOGGER.log(Level.INFO, config().getString("http.producer.host"));
        vertx.createHttpClient()
                .post(
                        config().getInteger("http.producer.port", 8080),
                        config().getString("http.producer.host", "message-producer"),
                        "/api/persons",
                        response -> {
                            response.exceptionHandler(ex -> {
                                ex.printStackTrace();
                            });
                            response.bodyHandler(body -> {
                                LOGGER.log(Level.INFO, body.toString());
                            });
                })
                .putHeader("Content-Length", String.valueOf(data.length()))
                .putHeader("Content-Type", "application/json")
                .write(data).end();
    }

    private JsonObject getRequestParams(MultiMap params){

        JsonObject paramMap = new JsonObject();
        for( Map.Entry entry: params.entries()){
            String key = (String)entry.getKey();
            Object value = entry.getValue();
            if(value instanceof List){
                value = (List<String>) entry.getValue();
            }
            else{
                value = (String) entry.getValue();
            }
            paramMap.put(key, value);
        }
        return paramMap;
    }

    private static JsonObject getQueryMap(String query)
    {
        String[] params = query.split("&");
        JsonObject map = new JsonObject();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = "";
            try {
                value = URLDecoder.decode(param.split("=")[1], "UTF-8");
            } catch (Exception e) {
            }
            map.put(name, value);
        }
        return map;
    }
}