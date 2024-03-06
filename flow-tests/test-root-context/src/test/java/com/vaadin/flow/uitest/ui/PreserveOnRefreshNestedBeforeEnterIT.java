/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

// IT for https://github.com/vaadin/flow/issues/12356
public class PreserveOnRefreshNestedBeforeEnterIT extends ChromeBrowserTest {

    @Test
    public void refreshViewWithNestedLayouts_eachBeforeEnterIsCalledOnlyOnce() {
        open();

        Assert.assertEquals("1", $(SpanElement.class)
                .id("RootLayout-before-enter-count").getText());
        Assert.assertEquals("1", $(SpanElement.class)
                .id("NestedLayout-before-enter-count").getText());
        Assert.assertEquals("1", $(SpanElement.class)
                .id("PreserveOnRefreshNestedBeforeEnterView-before-enter-count")
                .getText());

        open();

        Assert.assertEquals("2", $(SpanElement.class)
                .id("RootLayout-before-enter-count").getText());
        Assert.assertEquals("2", $(SpanElement.class)
                .id("NestedLayout-before-enter-count").getText());
        Assert.assertEquals("2", $(SpanElement.class)
                .id("PreserveOnRefreshNestedBeforeEnterView-before-enter-count")
                .getText());
    }
}
