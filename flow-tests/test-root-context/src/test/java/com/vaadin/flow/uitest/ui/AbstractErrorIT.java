/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.List;

import org.junit.Assert;
import org.openqa.selenium.By;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public abstract class AbstractErrorIT extends ChromeBrowserTest {

    protected void assertNoSystemErrors() {
        Assert.assertEquals(0,
                findElements(By.className("v-system-error")).size());

    }

    protected void assertErrorReported(String expectedMsg) {
        List<DivElement> errors = $(DivElement.class)
                .attributeContains("class", "error").all();
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(expectedMsg, errors.get(0).getText());
    }
}
