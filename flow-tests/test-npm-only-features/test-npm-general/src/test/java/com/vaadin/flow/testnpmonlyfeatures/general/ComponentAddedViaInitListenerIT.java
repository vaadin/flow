/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testnpmonlyfeatures.general;

import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ComponentAddedViaInitListenerIT extends ChromeBrowserTest {

    @Test
    public void componentAddedViaInitListenerIsLoaded() {
        open();

        TestBenchElement component = $("init-listener-component").first();
        TestBenchElement div = component.$("div").first();
        org.junit.Assert.assertEquals("Init Listener Component", div.getText());
    }

    @Override
    protected String getTestPath() {
        String path = super.getTestPath();
        return path.substring(0, path.lastIndexOf("/"));
    }
}
