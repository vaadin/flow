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
package com.vaadin.hummingbird.uitest.ui.template;

import java.util.logging.Level;
import java.util.stream.StreamSupport;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntries;

import com.vaadin.hummingbird.testutil.ChromeBrowserTest;
import com.vaadin.testbench.By;

/**
 * @author Vaadin Ltd
 *
 */
public class PolymerModelPropertiesIT extends ChromeBrowserTest {

    @Test
    public void propertiesAreSupplemented() {
        open();
        WebElement template = findElement(By.id("template"));
        // Existing property is not overridden
        Assert.assertEquals("foo",
                getInShadowRoot(template, By.id("name-id")).get().getText());
        Assert.assertEquals("false",
                getInShadowRoot(template, By.id("visible-id")).get().getText());
        Assert.assertEquals("false",
                getInShadowRoot(template, By.id("enable-id")).get().getText());
        Assert.assertEquals("0",
                getInShadowRoot(template, By.id("age-id")).get().getText());
        Assert.assertEquals("0",
                getInShadowRoot(template, By.id("height-id")).get().getText());

        assertHasProperty(template, "city");
        assertHasProperty(template, "list");

        LogEntries logs = driver.manage().logs().get("browser");
        if (logs != null) {
            Assert.assertTrue(
                    "Console doesn't contain a warning about property name clash",
                    StreamSupport.stream(logs.spliterator(), true).anyMatch(
                            entry -> entry.getLevel().equals(Level.WARNING)
                                    && entry.getMessage().replace("\\", "")
                                            .contains(
                                                    "Property \"name\" is already defined on the client side.")));
        }
    }

    private void assertHasProperty(WebElement template, String property) {
        Object object = getCommandExecutor().executeScript(
                "return arguments[0].constructor.__classProperties['" + property
                        + "']",
                template);

        Assert.assertNotNull("Property '" + property
                + "' has not been registered in the element", object);

        Boolean hasCity = (Boolean) getCommandExecutor().executeScript(
                "return arguments[0].hasOwnProperty('" + property + "')",
                template);
        Assert.assertTrue("Element has no property '" + property + "'",
                hasCity);
    }
}
