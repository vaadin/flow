/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.denyall;

import java.util.Collections;

import org.junit.Test;

import com.vaadin.fusion.generator.endpoints.AbstractEndpointGenerationTest;

public class DenyAllEndpointGenerationTest
        extends AbstractEndpointGenerationTest {

    public DenyAllEndpointGenerationTest() {
        super(Collections.singletonList(DenyAllEndpoint.class));
    }

    @Test
    public void should_notGenerateEndpointMethodsWithoutSecurityAnnotations_When_DenyAllOnClass() {
        verifyOpenApiObjectAndGeneratedTs();
    }
}
