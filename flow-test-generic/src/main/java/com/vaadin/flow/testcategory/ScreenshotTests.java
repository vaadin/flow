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
 * Screenshot tests should be annotated with @
 * {@code Category(ScreenshotTest.class} so they can be optionally excluded from
 * the build when needed.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface ScreenshotTests extends TestCategory {

}
