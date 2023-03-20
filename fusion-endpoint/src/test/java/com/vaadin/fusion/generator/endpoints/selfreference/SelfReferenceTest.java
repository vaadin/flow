/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.selfreference;

import com.vaadin.fusion.generator.endpoints.AbstractEndpointGenerationTest;

import java.util.Arrays;
import org.junit.Test;

public class SelfReferenceTest extends AbstractEndpointGenerationTest {

    public SelfReferenceTest() {
        super(Arrays.asList(SelfReferenceEndpoint.class, SelfReference.class));
    }

    @Test
    public void should_NotImportSelfInTS_When_ReferencingSelfInModel() {
        verifyOpenApiObjectAndGeneratedTs();
    }
}
