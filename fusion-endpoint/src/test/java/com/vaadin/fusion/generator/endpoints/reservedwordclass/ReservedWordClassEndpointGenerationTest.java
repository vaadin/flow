/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.reservedwordclass;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.fusion.generator.endpoints.AbstractEndpointGenerationTest;

public class ReservedWordClassEndpointGenerationTest
        extends AbstractEndpointGenerationTest {
    @Rule
    public ExpectedException expected = ExpectedException.none();

    public ReservedWordClassEndpointGenerationTest() {
        super(Collections.emptyList());
    }

    @Test
    public void Should_Fail_When_UsingReservedWordInClass() {
        expected.expect(IllegalStateException.class);
        expected.expectMessage("reserved");
        verifyOpenApiObjectAndGeneratedTs();
    }
}
