/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.sameclassname;

import java.util.List;
import java.util.Map;

import com.vaadin.fusion.Endpoint;

@Endpoint
public class SameClassNameEndpoint {
    public SameClassNameModel getMyClass(
            List<com.vaadin.fusion.generator.endpoints.sameclassname.subpackage.SameClassNameModel> sameClassNameModel) {
        return null;
    }

    public List<com.vaadin.fusion.generator.endpoints.sameclassname.subpackage.SameClassNameModel> getSubpackageModelList(
            Map<String, com.vaadin.fusion.generator.endpoints.sameclassname.subpackage.SameClassNameModel> sameClassNameModel) {
        return null;
    }

    public Map<String, com.vaadin.fusion.generator.endpoints.sameclassname.subpackage.SameClassNameModel> getSubpackageModelMap(
            Map<String, SameClassNameModel> sameClassNameModel) {
        return null;
    }

    public com.vaadin.fusion.generator.endpoints.sameclassname.subpackage.SameClassNameModel getSubpackageModel() {
        return null;
    }

    public void setSubpackageModel(
            com.vaadin.fusion.generator.endpoints.sameclassname.subpackage.SameClassNameModel model) {
    }

    public static class SameClassNameModel {
        String foo;
    }
}
