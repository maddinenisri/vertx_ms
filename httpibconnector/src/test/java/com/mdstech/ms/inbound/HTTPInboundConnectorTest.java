package com.mdstech.ms.inbound;

import com.mdstech.ms.inbound.domain.Person;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Srini on 4/10/17.
 */
@RunWith(VertxUnitRunner.class)
public class HTTPInboundConnectorTest {
    private static final Logger LOGGER = Logger.getLogger(HTTPInboundConnectorTest.class.getName());

    private Vertx vertx;
    private Integer port;

    @Before
    public void setUp(TestContext testContext) throws IOException {
        vertx = Vertx.vertx();
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();
        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject().put("http.port", port)
                );
        vertx.deployVerticle(HTTPInboundConnector.class.getName(), options, testContext.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext testContext) {
        vertx.deployVerticle(HTTPInboundConnector.class.getName(), testContext.asyncAssertSuccess());
    }

//    @Test
    public void testRootRoute(TestContext testContext) {
        final Async async = testContext.async();
        vertx.createHttpClient().get(port, "localhost", "/", response -> {
            response.handler(body -> {
                testContext.assertTrue(body.toString().contains("First Sample"));
                async.complete();
            });
        }).end();
    }

    @Test
    public void testGetAll(TestContext testContext) {
        final Async async = testContext.async();
        vertx.createHttpClient().get(port, "localhost", "/api/persons", response -> {
            response.handler(body -> {
                LOGGER.log(Level.INFO, body.toString());
                testContext.assertTrue(body.toString().contains("Srini"));
                async.complete();
            });
        }).end();
    }

    private String getSampleXML() {
        return "<Person><Name>Sample</Name><Role>Developer</Role></Person>";
    }
}
