/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.viteapp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.vaadin.example.addon.AddonLitComponent;
import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.viteapp.views.template.LitComponent;
import com.vaadin.viteapp.views.template.PolymerComponent;
import com.vaadin.viteapp.views.template.ReflectivelyReferencedComponent;
import com.vaadin.viteapp.views.template.TemplateView;

public class TemplateIT extends ChromeBrowserTest {

    @Before
    public void openView() {
        getDriver().get(getRootURL() + "/" + TemplateView.ROUTE);
        waitForDevServer();
        getCommandExecutor().waitForVaadin();
    }

    @Test
    public void testElementIdMapping() {
        final String initialValue = "Default";

        SpanElement litSpan = $(LitComponent.TAG).first().$(SpanElement.class)
                .first();
        Assert.assertEquals(initialValue, litSpan.getText());

        SpanElement polymerSpan = $(PolymerComponent.TAG).first()
                .$(SpanElement.class).first();
        Assert.assertEquals(initialValue, polymerSpan.getText());

        SpanElement addonLitSpan = $(AddonLitComponent.TAG).first()
                .$(SpanElement.class).first();
        Assert.assertEquals(initialValue, addonLitSpan.getText());

        final String newLabel = "New label";
        $(InputTextElement.class).first().setValue(newLabel);
        $(NativeButtonElement.class).first().click();

        Assert.assertEquals(newLabel, litSpan.getText());
        Assert.assertEquals(newLabel, polymerSpan.getText());
        Assert.assertEquals(newLabel, addonLitSpan.getText());
    }

    @Test
    public void testElementReferencedByReflection() {
        SpanElement span = $(ReflectivelyReferencedComponent.TAG).first()
                .$(SpanElement.class).first();
        Assert.assertEquals("ReflectivelyReferencedComponent contents",
                span.getText());
    }
}
