/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.webcomponent.MyView.APP_TEXT_ID;

public class MyViewIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/com.vaadin.flow.webcomponent.MyView";
    }

    @Test
    public void applicationOpens_withEmbeddedComponents() {
        open();

        $(SpanElement.class).waitForFirst();
        SpanElement spanWithText = $(SpanElement.class).id(APP_TEXT_ID);

        Assert.assertTrue(
                spanWithText.getText().equals("This is the application view"));

        checkLogsForErrors();
    }

}
