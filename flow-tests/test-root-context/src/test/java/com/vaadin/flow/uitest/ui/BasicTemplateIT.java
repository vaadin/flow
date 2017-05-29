package com.vaadin.flow.uitest.ui;
/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BasicTemplateIT extends ChromeBrowserTest {

    @Test
    public void testBasicTemplate() {
        open();

        WebElement bar = findElement(By.className("bar"));

        Assert.assertEquals("baz", bar.getText());
        Assert.assertTrue(isElementPresent(By.cssSelector(".bar input")));

        // Included file
        WebElement templateRoot = bar.findElement(By.xpath(".."));
        List<WebElement> children = templateRoot.findElements(By.xpath("*"));

        WebElement otherFileRoot = findElement(By.id("otherfile"));
        assertEquals(children.get(1), otherFileRoot);

        List<WebElement> otherFileChildren = otherFileRoot
                .findElements(By.xpath("*"));
        Assert.assertEquals(2, otherFileChildren.size());
        Assert.assertEquals("This is otherfile.html, nested file below",
                otherFileChildren.get(0).getText());
        assertEquals(otherFileChildren.get(1),
                findElement(By.id("nested-in-otherfile")));
        //
        WebElement containerButton = findElement(
                By.cssSelector("#container > button"));

        // Click button to remove it
        containerButton.click();
        Assert.assertFalse(
                isElementPresent(By.cssSelector("#container > button")));

        WebElement childSlotContent = findElement(
                By.className("childSlotContent"));

        // Click button to remove it
        childSlotContent.click();
        Assert.assertFalse(isElementPresent(By.className(".childSlotContent")));

        assertModelValue("", "null", false);

        findElement(By.id("modelText")).click();
        assertModelValue("text", "text", true);

        findElement(By.id("modelBoolean")).click();
        assertModelValue("false", "false", false);

        findElement(By.id("clearModel")).click();
        assertModelValue("", "null", false);

        findElement(By.id("updateAttributeBinding")).click();
        String value = findElement(By.id("attributeBinding"))
                .getAttribute("value");
        Assert.assertEquals("bar", value);

        findElement(By.id("setAttributes")).click();

        WebElement input = findElement(By.id("input"));
        Assert.assertEquals("updated", input.getAttribute("value"));
        Assert.assertEquals("placeholder", input.getAttribute("placeholder"));
    }

    private void assertModelValue(String text, String title, boolean classPresent) {
        WebElement element = findElement(By.id("bindings"));

        Assert.assertEquals(text, element.getText());
        // Set as a property, but reflected to an attribute in the browser
        Assert.assertEquals(title, element.getAttribute("title"));

        if (classPresent) {
            Assert.assertEquals("name", element.getAttribute("class"));
        } else {
            Assert.assertEquals("", element.getAttribute("class"));
        }

    }
}
