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
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.testutil.ChromeBrowserTest;
import com.vaadin.testbench.By;

/**
 * @author Vaadin Ltd.
 */
public class DomRepeatIT extends ChromeBrowserTest {

    @Test
    public void checkThatIndicesAreCorrect() {
        open();

        WebElement template = findElement(By.id("template"));

        for (int i = 0; i < DomRepeatUI.NUMBER_OF_EMPLOYEES; i++) {
            getInShadowRoot(template, By.id(DomRepeatUI.TR_ID_PREFIX + i)).get().click();
            String oldIndex = getInShadowRoot(template, By.id(DomRepeatUI.OLD_INDEX_ID)).get().getText();
            String newIndex = getInShadowRoot(template, By.id(DomRepeatUI.NEW_INDEX_ID)).get().getText();

            Assert.assertEquals(oldIndex, newIndex);
            Assert.assertEquals(i, Integer.parseInt(newIndex));
        }
    }
}
