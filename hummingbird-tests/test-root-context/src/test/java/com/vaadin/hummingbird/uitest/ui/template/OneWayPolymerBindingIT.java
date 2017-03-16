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

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.testcategory.ChromeTests;
import com.vaadin.hummingbird.testutil.SingleBrowserTest;
import com.vaadin.testbench.By;

/**
 * @author Vaadin Ltd.
 */
@Category(ChromeTests.class)
public class OneWayPolymerBindingIT extends SingleBrowserTest {

    @Test
    public void initialModelValueIsPresentAndModelUpdatesNormally() {
        open();

        WebElement template = findElement(By.id("template"));

        String messageDivText = getInShadowRoot(template, By.id("messageDiv"))
                .get().getText();
        String titleDivText = getInShadowRoot(template, By.id("titleDiv")).get()
                .getText();
        Assert.assertEquals(OneWayPolymerBindingTemplate.MESSAGE,
                messageDivText);
        Assert.assertEquals("", titleDivText);

        getInShadowRoot(template, By.id("changeModelValue")).get().click();

        String changedMessageDivText = getInShadowRoot(template,
                By.id("messageDiv")).get().getText();
        titleDivText = getInShadowRoot(template, By.id("titleDiv")).get()
                .getText();

        Assert.assertEquals(OneWayPolymerBindingTemplate.NEW_MESSAGE,
                changedMessageDivText);
        Assert.assertEquals("", titleDivText);
    }
}
