/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.testbench.TestBenchElement;

public class PushIT extends AbstractSpringTest {

    @Test
    public void websocketsWork() throws Exception {
        open();
        $("button").first().click();
        TestBenchElement world = $("p").attribute("id", "world").waitForFirst();
        Assert.assertEquals("World", world.getText());
    }

    @Override
    protected String getTestPath() {
        return "/push";
    }
}
