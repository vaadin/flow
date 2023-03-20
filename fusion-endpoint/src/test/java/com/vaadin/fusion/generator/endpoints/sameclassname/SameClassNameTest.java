/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.sameclassname;

import java.util.Arrays;

import org.junit.Test;

import com.vaadin.fusion.generator.endpoints.AbstractEndpointGenerationTest;
import com.vaadin.fusion.generator.endpoints.sameclassname.subpackage.SameClassNameModel;
import com.vaadin.fusion.generator.endpoints.sameclassname.subpackage.SubProperty;

public class SameClassNameTest extends AbstractEndpointGenerationTest {

    public SameClassNameTest() {
        super(Arrays.asList(SameClassNameEndpoint.class,
                SameClassNameModel.class, SubProperty.class));
    }

    @Test
    public void should_ImportCorrectModel_When_HaveSameTypeNameInEndpoint() {
        verifyOpenApiObjectAndGeneratedTs();
    }
}
