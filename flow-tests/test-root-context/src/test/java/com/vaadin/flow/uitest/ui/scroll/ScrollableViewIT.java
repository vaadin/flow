/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.scroll;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

public class ScrollableViewIT extends ChromeBrowserTest {

    @Override
    protected Class<? extends Component> getViewClass() {
        return ScrollableView.class;
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        open();
    }

    @Test
    public void scrollIntoView() {
        Assert.assertTrue(getScrollY() != 0);
        scrollBy(0, -getScrollY());
        Assert.assertTrue(getScrollY() == 0);
        findElement(By.id("button")).click();
        Assert.assertTrue(getScrollY() > 0);
    }

}
