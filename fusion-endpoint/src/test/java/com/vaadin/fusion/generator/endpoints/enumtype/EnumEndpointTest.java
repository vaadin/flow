/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.enumtype;

import java.util.Arrays;

import org.junit.Test;

import com.vaadin.fusion.generator.endpoints.AbstractEndpointGenerationTest;

public class EnumEndpointTest extends AbstractEndpointGenerationTest {
    public EnumEndpointTest() {
        super(Arrays.asList(EnumEndpoint.class));
    }

    @Test
    public void should_GenerateStringType_When_ReferringToEnumTypes() {
        verifyOpenApiObjectAndGeneratedTs();
    }
}
