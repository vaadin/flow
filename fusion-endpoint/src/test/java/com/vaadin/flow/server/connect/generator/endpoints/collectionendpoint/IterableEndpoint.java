package com.vaadin.flow.server.connect.generator.endpoints.collectionendpoint;

import java.util.Arrays;

import com.vaadin.flow.server.connect.Endpoint;
import com.vaadin.flow.server.connect.auth.AnonymousAllowed;

@Endpoint
@AnonymousAllowed
public class IterableEndpoint {

    public static class Foo {
        public String bar = "bar";
    }

    public Iterable<Foo> getFoos() {
        return Arrays.asList(new Foo(), new Foo());
    }
}
