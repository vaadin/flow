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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class CallJsFunctionIT extends ChromeBrowserTest {

    @Test
    public void callFunction_argumentsPassedAndReturnValueSentBack() {
        open();

        findElement(By.id("call")).click();

        Assert.assertEquals("a-1-true", getResult());
    }

    @Test
    public void callFunctionOnProperty_calledOnThatProperty() {
        open();

        findElement(By.id("call-on-property")).click();

        // The property the function is read from is its `this`, so the label
        // it reads is the one of the connector rather than of the element
        Assert.assertEquals("connector!", getResult());
    }

    @Test
    public void callFunctionThatIsNotThere_failureReachesTheServer() {
        open();

        findElement(By.id("call-missing")).click();

        Assert.assertEquals("failed", getResult());
    }

    private String getResult() {
        waitUntil(driver -> !findElement(By.id("result")).getText().isEmpty());
        return findElement(By.id("result")).getText();
    }
}
