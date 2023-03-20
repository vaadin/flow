/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.theme;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import org.junit.Assert;
import org.junit.Test;

public class TSIT extends ChromeBrowserTest {

    @Test
    public void lumoBadgeIsRenderedCorrectly() {
        open();
        checkLogsForErrors();

        DivElement badge = $("ts-component").first().$(DivElement.class)
                .attribute("theme", "badge").first();
        String badgeBackgroundColor = badge.getCssValue("backgroundColor");
        Assert.assertEquals("rgba(51, 139, 255, 0.13)", badgeBackgroundColor);
    }

    @Override
    protected String getTestPath() {
        String path = super.getTestPath();
        String view = "view/";
        return path.replace(view, "path/");
    }

}
