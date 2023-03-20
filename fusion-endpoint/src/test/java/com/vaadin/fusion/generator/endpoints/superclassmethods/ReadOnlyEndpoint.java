/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.superclassmethods;

import java.util.Optional;

import com.vaadin.fusion.EndpointExposed;

@EndpointExposed
public class ReadOnlyEndpoint<T, ID> extends NonEndpointImpl
        implements NonEndpoint {
    public Optional<T> get(ID id) {
        return Optional.ofNullable(null);
    }
}
