/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.complexhierarchyendpoint;

import java.util.Arrays;

import org.junit.Test;

import com.vaadin.fusion.generator.endpoints.AbstractEndpointGenerationTest;
import com.vaadin.fusion.generator.endpoints.complexhierarchymodel.GrandParentModel;
import com.vaadin.fusion.generator.endpoints.complexhierarchymodel.Model;
import com.vaadin.fusion.generator.endpoints.complexhierarchymodel.ParentModel;

public class ComplexHierarchyEndpointTest
        extends AbstractEndpointGenerationTest {
    public ComplexHierarchyEndpointTest() {
        super(Arrays.asList(ComplexHierarchyEndpoint.class, Model.class,
                ParentModel.class, GrandParentModel.class));
    }

    @Test
    public void should_GenerateParentModel_When_UsingChildModel() {
        verifyOpenApiObjectAndGeneratedTs();
    }
}
