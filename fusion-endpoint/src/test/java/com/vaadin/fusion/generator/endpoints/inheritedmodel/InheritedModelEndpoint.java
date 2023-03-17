/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.inheritedmodel;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.Version;
import io.swagger.v3.oas.models.media.ArraySchema;

import com.vaadin.fusion.Endpoint;

@Endpoint
public class InheritedModelEndpoint {

    public ParentModel getParentModel(ChildModel child) {
        return new ParentModel();
    }

    public static class ChildModel extends ParentModel {
        String name;
        // This is to make sure that inherited types from dependencies work
        // well.
        ArraySchema testObject;
        List<Map<String, Version>> abc;
        List<Map<String, Map<String, Version>>> def;
    }

    public static class ParentModel {
        String id;

    }
}
