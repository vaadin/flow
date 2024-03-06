/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testutil;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

/**
 * Removes any CurrentInstance thread locals before running a test.
 */
public class CurrentInstanceCleaner extends RunListener {
    @Override
    public void testStarted(Description description) throws Exception {
        // Clear current instances before each test so a previous test does not
        // affect the test
        try {
            Class<?> cls = Class
                    .forName("com.vaadin.flow.internal.CurrentInstance");
            cls.getMethod("clearAll").invoke(null);
        } catch (Exception e) { // NOSONAR
            // Not a Flow module
        }
    }
}
