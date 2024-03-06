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

public class InjectScriptTagIT extends ChromeBrowserTest {

    @Test
    public void openPage_scriptIsEscaped() {
        open();

        TestBenchElement parent = $("inject-script-tag-template").first();

        TestBenchElement div = parent.$(TestBenchElement.class).id("value-div");
        Assert.assertEquals("<!-- <script>", div.getText());

        WebElement slot = findElement(By.id("slot-1"));
        Assert.assertEquals("<!-- <script> --><!-- <script></script>",
                slot.getText());

        TestBenchElement button = parent.$(TestBenchElement.class)
                .id("change-value");
        button.click();

        Assert.assertEquals("<!-- <SCRIPT>", div.getText());
        slot = findElement(By.id("slot-2"));
        Assert.assertEquals("<!-- <SCRIPT> --><!-- <SCRIPT></SCRIPT>",
                slot.getText());
    }

}
