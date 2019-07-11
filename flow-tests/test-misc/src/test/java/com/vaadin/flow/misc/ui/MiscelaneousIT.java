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
package com.vaadin.flow.misc.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * A test class for miscelaneous tests checking features or fixes
 * that do not require their own IT module.
 *
 * Adding new IT modules penalizes build time, otherwise appending
 * tests to this class run new tests faster.
 */
public class MiscelaneousIT extends ChromeBrowserTest {
    @Override
    protected String getTestPath() {
        return "/";
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        open();
    }

    @Test //#5964
    public void should_loadThemedComponent_fromLocal() {
        WebElement body = findElement(By.tagName("body"));
        Assert.assertEquals("2px", body.getCssValue("padding"));
    }
}
