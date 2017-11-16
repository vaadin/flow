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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.By;

/**
 * @author Vaadin Ltd.
 */
public class DomRepeatIT extends ChromeBrowserTest {

    @Test
    public void checkThatIndicesAreCorrect() {
        open();

        WebElement template = findElement(By.id("template"));

        for (int i = 0; i < DomRepeatView.NUMBER_OF_EMPLOYEES; i++) {
            getInShadowRoot(template, By.id(DomRepeatView.TR_ID_PREFIX + i)).click();
            String eventIndex = getInShadowRoot(template, By.id(DomRepeatView.EVENT_INDEX_ID)).getText();
            String repeatIndex = getInShadowRoot(template, By.id(DomRepeatView.REPEAT_INDEX_ID)).getText();

            Assert.assertEquals(eventIndex, repeatIndex);
            Assert.assertEquals(i, Integer.parseInt(repeatIndex));
        }
    }
}
