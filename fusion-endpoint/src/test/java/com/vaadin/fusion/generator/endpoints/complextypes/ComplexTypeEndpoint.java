/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.complextypes;

import java.util.List;
import java.util.Map;

import com.vaadin.fusion.Endpoint;

@Endpoint
public class ComplexTypeEndpoint {
    public ComplexTypeModel getComplexTypeModel(
            List<Map<String, String>> data) {
        return new ComplexTypeModel();
    }

    public static class ComplexTypeModel {
        List<Map<String, List<String>>> complexList;
        Map<String, List<String>> complexMap;
    }
}
