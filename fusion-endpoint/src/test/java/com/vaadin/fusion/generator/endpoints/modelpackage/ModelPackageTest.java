/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.modelpackage;

import java.util.Arrays;

import org.junit.Test;

import com.vaadin.fusion.generator.endpoints.AbstractEndpointGenerationTest;
import com.vaadin.fusion.generator.endpoints.modelpackage.subpackage.Account;

public class ModelPackageTest extends AbstractEndpointGenerationTest {

    public ModelPackageTest() {
        super(Arrays.asList(ModelPackageEndpoint.class,
                SubModelPackageEndpoint.class, Account.class));
    }

    @Test
    public void should_ImportCorrectModel_When_ModelsHaveSameNameInDifferentPackage() {
        verifyOpenApiObjectAndGeneratedTs();
    }
}
