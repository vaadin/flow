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

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ByteCodeScanningIT extends ChromeBrowserTest {

    @Test
    public void buttonIsNotInitializedInProductionMode() {
        // in case of this URL parameter presence the Fallback chunk data will
        // be removed and the chunk won't be loaded
        open("drop-fallback");

        TestBenchElement component = $(TestBenchElement.class)
                .id(ByteCodeScanningView.COMPONENT_ID);

        // in production mode without fallback chunk we use optimized bundle by
        // default, so component should not be initialized
        Assert.assertTrue(
                "component expected not initialized in production mode",
                component.$("button").all().isEmpty());
    }

}
