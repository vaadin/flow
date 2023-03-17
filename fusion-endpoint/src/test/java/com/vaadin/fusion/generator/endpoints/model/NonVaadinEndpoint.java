/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.model;

import com.vaadin.fusion.generator.endpoints.model.NonVaadinEndpoint.Endpoint;

@Endpoint
public class NonVaadinEndpoint {

    public String getDescription() {
        return "Not a Vaadin Endpoint, should not generate ts models for it";
    }

    public @interface Endpoint {

    }
}
