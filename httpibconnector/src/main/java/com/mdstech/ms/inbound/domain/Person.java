package com.mdstech.ms.inbound.domain;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Srini on 4/10/17.
 */
@Data
public class Person {
    private static final AtomicInteger COUNTER = new AtomicInteger();
    private final int id;

    private String name;
    private String role;

    public Person(String name, String role) {
        this.id = COUNTER.getAndIncrement();
        this.name = name;
        this.role = role;
    }

    public Person() {
        this.id = COUNTER.getAndIncrement();
    }
}
