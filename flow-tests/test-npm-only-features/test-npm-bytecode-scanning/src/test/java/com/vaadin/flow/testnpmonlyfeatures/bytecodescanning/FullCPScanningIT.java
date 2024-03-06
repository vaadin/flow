/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testnpmonlyfeatures.bytecodescanning;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class FullCPScanningIT extends ChromeBrowserTest {

    @Test
    public void buttonIsInitializedInDevMode() {
        open();

        TestBenchElement component = $(TestBenchElement.class)
                .id(ByteCodeScanningView.COMPONENT_ID);

        // in dev mode we use complete bundle by default, so component
        // should be initialized and shown
        Assert.assertFalse("component expected initialized in dev mode",
                component.$("button").all().isEmpty());
    }

    @Override
    protected Class<? extends Component> getViewClass() {
        return ByteCodeScanningView.class;
    }
}
