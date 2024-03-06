/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ChangeInjectedComponentTextIT extends ChromeBrowserTest {

    @Test
    public void setText_injectedComponent_textReplacesContent() {
        open();

        WebElement injected = $("update-injected-component-text").first()
                .$(TestBenchElement.class).id("injected");
        Assert.assertEquals(
                "New text value doesn't replace the content of the element",
                "new text", injected.getText());
        Assert.assertEquals(
                "The 'setText()' method should remove all children from the injected component",
                0, injected.findElements(By.cssSelector("*")).size());
    }
}
