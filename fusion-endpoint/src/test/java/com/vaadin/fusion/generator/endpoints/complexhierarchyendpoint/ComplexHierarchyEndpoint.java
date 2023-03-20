/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.complexhierarchyendpoint;

import com.vaadin.fusion.Endpoint;
import com.vaadin.fusion.generator.endpoints.complexhierarchymodel.Model;

@Endpoint
public class ComplexHierarchyEndpoint {

    // Using Model from another package is intentional here to verify the
    // generator's parsing logic for that case
    public Model getModel() {
        return new Model();
    }

}
