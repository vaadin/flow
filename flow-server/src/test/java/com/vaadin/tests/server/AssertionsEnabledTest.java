/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.tests.server;

import junit.framework.TestCase;

public class AssertionsEnabledTest extends TestCase {
    public void testAssertionsEnabled() {
        boolean assertFailed = false;
        try {
            assert false;
        } catch (AssertionError e) {
            assertFailed = true;
        } finally {
            assertTrue("Unit tests should be run with assertions enabled",
                    assertFailed);
        }
    }
}
