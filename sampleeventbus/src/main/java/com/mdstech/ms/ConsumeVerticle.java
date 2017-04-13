package com.mdstech.ms;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;

/**
 * Created by Srini on 4/12/17.
 */
public class ConsumeVerticle extends AbstractVerticle {
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        EventBus eb = vertx.eventBus();
        System.out.println("In ConsumeVerticle.start (async)");
        vertx.setTimer(2000, tid -> {
            System.out.println("Startup tasks are now complete, ConsumeVerticle is now started!");
            startFuture.complete();
        });
        eb.consumer("news-feed", message -> System.out.println("Received news on consumer 1: " + message.body()));
        eb.consumer("news-feed", message -> System.out.println("Received news on consumer 2: " + message.body()));
        eb.consumer("news-feed", message -> System.out.println("Received news on consumer 3: " + message.body()));
        System.out.println("Ready!");
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        vertx.setTimer(2000, tid -> {
            System.out.println("Cleanup tasks are now complete, ConsumeVerticle is now stopped!");
            stopFuture.complete();
        });
    }
}
