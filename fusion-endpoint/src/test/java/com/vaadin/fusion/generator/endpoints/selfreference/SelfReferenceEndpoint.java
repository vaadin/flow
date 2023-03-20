/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.selfreference;

import com.vaadin.fusion.Endpoint;

@Endpoint
public class SelfReferenceEndpoint {

    public SelfReference getModel() {
        return new SelfReference();
    }
}
