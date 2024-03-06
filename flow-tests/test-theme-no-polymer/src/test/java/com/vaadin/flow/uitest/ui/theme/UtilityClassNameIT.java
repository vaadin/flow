/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.theme;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class UtilityClassNameIT extends ChromeBrowserTest {

    @Test
    public void lumoUtils_customStylesHaveBeenExpanded() {
        open();
        checkLogsForErrors();

        SpanElement primary = $(SpanElement.class).id("primary");
        Assert.assertEquals("rgba(0, 128, 0, 1)", primary.getCssValue("color"));
    }
}
