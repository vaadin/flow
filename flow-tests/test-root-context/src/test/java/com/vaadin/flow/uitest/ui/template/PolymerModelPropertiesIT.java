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
package com.vaadin.flow.uitest.ui.template;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.By;

public class PolymerModelPropertiesIT extends ChromeBrowserTest {

    @Test
    public void propertySyncWithModel() {
        open();

        WebElement initial = findElement(By.id("property-value"));
        Assert.assertEquals("Property value:foo, model value: foo",
                initial.getText());

        WebElement template = findElement(By.id("template"));
        WebElement input = getInShadowRoot(template, By.id("input"));
        input.clear();
        input.sendKeys("x");

        // property update event comes immediately
        List<WebElement> propertyUpdates = findElements(
                By.id("property-update-event"));
        WebElement propertyUpdate = propertyUpdates
                .get(propertyUpdates.size() - 1);
        Assert.assertEquals("Property value:x, model value: x",
                propertyUpdate.getText());

        // now move focus out of the input and check that value change event is
        // fired
        propertyUpdate.click();

        List<WebElement> valueUpdates = findElements(By.id("value-update"));
        WebElement valueUpdate = valueUpdates.get(valueUpdates.size() - 1);
        Assert.assertEquals("Property value:x, model value: x",
                valueUpdate.getText());
    }
}
