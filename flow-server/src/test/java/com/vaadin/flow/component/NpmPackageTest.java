/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.lang.annotation.Annotation;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

public class NpmPackageTest {

    // Keep this here just to make sure the @NpmPackage annotation exists, since
    // it's used outside of the flow-server module, and it has no other use
    // inside flow-server, yet.

    @JsModule("test-component")
    @NpmPackage(value = "test-package", version = "0.0.0")
    private static class TestComponent extends Component {
    }

    @Test
    public void testDummy() {
        Annotation[] annotations = TestComponent.class.getAnnotations();

        Assert.assertEquals("NpmPackage is missing",
                annotations[1].annotationType(), NpmPackage.class);
    }

}
