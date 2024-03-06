/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template.collections;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TwoWayListBindingIT extends ChromeBrowserTest {

    @Test
    public void itemsInList_twoWayDataBinding_updatesAreSentToServer() {
        open();

        findElement(By.id("enable")).click();

        List<WebElement> fields = $("two-way-list-binding").first()
                .$(DivElement.class).first().findElements(By.id("input"));

        // self check
        Assert.assertEquals(2, fields.size());

        fields.get(0).clear();
        fields.get(0).sendKeys("baz");
        fields.get(0).sendKeys(Keys.TAB);

        Assert.assertEquals("[baz, bar]", getLastInfoMessage());

        fields.get(1).sendKeys("foo");
        fields.get(1).sendKeys(Keys.TAB);

        Assert.assertEquals("[baz, barfoo]", getLastInfoMessage());
    }

    private String getLastInfoMessage() {
        List<WebElement> messages = findElements(By.className("messages"));
        // self check
        Assert.assertTrue(messages.size() > 0);
        return messages.get(messages.size() - 1).getText();
    }
}
