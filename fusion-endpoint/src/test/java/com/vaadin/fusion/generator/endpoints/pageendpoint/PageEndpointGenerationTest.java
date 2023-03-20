/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.pageendpoint;

import com.vaadin.fusion.generator.endpoints.AbstractEndpointGenerationTest;

import org.junit.Test;

import java.util.Collections;

public class PageEndpointGenerationTest extends AbstractEndpointGenerationTest {

    public PageEndpointGenerationTest() {
        super(Collections.singletonList(PageEndpoint.class));
    }

    @Test
    public void should_ConvertIterableIntoArrayInTS() {
        verifyOpenApiObjectAndGeneratedTs();
    }
}
