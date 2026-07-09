/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.partial;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.AnchorElement;
import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.spring.test.partial.MainLayout.RESET_ID;
import static com.vaadin.flow.spring.test.partial.MainLayout.EVENT_LOG_ID;
import static com.vaadin.flow.spring.test.partial.RootLayout.ROOT_EVENT_LOG_ID;
import static com.vaadin.flow.spring.test.partial.SecondView.SECOND_ID;

@NotThreadSafe
public class PartialMatchRefreshIT extends ChromeBrowserTest {

    @Test
    public void whenUpdatingUrl_parentChainShouldBeReused() {
        open();

        Assert.assertEquals("1: RootLayout: constructor",
                $(DivElement.class).id(ROOT_EVENT_LOG_ID).getText());
        Assert.assertEquals("1: MainLayout: constructor",
                $(DivElement.class).id(EVENT_LOG_ID).getText());
        Assert.assertEquals("Main navigation link should be available", 1,
                $(AnchorElement.class).all().size());

        getDriver().get(getRootURL() + "/second");

        Assert.assertTrue("Couldn't find second view text div",
                $(DivElement.class).id(SECOND_ID).isDisplayed());

        Assert.assertEquals("1: MainLayout: constructor",
                $(DivElement.class).id(EVENT_LOG_ID).getText());
        Assert.assertEquals("1: RootLayout: constructor",
                $(DivElement.class).id(ROOT_EVENT_LOG_ID).getText());
    }

    @After
    public void tearDown() {
        $(NativeButtonElement.class).id(RESET_ID).click();
    }

    @Override
    protected String getTestPath() {
        return "/main";
    }
}
