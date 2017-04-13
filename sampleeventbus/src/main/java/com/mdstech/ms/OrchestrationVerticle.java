package com.mdstech.ms;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

import java.util.function.Consumer;

/**
 * Created by Srini on 4/12/17.
 */
public class OrchestrationVerticle extends AbstractVerticle {

    public static void main(String[] args) {
        VertxOptions vertxOptions = new VertxOptions().setClustered(false);
        System.setProperty("vertx.cwd", "/src/main/java/com/mdstech/ms");
        Consumer<Vertx> runner = vertx -> {
            try {
                    vertx.deployVerticle("com.mdstech.ms.OrchestrationVerticle");
            } catch (Throwable t) {
                t.printStackTrace();
            }
        };
        if (vertxOptions.isClustered()) {
            Vertx.clusteredVertx(vertxOptions, res -> {
                if (res.succeeded()) {
                    Vertx vertx = res.result();
                    runner.accept(vertx);
                } else {
                    res.cause().printStackTrace();
                }
            });
        } else {
            Vertx vertx = Vertx.vertx(vertxOptions);
            runner.accept(vertx);
        }
    }

    @Override
    public void start() {
        vertx.deployVerticle("com.mdstech.ms.PublishVerticle", this::monitor);
        vertx.deployVerticle("com.mdstech.ms.ConsumeVerticle", this::monitor);

    }

    private void monitor(AsyncResult<String> result) {
        if (result.succeeded()) {
            String deploymentID = result.result();
            System.out.println("Other verticle deployed ok, deploymentID = " + deploymentID);
            vertx.undeploy(deploymentID, res2 -> {
                if (res2.succeeded()) {
                    System.out.println("Undeployed ok!");
                } else {
                    res2.cause().printStackTrace();
                }
            });
        } else {
            result.cause().printStackTrace();
        }
    }
}
