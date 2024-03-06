/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class OneWayPolymerBindingIT extends ChromeBrowserTest {

    // Numerous tests are carried out in the single test case, because it's
    // expensive to launch numerous Chrome instances
    @Test
    public void initialModelValueIsPresentAndModelUpdatesNormally() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");

        checkInitialState(template);
        checkTemplateModel(template);

        template.$(TestBenchElement.class).id("changeModelValue").click();

        checkStateAfterClick(template);
        checkTemplateModel(template);
    }

    private void checkInitialState(TestBenchElement template) {
        String messageDivText = template.$(TestBenchElement.class)
                .id("messageDiv").getText();
        String titleDivText = template.$(TestBenchElement.class).id("titleDiv")
                .getText();
        Assert.assertEquals(OneWayPolymerBindingView.MESSAGE, messageDivText);
        Assert.assertEquals("", titleDivText);
    }

    private void checkTemplateModel(TestBenchElement template) {
        assertTrue(template.$(TestBenchElement.class)
                .attribute("id", "titleDivConditional").all().size() > 0);
        Assert.assertEquals(0, template.$(TestBenchElement.class)
                .attribute("id", "nonExistingProperty").all().size());
    }

    private void checkStateAfterClick(TestBenchElement template) {
        String changedMessageDivText = template.$(TestBenchElement.class)
                .id("messageDiv").getText();
        String titleDivText = template.$(TestBenchElement.class).id("titleDiv")
                .getText();

        Assert.assertEquals(OneWayPolymerBindingView.NEW_MESSAGE,
                changedMessageDivText);
        Assert.assertEquals("", titleDivText);
    }
}
