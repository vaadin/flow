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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ElementPropertySignalBindingIT extends ChromeBrowserTest {

    @Before
    public void setUp() {
        open();
    }

    @Test
    public void checkInitialPropertyValue_modifyPropertyValue_checkModifiedValue() {
        WebElement resultElement = findElement(By.id("result-div"));
        WebElement signalValueElement = findElement(By.id("signal-value-div"));
        Assert.assertEquals("testproperty changed to: foo",
                resultElement.getText());
        Assert.assertEquals("Signal value: foo", signalValueElement.getText());

        $(DivElement.class).id("target-div").setProperty("testproperty",
                "changed-value");
        $(DivElement.class).id("target-div").dispatchEvent("change");

        Assert.assertEquals("testproperty changed to: changed-value",
                resultElement.getText());
        Assert.assertEquals("Signal value: changed-value",
                signalValueElement.getText());

    }
}
