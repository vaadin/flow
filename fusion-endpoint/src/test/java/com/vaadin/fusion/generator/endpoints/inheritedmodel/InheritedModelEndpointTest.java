/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.inheritedmodel;

import java.util.Arrays;

import com.fasterxml.jackson.core.Version;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.XML;
import org.junit.Test;

import com.vaadin.fusion.generator.endpoints.AbstractEndpointGenerationTest;

public class InheritedModelEndpointTest extends AbstractEndpointGenerationTest {
    public InheritedModelEndpointTest() {
        super(Arrays.asList(InheritedModelEndpoint.class, Discriminator.class,
                Schema.class, ArraySchema.class, ExternalDocumentation.class,
                XML.class, Version.class));
    }

    @Test
    public void should_GenerateParentModel_When_UsingChildModel() {
        verifyOpenApiObjectAndGeneratedTs();
    }
}
