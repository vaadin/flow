/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.collectionendpoint;

import java.util.Collections;

import org.junit.Test;

import com.vaadin.fusion.generator.endpoints.AbstractEndpointGenerationTest;

public class CollectionEndpointGenerationTest
        extends AbstractEndpointGenerationTest {

    public CollectionEndpointGenerationTest() {
        super(Collections.singletonList(CollectionEndpoint.class));
    }

    @Test
    public void should_DistinguishBetweenUserAndBuiltinTypes_When_TheyHaveSameName() {
        verifyOpenApiObjectAndGeneratedTs();
    }
}
