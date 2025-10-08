/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.testnpmonlyfeatures.nobuildmojo;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class TodoIT extends ChromeBrowserTest {

    @Before
    public void init() {
        open();
    }

    @Override
    protected String getTestPath() {
        return "/view/com.vaadin.flow.frontend.TodoView";
    }

    @Test
    public void testAddOn() {
        TestBenchElement template = $(TestBenchElement.class).id("template");

        TestBenchElement createTemplate = template.$(TestBenchElement.class)
                .id("creator");

        TestBenchElement todo = createTemplate.$(TestBenchElement.class)
                .id("task-input");
        todo.sendKeys("Important task");

        TestBenchElement user = createTemplate.$(TestBenchElement.class)
                .id("user-name-input");
        user.sendKeys("Teuvo testi");

        TestBenchElement createButton = createTemplate.$(TestBenchElement.class)
                .id("create-button");
        createButton.click();

        TestBenchElement todoElement = template
                .findElement(By.tagName("todo-element"));
        Assert.assertEquals("Important task",
                todoElement.$(TestBenchElement.class).id("task").getText());

    }

    @Test
    public void bundleCreated() {
        File baseDir = new File(System.getProperty("user.dir", "."));

        // should create a dev-bundle
        Assert.assertTrue("New devBundle should be generated",
                new File(baseDir, "target/" + Constants.DEV_BUNDLE_LOCATION)
                        .exists());
    }
}
