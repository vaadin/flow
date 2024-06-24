/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testcategory;

/**
 * Tests that should not be run with IE11 should be annotated with @
 * {@code Category(IgnoreIE11.class)} so they can be optionally excluded from
 * the build when running with IE11.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public interface IgnoreIE11 extends TestCategory {

}
