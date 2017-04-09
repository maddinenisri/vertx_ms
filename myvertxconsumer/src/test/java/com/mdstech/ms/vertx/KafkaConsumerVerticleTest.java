package com.mdstech.ms.vertx;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
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


/**
 * Created by Srini on 4/8/17.
 */
@RunWith(VertxUnitRunner.class)
public class KafkaConsumerVerticleTest {
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
        vertx.deployVerticle(KafkaConsumerVerticle.class.getName(), options, testContext.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext testContext) {
        vertx.deployVerticle(KafkaConsumerVerticle.class.getName(), testContext.asyncAssertSuccess());
    }

    @Test
    public void testKafkaConsumer(TestContext testContext) {
        final Async async = testContext.async();
        vertx.createHttpClient().getNow(port, "localhost", "/", response -> {
           response.handler(body -> {
               testContext.assertTrue(body.toString().contains("First Sample"));
               async.complete();
           });
        });
    }
}
