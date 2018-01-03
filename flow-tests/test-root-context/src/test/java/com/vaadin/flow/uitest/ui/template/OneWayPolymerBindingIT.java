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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.By;

/**
 * @author Vaadin Ltd.
 */
public class OneWayPolymerBindingIT extends ChromeBrowserTest {

    // Numerous tests are carried out in the single test case, because it's
    // expensive to launch numerous Chrome instances
    @Test
    public void initialModelValueIsPresentAndModelUpdatesNormally() {
        open();

        WebElement template = findElement(By.id("template"));

        checkInitialState(template);
        checkTemplateModel(template);

        getInShadowRoot(template, By.id("changeModelValue")).click();

        checkStateAfterClick(template);
        checkTemplateModel(template);
    }

    private void checkInitialState(WebElement template) {
        String messageDivText = getInShadowRoot(template, By.id("messageDiv"))
                .getText();
        String titleDivText = getInShadowRoot(template, By.id("titleDiv"))
                .getText();
        Assert.assertEquals(OneWayPolymerBindingView.MESSAGE,
                messageDivText);
        Assert.assertEquals("", titleDivText);
    }

    private void checkTemplateModel(WebElement template) {
        assertTrue(isPresentInShadowRoot(template, By.id("titleDivConditional")));
        assertFalse(isPresentInShadowRoot(template, By.id("nonExistingProperty")));
    }

    private void checkStateAfterClick(WebElement template) {
        String changedMessageDivText = getInShadowRoot(template,
                By.id("messageDiv")).getText();
        String titleDivText = getInShadowRoot(template, By.id("titleDiv"))
                .getText();

        Assert.assertEquals(OneWayPolymerBindingView.NEW_MESSAGE,
                changedMessageDivText);
        Assert.assertEquals("", titleDivText);
    }
}
