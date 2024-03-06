/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testcategory;

/**
 * Tests that should not be run inside OSGi container should be annotated with @
 * {@code Category(IgnoreOSGi.class)} so they can be optionally excluded from
 * the build.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public interface IgnoreOSGi extends TestCategory {

}
