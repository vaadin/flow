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
package com.vaadin.flow.uitest.ui;

import java.util.List;

import org.junit.Assert;
import org.openqa.selenium.By;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public abstract class AbstractErrorIT extends ChromeBrowserTest {

    protected void assertNoSystemErrors() {
        Assert.assertEquals(0,
                findElements(By.className("v-system-error")).size());

    }

    protected void assertErrorReported(String expectedMsg) {
        List<DivElement> errors = $(DivElement.class)
                .attributeContains("class", "error").all();
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(expectedMsg, errors.get(0).getText());
    }
}
