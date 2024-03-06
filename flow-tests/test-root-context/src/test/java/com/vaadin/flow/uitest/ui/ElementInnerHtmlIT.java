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

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ElementInnerHtmlIT extends ChromeBrowserTest {

    @Test
    public void elementInitOrder() {
        open();
        DivElement innerHtml = $(DivElement.class).id("inner-html-field");

        Assert.assertEquals("", innerHtml.getPropertyString("innerHTML"));

        $(NativeButtonElement.class).id("set-foo").click();
        Assert.assertEquals("<p>Foo</p>",
                innerHtml.getPropertyString("innerHTML"));

        $(NativeButtonElement.class).id("set-foo").click();
        Assert.assertEquals("<p>Foo</p>",
                innerHtml.getPropertyString("innerHTML"));

        $(NativeButtonElement.class).id("set-boo").click();
        Assert.assertEquals("<p>Boo</p>",
                innerHtml.getPropertyString("innerHTML"));

        $(NativeButtonElement.class).id("set-boo").click();
        Assert.assertEquals("<p>Boo</p>",
                innerHtml.getPropertyString("innerHTML"));

        $(NativeButtonElement.class).id("set-null").click();
        Assert.assertEquals("", innerHtml.getPropertyString("innerHTML"));
    }

    @Test
    public void setInnerHtmlAfterChangeVisibility() {
        open();
        DivElement innerHtml = $(DivElement.class).id("inner-html-field");

        $(NativeButtonElement.class).id("set-foo").click();
        Assert.assertEquals("<p>Foo</p>",
                innerHtml.getPropertyString("innerHTML"));

        $(NativeButtonElement.class).id("toggle-visibility").click();

        $(NativeButtonElement.class).id("set-boo").click();
        $(NativeButtonElement.class).id("set-foo").click();

        $(NativeButtonElement.class).id("toggle-visibility").click();
        Assert.assertEquals("<p>Foo</p>",
                innerHtml.getPropertyString("innerHTML"));
    }
}
