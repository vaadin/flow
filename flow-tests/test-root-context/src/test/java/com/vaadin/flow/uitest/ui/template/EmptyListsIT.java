/*
 * Copyright (C) 2000-2026 Vaadin Ltd
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

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class EmptyListsIT extends ChromeBrowserTest {

    @Test
    public void emptyListsAreProperlyHandled() {
        open();

        TestBenchElement template = $("*").id("template");

        Assert.assertTrue(
                template.$("*").attributeContains("class", "item").exists());

        findElement(By.id("set-empty")).click();

        checkLogsForErrors();
    }
}
