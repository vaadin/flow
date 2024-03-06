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
 * Tests which take more than 0.5s to run should be annotated with @
 * {@code @Category(SlowTests.class)} and will be excluded by default from the
 * test suite.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface SlowTests {

}
