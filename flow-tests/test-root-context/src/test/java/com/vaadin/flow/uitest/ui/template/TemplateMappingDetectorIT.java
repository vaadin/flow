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
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TemplateMappingDetectorIT extends ChromeBrowserTest {

    @Test
    public void regularTemplate_mappedComponentsAreMarkedAsSuch() {
        open();

        WebElement container = findElement(
                By.tagName("template-mapping-detector"));
        assertMappedComponentsAreMarkedProperly(container, false);
    }

    @Test
    public void templateInTemplate_mappedComponentsAreMarkedAsSuch() {
        open();

        WebElement parentTemplate = findElement(
                By.tagName("template-mapping-detector-parent"));
        WebElement container = findInShadowRoot(parentTemplate,
                By.id("detector")).get(0);
        assertMappedComponentsAreMarkedProperly(container, true);
    }

    @Test
    public void composite_mappedComponentsAreMarkedAsSuch() {
        open();

        WebElement container = findElement(By.id("composite"));
        assertMappedComponentsAreMarkedProperly(container, false);
    }

    private void assertMappedComponentsAreMarkedProperly(WebElement container,
            boolean templateInTemplate) {
        WebElement mappedComponent = findInShadowRoot(container,
                By.id("detector1")).get(0);
        Assert.assertEquals("Template mapped: true", mappedComponent.getText());

        WebElement standaloneComponent = findInShadowRoot(container,
                By.id("detector2")).get(0);
        Assert.assertEquals("Template mapped: false",
                standaloneComponent.getText());

        WebElement standaloneComposite = findInShadowRoot(container,
                By.id("detector3")).get(0);
        Assert.assertEquals(
                "Composite template mapped: false Template mapped: false",
                standaloneComposite.getText());

        WebElement theTemplateItself = findInShadowRoot(container,
                By.id("detector4")).get(0);
        Assert.assertEquals("The template itself: " + templateInTemplate,
                theTemplateItself.getText());
    }

}
