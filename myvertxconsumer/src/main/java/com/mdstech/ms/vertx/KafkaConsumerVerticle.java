package com.mdstech.ms.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

/**
 * Created by Srini on 4/8/17.
 */
public class KafkaConsumerVerticle extends AbstractVerticle {
    @Override
    public void start(Future<Void> future) {
        vertx.createHttpServer().requestHandler( r -> {
            r.response().end("<h1>First Sample Vert.x Service</h1>");
        })
        .listen(
            config().getInteger("http.port",8080),
            result -> {
                if(result.succeeded()) {
                    future.complete();
                }
                else {
                future.fail(result.cause());
                }
            }
        );
    }
}
