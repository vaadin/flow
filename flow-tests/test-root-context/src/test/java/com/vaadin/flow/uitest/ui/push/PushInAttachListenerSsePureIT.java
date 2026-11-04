/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.push;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@Category(PushTests.class)
public class PushInAttachListenerSsePureIT extends ChromeBrowserTest {

    @Test
    public void pushInAttachListener_uiKeepsWorking() {
        open();

        for (int i = 1; i <= 3; i++) {
            findElement(By.id(PushInAttachListenerSsePureView.ADD_BUTTON))
                    .click();
            int added = i;
            waitUntil(driver -> findElements(By
                    .className(PushInAttachListenerSsePureView.ATTACHED_CLASS))
                    .size() == added);
        }

        findElement(By.id(PushInAttachListenerSsePureView.COUNT_BUTTON))
                .click();
        waitUntil(driver -> "1".equals(
                findElement(By.id(PushInAttachListenerSsePureView.COUNT_ID))
                        .getText()));
        Assert.assertEquals(3,
                findElements(By.className(
                        PushInAttachListenerSsePureView.ATTACHED_CLASS))
                        .size());
    }
}
