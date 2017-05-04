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
package com.vaadin.flow.uitest.ui;

import java.util.Locale;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class AttachExistingElementIT extends ChromeBrowserTest {

    @Test
    public void attachExistingElement() {
        open();

        // attach label
        findElement(By.id("attach-label")).click();

        Assert.assertTrue(isElementPresent(By.id("label")));
        Assert.assertEquals("label", findElement(By.id("label")).getTagName()
                .toLowerCase(Locale.ENGLISH));

        // attach existing server-side element
        findElement(By.id("attach-populated-label")).click();
        WebElement label = findElement(By.id("label"));
        Assert.assertEquals("already-populated", label.getAttribute("class"));

        // attach header element
        findElement(By.id("attach-header")).click();

        Assert.assertTrue(isElementPresent(By.id("header")));
        Assert.assertEquals("h1", findElement(By.id("header")).getTagName()
                .toLowerCase(Locale.ENGLISH));

        // attach a child in the shadow root of the div

        findElement(By.id("attach-label-inshadow")).click();
        Optional<WebElement> labelInShadow = getInShadowRoot(
                findElement(By.id("element-with-shadow")),
                By.id("label-in-shadow"));
        Assert.assertTrue(labelInShadow.isPresent());
        Assert.assertEquals("label",
                labelInShadow.get().getTagName().toLowerCase(Locale.ENGLISH));

        // Try to attach non-existing element
        findElement(By.id("non-existing-element")).click();
        Assert.assertTrue(isElementPresent(By.id("non-existing-element")));
    }
}
