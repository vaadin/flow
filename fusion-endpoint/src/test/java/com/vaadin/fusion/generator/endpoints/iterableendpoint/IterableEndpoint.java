/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.iterableendpoint;

import java.util.Arrays;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.fusion.Endpoint;

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
