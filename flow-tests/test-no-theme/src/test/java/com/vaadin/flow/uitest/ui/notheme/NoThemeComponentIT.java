/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.notheme;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import org.junit.Assert;
import org.junit.Test;

public class NoThemeComponentIT extends ChromeBrowserTest {

    @Test
    public void themeIsNotApplied() {
        open();

        TestBenchElement link = $("a").first();
        String text = link.getText();
        Assert.assertEquals("Hello notheme", text);
        String color = link.getCssValue("color");
        Assert.assertEquals(
                "Unexpected color for a link. "
                        + "@NoTheme should not theme a link anyhow.",
                "rgba(0, 0, 0, 1)", color);
    }
}
