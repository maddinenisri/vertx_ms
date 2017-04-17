package com.mdstech.ms;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;

/**
 * Created by Srini on 4/12/17.
 */
public class PublishVerticle extends AbstractVerticle {
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        System.out.println("In PublishVerticle.start (async)");
        EventBus eb = vertx.eventBus();
        vertx.setTimer(2000, tid -> {
            System.out.println("Startup tasks are now complete, PublishVerticle is now started!");
            startFuture.complete();
        });
        vertx.setPeriodic(1000, v -> eb.send("news-feed", "Some news!", reply -> {
            if (reply.succeeded()) {
                System.out.println("Received reply " + reply.result().body());
            } else {
                System.out.println("No reply");
            }
        }));
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        vertx.setTimer(2000, tid -> {
            System.out.println("Cleanup tasks are now complete, PublishVerticle is now stopped!");
            stopFuture.complete();
        });
    }
}
