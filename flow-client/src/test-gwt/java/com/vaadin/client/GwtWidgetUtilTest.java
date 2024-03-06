/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client;

public class GwtWidgetUtilTest extends ClientEngineTestBase {

    public void testgetAbsoluteUrl() {
        String absoluteUrl = WidgetUtil.getAbsoluteUrl("foo");
        assertTrue(absoluteUrl.startsWith("http://localhost:"));
        assertTrue(
                absoluteUrl.endsWith("com.vaadin.ClientEngineXSI.JUnit/foo"));

    }
}
