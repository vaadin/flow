/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.iterableendpoint;

import com.vaadin.fusion.generator.endpoints.AbstractEndpointGenerationTest;

import org.junit.Test;

import java.util.Collections;

/**
 * IterableEndpointGenerationTest, created on 21/12/2020 23.00
 * 
 * @author nikolaigorokhov
 */
public class IterableEndpointGenerationTest
        extends AbstractEndpointGenerationTest {

    public IterableEndpointGenerationTest() {
        super(Collections.singletonList(IterableEndpoint.class));
    }

    @Test
    public void should_ConvertIterableIntoArrayInTS() {
        verifyOpenApiObjectAndGeneratedTs();
    }
}
