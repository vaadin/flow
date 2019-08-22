/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.webcomponent;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class DefaultValueInitializationIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return Constants.PAGE_CONTEXT + "/defaultValue.html";
    }

    @Test
    public void defaultValues_areSetToCorrectValues_withCorrectUpdateCounts() {
        open();
        /*
         * The page contains three instances of
         * DefaultValueInitializationComponents (default-value-init). id:init-1
         * will have the default value set in Java, id:init-2 will have default
         * value set by an attribute, and id:init-3 will have a value updated by
         * property change
         */

        // Java default
        Assert.assertEquals("Java default value is correct", "1",
                value("init-1"));
        Assert.assertEquals("Java default's counter is correct", "1",
                counter("init-1"));

        // JS default
        Assert.assertEquals("JS default value is correct", "2",
                value("init-2"));
        Assert.assertEquals("JS default's counter is correct", "1",
                counter("init-2"));

        // Updated property
        Assert.assertEquals("Property updated default value is correct", "3",
                value("init-3"));
        Assert.assertEquals("Property updated default's counter is correct",
                "1", counter("init-3"));

        // Verify that counters actually update by clicking a button which
        // updates all the properties
        findElement(By.id("update-properties")).click();

        // Java default
        Assert.assertEquals("Java default's value is changed", "4",
                value("init-1"));
        Assert.assertEquals("Java default's counter increases", "2",
                counter("init-1"));

        // JS default
        Assert.assertEquals("JS default's value is changed", "4",
                value("init-2"));
        Assert.assertEquals("JS default's counter increases", "2",
                counter("init-2"));

        // Updated property
        Assert.assertEquals("Property updated default's value is changed", "4",
                value("init-3"));
        Assert.assertEquals("Property updated default's counter increases", "2",
                counter("init-3"));
    }

    private String value(String componentId) {
        WebElement comp = findElement(By.id(componentId));
        return comp.findElement(By.id("value")).getText();
    }

    private String counter(String componentId) {
        WebElement comp = findElement(By.id(componentId));
        return comp.findElement(By.id("counter")).getText();
    }
}
