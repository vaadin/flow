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
 * Tests that requires Chrome browser for their execution should be annotated
 * with @ {@code Category(ChromeTests.class} so they can be optionally excluded
 * from the build when needed.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public interface ChromeTests extends TestCategory {

}
