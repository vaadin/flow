/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.bootstrap;

import org.junit.Assert;
import org.junit.Test;

public class LocationParserTest {

    @Test
    public void testParameterParsing() {
        Assert.assertEquals(null, LocationParser.getParameter("?", "foo"));
        Assert.assertEquals(null, LocationParser.getParameter("?bar", "foo"));
        Assert.assertEquals("", LocationParser.getParameter("?foo", "foo"));
        Assert.assertEquals("", LocationParser.getParameter("?foo=", "foo"));
        Assert.assertEquals("bar",
                LocationParser.getParameter("?foo=bar", "foo"));
        Assert.assertEquals("bar",
                LocationParser.getParameter("?foo=bar&", "foo"));

        Assert.assertEquals("", LocationParser.getParameter("?foo&bar", "foo"));
        Assert.assertEquals("", LocationParser.getParameter("?bar&foo", "foo"));
        Assert.assertEquals("",
                LocationParser.getParameter("?bar&foo=", "foo"));
        Assert.assertEquals("a",
                LocationParser.getParameter("?bar&foo=a", "foo"));
    }
}
