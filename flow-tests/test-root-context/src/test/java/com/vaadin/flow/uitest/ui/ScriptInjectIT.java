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
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ScriptInjectIT extends ChromeBrowserTest {

    @Test
    public void ensureNoAlerts() {
        open();
        List<WebElement> inputs = findElements(By.xpath("//input")).stream()
                .filter(element -> "text".equals(element.getAttribute("type")))
                .collect(Collectors.toList());
        Assert.assertEquals(ScriptInjectView.values.length, inputs.size());

        // All inputs should contain some variant of
        // <script>alert('foo');</script>
        for (int i = 0; i < inputs.size(); i++) {
            WebElement e = inputs.get(i);
            Assert.assertEquals(
                    ScriptInjectView.getValue(ScriptInjectView.values[i]),
                    e.getAttribute("value"));
        }

    }
}
