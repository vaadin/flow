/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.model;

import java.util.Arrays;

import org.junit.Test;

import com.vaadin.fusion.generator.endpoints.AbstractEndpointGenerationTest;
import com.vaadin.fusion.generator.endpoints.model.subpackage.ModelFromDifferentPackage;

public class ModelEndpointTest extends AbstractEndpointGenerationTest {
    public ModelEndpointTest() {
        super(Arrays.asList(ModelEndpoint.class,
                ModelFromDifferentPackage.class,
                ComplexTypeParamsEndpoint.class,
                ComplexReturnTypeEndpoint.class));
    }

    @Test
    public void should_GenerateCorrectModels_When_ModelsHaveComplexStructure() {
        verifyOpenApiObjectAndGeneratedTs();
    }
}
