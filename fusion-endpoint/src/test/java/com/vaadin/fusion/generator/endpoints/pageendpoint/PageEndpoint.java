/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.pageendpoint;

import java.util.Arrays;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.fusion.Endpoint;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@Endpoint
@AnonymousAllowed
public class PageEndpoint {

    public static class Foo {
        private String value;

        public Foo(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public Page<Foo> getPageOfObjects() {
        return new PageImpl<Foo>(Arrays.asList(new Foo("A"), new Foo("B")));
    }

    public Page<String> getPageOfStrings() {
        return new PageImpl<String>(Arrays.asList("A", "B"));
    }
}
