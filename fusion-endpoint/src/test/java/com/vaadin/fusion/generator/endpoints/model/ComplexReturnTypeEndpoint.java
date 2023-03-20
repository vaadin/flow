/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.model;

import java.util.List;

import com.vaadin.fusion.Endpoint;

@Endpoint
public class ComplexReturnTypeEndpoint {
    public List<ModelEndpoint.Account> getAccounts() {
        return null;
    }
}
