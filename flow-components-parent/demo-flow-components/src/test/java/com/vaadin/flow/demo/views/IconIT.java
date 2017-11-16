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
package com.vaadin.flow.demo.views;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.demo.ComponentDemoTest;
import org.openqa.selenium.By;
import com.vaadin.ui.icon.VaadinIcons;

/**
 * Integration tests for the {@link IconView}.
 */
public class IconIT extends ComponentDemoTest {

    @Test
    public void basicIcons() {
        assertIconProperty("edit-icon", "edit");
        assertIconProperty("close-icon", "close");
    }

    @Test
    public void styledIcon() {
        WebElement icon = layout.findElement(By.id("logo-icon"));
        assertIconProperty(icon, "vaadin-h");

        assertCssValue(icon, "width", "100px");
        assertCssValue(icon, "height", "100px");

        // Selenium returns the color in rgba-format for some reason
        assertCssValue(icon, "color", "rgba(255, 165, 0, 1)");
    }

    @Test
    public void allAvailableIcons() {
        WebElement allIcons = layout
                .findElement(By.className("all-icons-layout"));
        List<WebElement> children = allIcons
                .findElements(By.tagName("vaadin-vertical-layout"));

        Assert.assertEquals(VaadinIcons.values().length, children.size());

        for (int i = 0; i < children.size(); i++) {
            WebElement icon = children.get(i)
                    .findElement(By.tagName("iron-icon"));
            WebElement label = children.get(i).findElement(By.tagName("label"));
            String enumName = VaadinIcons.values()[i].name();

            Assert.assertEquals(enumName, label.getText());

            assertIconProperty(icon, enumName.toLowerCase().replace('_', '-'));
        }
    }

    private void assertIconProperty(String id, String iconName) {
        assertIconProperty(layout.findElement(By.id(id)), iconName);
    }

    private void assertIconProperty(WebElement icon, String iconName) {
        Assert.assertEquals("vaadin:" + iconName, icon.getAttribute("icon"));
    }

    private void assertCssValue(WebElement element, String propertyName,
            String expectedValue) {
        Assert.assertEquals(expectedValue, element.getCssValue(propertyName));
    }

    @Override
    protected String getTestPath() {
        return "/icon";
    }

}
