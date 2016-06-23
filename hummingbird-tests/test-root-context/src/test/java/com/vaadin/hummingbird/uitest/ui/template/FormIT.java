/*
 * Copyright 2000-2016 Vaadin Ltd.
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
import org.openqa.selenium.By;

import com.vaadin.hummingbird.testutil.PhantomJSTest;

public class FormIT extends PhantomJSTest {

    @Test
    public void updateServerSide() {
        open();
        Assert.assertEquals("Hello", getValue(FormView.ID_FIRST_NAME));
        Assert.assertEquals("World", getValue(FormView.ID_LAST_NAME));
        Assert.assertEquals("32", getValue(FormView.ID_AGE));

        findElement(By.tagName("button")).click();

        Assert.assertEquals("Hello!", getValue(FormView.ID_FIRST_NAME));
        Assert.assertEquals("World?", getValue(FormView.ID_LAST_NAME));
        Assert.assertEquals("33", getValue(FormView.ID_AGE));
    }

    private String getValue(String inputId) {
        return findElement(By.id(inputId)).getAttribute("value");
    }
}
